package com.leadstracker.leadstracker.services.Implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.entities.*;
import com.leadstracker.leadstracker.repositories.*;
import com.leadstracker.leadstracker.response.PaginatedResponse;
import com.leadstracker.leadstracker.security.AppConfig;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.leadstracker.leadstracker.security.SecurityConstants.*;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private AmazonSES amazonSES;

    @Autowired
    Utils utils;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    TeamsRepository teamsRepository;

    @Autowired
    UserTargetRepository userTargetRepository;

    @Autowired
    ClientRepository clientRepository;


    @Value("${OTP_Default_Value:}")
    private String otpDefaultValue;

    @Value("${OTP_Default_Boolean_Value:}")
    private Boolean otpDefaultBooleanValue;


    @Override
    public UserDto getUserByUserId(String userId) {
        UserDto userDto = new UserDto();

        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + userId + "not found");
        }
        BeanUtils.copyProperties(userEntity, userDto);

        if (userEntity.getRole() != null) {
            userDto.setRole(userEntity.getRole().getName());
        }
        return userDto;

    }

    /**
     * @param email
     * @return
     */
    @Override
    public UserDto getUser(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) {
            throw new UsernameNotFoundException(email);
        }
        UserDto returnUser = new UserDto();
        returnUser.setUserId(userEntity.getUserId());
        returnUser.setEmail(userEntity.getEmail());
        returnUser.setPassword(userEntity.getPassword());
        returnUser.setFirstName(userEntity.getFirstName());
        returnUser.setLastName(userEntity.getLastName());
        returnUser.setOtpExpiryDate(userEntity.getOtpExpiryDate());
        returnUser.setOtp(userEntity.getOtp());
        returnUser.setId(userEntity.getId());
        returnUser.setOtpFailedAttempts(userEntity.getOtpFailedAttempts());
        returnUser.setDefaultPassword(userEntity.isDefaultPassword());

        return returnUser;
    }

    @Override
    public UserDto updateUser(String userId, UserDto user) {

        UserEntity userEntity = userRepository.findByUserId(userId);

        if (userEntity == null) {
            throw new UsernameNotFoundException("User with ID: " + userId + "not found");
        }

        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        userEntity.setEmail(user.getEmail());
        userEntity.setPhoneNumber(user.getPhoneNumber());
        userEntity.setStaffId(user.getStaffId());
        userEntity.setRole(userEntity.getRole());
        // Updating team lead
        if (user.getTeamLeadUserId() != null && !user.getTeamLeadUserId().isEmpty()) {
            UserEntity teamLeadEntity = userRepository.findByUserId(user.getTeamLeadUserId());
            if (teamLeadEntity == null) {
                throw new UsernameNotFoundException("Team Lead with User ID: " + user.getTeamLeadUserId() + " not found");
            }
            userEntity.setTeamLead(teamLeadEntity);
        }

        //email
        //staffId
        //phoneNumber
        UserEntity updatedUser = userRepository.save(userEntity);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public List<UserDto> getAllUsers(int page, int limit) {
        List<UserDto> returnUsers = new ArrayList<>();

        if (page > 0) {
            page -= 1;
        }
        Pageable pageableRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);
        List<UserEntity> users = usersPage.getContent();

        for (UserEntity userEntity : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(userEntity, userDto);
            returnUsers.add(userDto);
        }

        return returnUsers;
    }

    @Override
    public String initiatePasswordReset(String email) {
        UserEntity user = userRepository.findByEmail(email);
        if (user == null) {
            return null;
    }
        String token = utils.generatePasswordResetToken();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiration(new Date(System.currentTimeMillis() + Password_Reset_Expiration_Time));

        userRepository.save(user);

        amazonSES.sendPasswordResetRequest(user.getFirstName(), user.getEmail(), token);

        // http://localhost:8080/reset-password?token=xyz123
        System.out.println("Password reset link: http://localhost:8080/reset-password?token=" + token);

        return token;
    }

    @Override
    public void resetPassword(String token, String newPassword, String confirmNewPassword) {

        UserEntity user = userRepository.findByPasswordResetToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password reset token");
        }
        if (user.getPasswordResetExpiration() == null || user.getPasswordResetExpiration().before(new Date())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token has expired");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiration(null);
        user.setDefaultPassword(false);

        userRepository.save(user);
    }

//    /**
//    * @param token
//     * @return
//     */
    @Override
    public boolean verifyEmailToken(String token) {
        boolean returnUser = false;
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if (userEntity != null) {
            boolean hasTokenExpired = utils.hasTokenExpired(token);

            if(!hasTokenExpired) {
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnUser = true;
            }
        }
        return returnUser;
    }


    //otp implementation
    public void saveOtp(String email, String otp, Date expiryTime) {
        UserEntity user = userRepository.findByEmail(email);

        user.setOtp(otp);
        user.setOtpExpiryDate(expiryTime);
        userRepository.save(user);
    }


    public boolean validateOtp(String email, String otp) {
        UserEntity user = userRepository.findByEmail(email);

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new RuntimeException("Account is permanently locked. Contact Support");
        }

        // Checking if user is temporarily blocked
        if (user.getTempBlockTime() != null &&
                System.currentTimeMillis() - user.getTempBlockTime().getTime() < Temp_Block_Duration) {
            long remainingTime = (Temp_Block_Duration -
                    (System.currentTimeMillis() - user.getTempBlockTime().getTime())) / 60000;
            throw new RuntimeException("Account temporarily blocked. Try again in " + remainingTime + " minutes.");
        }

        // Allowing QA default OTP if enabled and matched
        if (otpDefaultBooleanValue && otpDefaultValue.equals(otp)) {
            // Resetting failed attempts and unblock the user if previously blocked
            user.setOtpFailedAttempts(0);
            user.setOtp(null);
            user.setTempBlockTime(null);
            userRepository.save(user);
            return true;
        }

        // Validate OTP
        if (user.getOtp() == null || !otp.equals(user.getOtp())) {
            user.setOtpFailedAttempts(user.getOtpFailedAttempts() + 1);

            // Temporary block on the 3rd attempt
            if (user.getOtpFailedAttempts() > Max_Temp_Attempts &&
                    user.getOtpFailedAttempts() < Max_Perm_Attempts) {
                user.setTempBlockTime(new Date());
                userRepository.save(user);
                throw new ResponseStatusException(HttpStatus.LOCKED,
                        "Too many attempts. Account temporarily blocked for 15 minutes.");
            }

            // Permanently locking on the 5th attempt
            if (user.getOtpFailedAttempts() > Max_Perm_Attempts) {
                user.setAccountLocked(true);
                userRepository.save(user);
                throw new ResponseStatusException(HttpStatus.LOCKED,
                        "Too many attempts. Account permanently locked. Contact support");
            }

            userRepository.save(user);
            return false;
        }

        if (new Date().after(user.getOtpExpiryDate())) {
            throw new RuntimeException("OTP expired");
        }

        //resetting attempts on success
        user.setOtpFailedAttempts(0);
        user.setOtp(null);
        user.setTempBlockTime(null);
        userRepository.save(user);
        return true;

    }


    //clearing expired OTPs
    @Scheduled(fixedRate = 180000) // for 3 minutes
    public void cleanupExpiredOtps() {
        List<UserEntity> users = userRepository.findByOtpIsNotNull();
        Date now = new Date();

        users.forEach(user -> {
            if (user.getOtpExpiryDate() != null &&
                    now.after(user.getOtpExpiryDate())) {
                user.setOtp(null);
                user.setOtpExpiryDate(null);
                userRepository.save(user);
            }
        });
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);
        System.out.println("user entity :"+ userEntity);

        if (userEntity == null) throw new UsernameNotFoundException(email);

        return new UserPrincipal(userEntity);

    }

    @Override
    public void deleteUser(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        userRepository.delete(userEntity);
    }


    @Override

    public Map<String, Object> resendOtp(String email) {
        UserEntity user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found with email : " + email);
        }

        int attempts = Optional.ofNullable(user.getResendOtpAttempts()).orElse(0);
        LocalDateTime lastResendTime = user.getLastOtpResendTime();
        int remainingAttempts = Math.max(0, Max_Resend_Attempts - (attempts + 1));


        //  Validating resend attempts
        if (attempts >= Max_Resend_Attempts &&
                lastResendTime != null &&
                Duration.between(lastResendTime, LocalDateTime.now()).toMinutes() < Resend_Cooldown.toMinutes()) {

            long remainingCooldown = Resend_Cooldown.toMinutes() -
                    Duration.between(lastResendTime, LocalDateTime.now()).toMinutes();

            return Map.of(
                    "status", "ERROR",
                    "message", "Too many resend attempts. Try again after " + remainingCooldown + " minutes.",
                    "timestamp", LocalDateTime.now()
            );
        }

        //  Generating and sending new OTP
        String newOtp = String.format("%06d", new SecureRandom().nextInt(999999));
        user.setOtp(newOtp);
        user.setOtpExpiryDate(new Date(System.currentTimeMillis() + 180000));
        user.setResendOtpAttempts(attempts + 1);
        user.setLastOtpResendTime(LocalDateTime.now());
        userRepository.save(user);

        amazonSES.sendLoginOtpEmail(user.getFirstName(), email, newOtp);

        return Map.of(
                "status", "SUCCESS",
                "message", "New OTP sent successfully",
                "timestamp", LocalDateTime.now(),
                "resendAttemptsRemaining", remainingAttempts
        );
    }


    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        // Validating required fields
        if (userDto.getFirstName() == null || userDto.getFirstName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First Name is required");
        }
        if (userDto.getPhoneNumber() == null || userDto.getPhoneNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone Number is required");
        }
        if (userDto.getStaffId() == null || userDto.getStaffId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID Number is required");
        }
        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (userDto.getRole() == null || userDto.getRole().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        // Checking uniqueness
        if (userRepository.findByEmail(userDto.getEmail()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        if (userRepository.findByStaffId(userDto.getStaffId()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID already exists");
        }

        UserEntity userEntity = modelMapper.map(userDto, UserEntity.class);
        userEntity.setUserId(utils.generateUserId(30));

        // Handling role
        String rawRole = userDto.getRole().trim();
        String roleName = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
        RoleEntity role = roleRepository.findByName(roleName);

        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
        userEntity.setRole(role);

        userEntity.setCreatedDate(LocalDateTime.now());

        //Team handling
        TeamsEntity team = teamsRepository.findByName(userDto.getTeamName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Team not found: " + userDto.getTeamName()));


        userEntity.setTeam(team);
        team.getUsers().add(userEntity);

        // setting the team lead
        if (roleName.equalsIgnoreCase("ROLE_TEAM_LEAD")) {
            team.setTeamLead(userEntity);
        }

        // Handling the team member case
//        if (userDto.getRole().replace("ROLE_", "").equalsIgnoreCase("TEAM_MEMBER")) {
//            if (userDto.getTeamLeadUserId() == null || userDto.getTeamLeadUserId().trim().isEmpty()) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                        "Team Lead ID is required for Team Members");
//            }
//
//            UserEntity teamLead = userRepository.findByUserId(userDto.getTeamLeadUserId());
//            if (teamLead == null) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                        "Assigned Team Lead not found");
//            }
//            userEntity.setTeamLead(teamLead);
//
//        }

        // Setting password and other defaults
        String rawPassword = utils.generateDefaultPassword();
//        userEntity.setPassword(bCryptPasswordEncoder.encode(rawPassword));
        userEntity.setEmailVerificationStatus(true);
        userEntity.setDefaultPassword(true);
        userEntity.setPassword(bCryptPasswordEncoder.encode(userDto.getPassword()));
//        userEntity.setTeam(userDto.getTeamName());

        userRepository.save(userEntity);

        UserDto responseDto = modelMapper.map(userEntity, UserDto.class);
        responseDto.setRole(userEntity.getRole().getName().replace("ROLE_", ""));

        amazonSES.sendOnboardingEmail(responseDto.getEmail(), responseDto.getFirstName(), rawPassword);

        return responseDto;

//        UserEntity savedUser = userRepository.save(userEntity);
//        UserDto responseDto = modelMapper.map(savedUser, UserDto.class);
//        responseDto.setRole(savedUser.getRole().getName().replace("ROLE_", ""));
//
//       amazonSES.sendOnboardingEmail(responseDto.getEmail(), responseDto.getFirstName(), rawPassword);
//        return responseDto;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public List<UserDto> getMembersUnderLead(String id, String name) {

        List<UserEntity> teamMembers = userRepository.findByTeamLead_UserId(id);

        return teamMembers.stream()

                .map(user -> {
                    UserDto dto = modelMapper.map(user, UserDto.class);

                    UserEntity lead;

                    if (user.getTeamLead() != null) {
                        lead = user.getTeamLead();
                    } else if (user.getTeam() != null) {
                        lead = user.getTeam().getTeamLead();
                    } else {
                        lead = null;
                    }

                    if (lead != null) {
                        dto.setTeamLeadName(lead.getFirstName() + " " + lead.getLastName());
                    }

                    return dto;
                })
                .toList();
    }


    /**
     * @param userId
     * @param memberId
     * @return
     */
    @Override
    public UserDto getMemberUnderLead(String userId, String memberId) {

        UserEntity memberEntity = (UserEntity) userRepository.findByUserIdAndTeamLead_UserId(memberId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Team member not found under the specified team lead."));

        return modelMapper.map(memberEntity, UserDto.class);
    }

    /**
     * @param loggedInEmail
     * @return
     */
    @Override
    public UserDto getUserByEmail(String loggedInEmail) {
        UserDto userDto = new UserDto();

        UserEntity userEntity = userRepository.findByEmail(loggedInEmail);

        if (userEntity == null) {
            throw new UsernameNotFoundException("User with Email: " + loggedInEmail + "not found");
        }
        BeanUtils.copyProperties(userEntity, userDto);
        return userDto;
    }

    /**
     * @return
     */
    @Override
    public Page<UserDto> getAllTeamMembers(String name, String team, int page, int limit) {
        if (page > 0) {
            page -= 1;
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));

        RoleEntity role = roleRepository.findByName("ROLE_TEAM_MEMBER");
        if (role == null) {
            role = new RoleEntity("ROLE_TEAM_MEMBER");
            roleRepository.save(role);
        }

        Page<UserEntity> teamMemberPage;

        // Using filtered search if filters are provided
        if ((name != null && !name.trim().isEmpty()) || (team != null && !team.trim().isEmpty())) {
            teamMemberPage = userRepository.findTeamMembersByFilters(
                    (name != null && !name.trim().isEmpty()) ? name : null,
                    (team != null && !team.trim().isEmpty()) ? team : null,
                    pageable);
        } else {
            // Using simple pagination if no filters
            teamMemberPage = userRepository.findByRole(role, pageable);
        }

        // Converting to DTO page
        return teamMemberPage.map(member -> modelMapper.map(member, UserDto.class));

    }



    /**
     * @return
     */
    @Override
    public Page<UserDto> getAllTeamLeads(String name, String team, int page, int limit) {

        if (page > 0) {
            page -= 1;
        }
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));

        RoleEntity role = roleRepository.findByName("ROLE_TEAM_LEAD");
        if (role == null) {
            role = new RoleEntity("ROLE_TEAM_LEAD");
            roleRepository.save(role);
        }

        Page<UserEntity> teamLeadPage;

        // Using filtered search if filters are provided
        if ((name != null && !name.trim().isEmpty()) || (team != null && !team.trim().isEmpty())) {
            teamLeadPage = userRepository.findTeamLeadsByFilters(
                    (name != null && !name.trim().isEmpty()) ? name : null,
                    (team != null && !team.trim().isEmpty()) ? team : null,
                    pageable
            );
        } else {
            // Using simple pagination if no filters
            teamLeadPage = userRepository.findByRole(role, pageable);
        }

        // Converting to DTO page
        return teamLeadPage.map(lead -> modelMapper.map(lead, UserDto.class));
    }
//    @Override
//    public Page<UserDto> getAllTeamLeads(String name, String team, int page, int limit) {
//
//        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
//        RoleEntity role = roleRepository.findByName("ROLE_TEAM_LEAD");
//        if (role == null) {
//            role = new RoleEntity("ROLE_TEAM_LEAD");
//            roleRepository.save(role);
//        }
//
//        List<UserEntity> teamLeadEntities = userRepository.findByRole(role, pageable);
//
//        // Applying filters
//        return teamLeadEntities.stream()
//                .filter(lead -> {
//                    boolean nameMatch = true;
//                    boolean teamMatch = true;
//
//                    // Filtering by name (first name, last name, or full name)
//                    if (name != null && !name.trim().isEmpty()) {
//                        String searchName = name.trim().toLowerCase();
//                        String fullName = (lead.getFirstName() + " " + lead.getLastName()).toLowerCase();
//                        nameMatch = fullName.contains(searchName) ||
//                                lead.getFirstName().toLowerCase().contains(searchName) ||
//                                lead.getLastName().toLowerCase().contains(searchName);
//                    }
//
//                    // Filtering by team name
//                    if (team != null && !team.trim().isEmpty()) {
//                        String searchTeam = team.trim().toLowerCase();
//                        teamMatch = lead.getTeam() != null &&
//                                lead.getTeam().getName().toLowerCase().contains(searchTeam);
//                    }
//
//                    return nameMatch && teamMatch;
//                })
//                .map(lead -> modelMapper.map(lead, UserDto.class))
//                .collect(Collectors.toList());
//    }

    /**
     * @param teamDto
     * @return
     */
    @Override
    public TeamDto createTeam(TeamDto teamDto) {
        // Checking if team name already exists
        if (teamsRepository.findByNameIgnoreCase(teamDto.getName()) != null) {
            throw new RuntimeException("Team with this name already exists");
        }

        // Check that teamLeadId is provided
//        if (teamDto.getTeamLeadId() == null) {
//            throw new RuntimeException("Team Lead is required");
//        }

        UserEntity teamLead = userRepository.findByUserId(teamDto.getTeamLeadId());

        // Check if already assigned to a team
//        if (teamLead.getTeam() != null) {
//            throw new RuntimeException("This Team Lead is already assigned to a team");
//        }

        TeamsEntity teamEntity = modelMapper.map(teamDto, TeamsEntity.class);
//        teamEntity.setTeamLead(teamLead);

        // Saving the team entity
        TeamsEntity savedTeam = teamsRepository.save(teamEntity);

        // Setting the team of the team lead
//        teamLead.setTeam(savedTeam);
//        userRepository.save(teamLead);

        TeamDto responseDto = modelMapper.map(savedTeam, TeamDto.class);
//        responseDto.setTeamLeadId(teamLead.getUserId());
//        responseDto.setTeamLeadName(teamLead.getFirstName() + " " + teamLead.getLastName());

        return responseDto;

    }

    /**
     * @param page
     * @param limit
     * @return
     */
    @Override
    public Page<UserDto> getTeamMembersData(String userId, int page, int limit) {

//        UserEntity userEntity = userRepository.findByUserId(userId);
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<UserEntity> userEntities = userRepository.findByTeamLead_UserId(userId, pageable);

        return userEntities.map(user -> modelMapper.map(user, UserDto.class));

    }

    /**
     * @param keyword
     * @return
     */
    @Override
    public List<UserDto> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        keyword = keyword.trim().toLowerCase();

        List<UserEntity> users;

        // If the keyword contains '@', search full email
        if (keyword.contains("@")) {
            users = userRepository.findByEmailContainingIgnoreCase(keyword);
        }
        // If keyword looks like a date (yyyy-MM-dd)
        else if (keyword.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate date = LocalDate.parse(keyword);
            users = userRepository.findByCreatedDateBetween(
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
            );
        }
        // Otherwise search name or username part of email
        else {
            users = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailStartingWithIgnoreCase(
                    keyword, keyword, keyword
            );
        }

        return users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
    }

    /**
     * @param teamId
     * @return
     */
    @Override
    public TeamDto getTeamById(String teamId) {
        TeamsEntity teamEntity = teamsRepository.findById(Long.valueOf(teamId))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        TeamDto teamDto = modelMapper.map(teamEntity, TeamDto.class);

        if (teamEntity.getTeamLead() != null) {
            teamDto.setId(teamEntity.getId());
            teamDto.setTeamLeadId(teamEntity.getTeamLead().getUserId());
            teamDto.setTeamLeadName(teamEntity.getTeamLead().getFirstName() + " " +
                    teamEntity.getTeamLead().getLastName());
            teamDto.setTeamLeadEmail(teamEntity.getTeamLead().getEmail());
            teamDto.setLeadPhoneNumber(teamEntity.getTeamLead().getPhoneNumber());
            teamDto.setLeadStaffId(teamEntity.getTeamLead().getStaffId());
            teamDto.setCreatedDate(teamEntity.getTeamLead().getCreatedDate());
        }

        return teamDto;
    }

    /**
     * @param teamId
     * @param teamDto
     * @return
     */
    @Override
    public TeamDto updateTeam(String teamId, TeamDto teamDto) {
        TeamsEntity teamEntity = teamsRepository.findById(Long.valueOf(teamId))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Updating fields
        if (teamDto.getName() != null && !teamDto.getName().isBlank()) {
            teamEntity.setName(teamDto.getName());
        }

        if (teamDto.getTeamLeadId() != null && !teamDto.getTeamLeadId().isBlank()) {
            UserEntity teamLead = userRepository.findByUserId(teamDto.getTeamLeadId());
            teamEntity.setTeamLead(teamLead);
        }

        TeamsEntity savedEntity = teamsRepository.save(teamEntity);

        TeamDto updatedDto = modelMapper.map(savedEntity, TeamDto.class);
        if (savedEntity.getTeamLead() != null) {
            updatedDto.setTeamLeadId(savedEntity.getTeamLead().getUserId());
            updatedDto.setTeamLeadName(savedEntity.getTeamLead().getFirstName() + " " +
                    savedEntity.getTeamLead().getLastName());
        }

        return updatedDto;

    }

    /**
     * @param teamId
     */
    @Override
    public void deactivateTeam(String teamId) {
        TeamsEntity teamEntity = teamsRepository.findById(Long.valueOf(teamId))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        teamEntity.setActive(false);
        teamsRepository.save(teamEntity);
    }


    @Override
    public void reactivateTeam(String teamId, String newTeamLeadId) {
        TeamsEntity teamEntity = teamsRepository.findById(Long.valueOf(teamId))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (teamEntity.isActive()) {
            throw new RuntimeException("Team is already active");
        }

        UserEntity currentLead = teamEntity.getTeamLead();

        //checking the role for validity
        boolean leadIsValid = currentLead != null && "ROLE_TEAM_LEAD".equals(currentLead.getRole());

        if (!leadIsValid) {
            if (newTeamLeadId != null) {
                // Assigning new team lead
                UserEntity newLead = userRepository.findByUserId(newTeamLeadId);

                if (!newLead.getRole().equals(leadIsValid)) {
                    throw new RuntimeException("Selected user is not a team lead");
                }

                teamEntity.setTeamLead(newLead);
            } else {
                throw new RuntimeException("Current team lead is invalid. Please provide a new team lead ID.");
            }
        }

        // Reactivating team
        teamEntity.setActive(true);
        teamsRepository.save(teamEntity);
    }



    /**
     * @param name
     * @return
     */
    @Override
    public List<TeamDto> searchTeamByName(String name) {
        List<TeamsEntity> teamEntities = teamsRepository.findByNameContainingIgnoreCase(name);

        if (teamEntities.isEmpty()) {
            throw new RuntimeException("No teams found with name containing: " + name);
        }

        return teamEntities.stream().map(team -> {
            TeamDto dto = modelMapper.map(team, TeamDto.class);
            if (team.getTeamLead() != null) {
                dto.setTeamLeadId(team.getTeamLead().getUserId());
                dto.setTeamLeadName(team.getTeamLead().getFirstName() + " " +
                        team.getTeamLead().getLastName());
            }
            return dto;
        }).toList();
    }


    public TeamDto getTeamWithMembers(String teamId, LocalDate startDate, LocalDate endDate, int page, int limit) {
        if(page > 0) {
            page--;
        }
        TeamsEntity team = (TeamsEntity) teamsRepository.findByIdAndActiveTrue(Long.valueOf(teamId))
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // date range
        Date[] dateRange = calculateDateRange(startDate, endDate);

        UserEntity teamLead = team.getTeamLead();
        List<UserEntity> participants = new ArrayList<>(team.getUsers());

        participants = participants.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new));

        //setting up pagination
        int totalItems = participants.size();
        int totalPages = (int) Math.ceil((double) totalItems / limit);

        int fromIndex = page * limit;
        int toIndex = Math.min(fromIndex + limit, totalItems);

        List<UserEntity> paginatedParticipants = new ArrayList<>();
        if (fromIndex < totalItems) {
            paginatedParticipants = participants.subList(fromIndex, toIndex);
        }

        // mapping paginated members
        List<TeamMemberPerformanceDto> memberDtos = paginatedParticipants.stream()
                .map(p -> teamMemberStats(p, dateRange[0], dateRange[1]))
                .toList();

        // building paginated response
        PaginatedResponse<TeamMemberPerformanceDto> paginatedResponse = new PaginatedResponse<>();
        paginatedResponse.setData(memberDtos);
        paginatedResponse.setCurrentPage(page);
        paginatedResponse.setTotalPages(totalPages);
        paginatedResponse.setTotalItems(totalItems);
        paginatedResponse.setPageSize(limit);
        paginatedResponse.setHasNext(page < totalPages - 1);
        paginatedResponse.setHasPrevious(page > 0);

        // map to TeamDto
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setTeamLeadId(teamLead != null ? teamLead.getUserId() : null);
        dto.setTeamLeadName(teamLead != null ? teamLead.getFirstName() + " " + teamLead.getLastName() : null);
        dto.setTeamLeadEmail(teamLead != null ? teamLead.getEmail() : null);
        dto.setLeadPhoneNumber(teamLead != null ? teamLead.getPhoneNumber() : null);
        dto.setLeadStaffId(teamLead != null ? teamLead.getStaffId() : null);
        dto.setCreatedDate(teamLead != null ? teamLead.getCreatedDate() : null);

        // map team members
        dto.setTeamMembers(paginatedResponse);
//        dto.setTeamMembers(
//                participants.stream()
//                        .map(p -> teamMemberStats(p, dateRange[0], dateRange[1]))
//                        .collect(Collectors.toList())
//        );

        return dto;
    }

    // Team member stats
    private TeamMemberPerformanceDto teamMemberStats(UserEntity member, Date start, Date end) {
        List<ClientEntity> memberClients = clientRepository.findByCreatedByAndCreatedDateBetween(member, start, end);

        // Latest target for this user
        UserTargetEntity latestTarget = userTargetRepository.findTopByUserOrderByAssignedDateDesc(member);
        int target = (latestTarget != null) ? latestTarget.getTargetValue() : 0;

        int submitted = memberClients.size();
        double progressPercentage = (target > 0) ? Math.ceil((submitted * 100.0) / target) : 0.0;

        TeamMemberPerformanceDto dto = new TeamMemberPerformanceDto();
        dto.setMemberId(member.getUserId());
        dto.setMemberName(member.getFirstName() + " " + member.getLastName());
        dto.setEmail(member.getEmail());
        dto.setTeamName(member.getTeam() != null ? member.getTeam().getName() : null);
        dto.setTeamLeadName(member.getTeam() != null && member.getTeam().getTeamLead() != null
                ? member.getTeam().getTeamLead().getFirstName() + " " + member.getTeam().getTeamLead().getLastName()
                : null);
        dto.setCreatedDate(member.getCreatedDate());
        dto.setStaffId(member.getStaffId());
        dto.setPhoneNumber(member.getPhoneNumber());

        dto.setTotalClientsSubmitted(submitted);
        dto.setTarget(target);
        dto.setProgressPercentage(progressPercentage);
        dto.setProgressFraction(submitted + "/" + target);

        dto.setClientStatus(
                memberClients.stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getClientStatus().getDisplayName(),
                                Collectors.summingInt(c -> 1)
                        ))
        );

        return dto;
    }

    // Date range method
    private Date[] calculateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        // using last 5 days as default if no dates are provided
        if (startDate == null && endDate == null) {
            endDate = today;
            startDate = today.minusDays(4);
        }
        //  If only startDate is provided
        else if (startDate != null && endDate == null) {
            endDate = today;
        }
        //  If only endDate is provided
        else if (startDate == null && endDate != null) {
            startDate = endDate.minusDays(4);
        }

        // Ensuring at least 5 days
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween < 4) {
            startDate = endDate.minusDays(4);
        }

        return new Date[]{
                Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        };
    }

}

