package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.*;
import com.leadstracker.leadstracker.config.SpringApplicationContext;
import com.leadstracker.leadstracker.entities.AuthorityEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.repositories.UserRepository;
import com.leadstracker.leadstracker.request.*;
import com.leadstracker.leadstracker.response.*;
import com.leadstracker.leadstracker.security.AppConfig;
import com.leadstracker.leadstracker.security.SecurityConstants;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.ClientService;
import com.leadstracker.leadstracker.services.TeamTargetService;
import com.leadstracker.leadstracker.services.UserProfileService;
import com.leadstracker.leadstracker.services.UserService;
//import jakarta.validation.Valid;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/v1/leads")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    Utils utils;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClientService clientService;

    @Autowired
    TeamTargetService teamTargetService;

    @Autowired
    UserProfileService userProfileService;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserRest> createUser(@RequestBody UserDetails userDetails) throws Exception {

        UserDto userDto = modelMapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.createUser(userDto);
        UserRest userRest = modelMapper.map(createdUser, UserRest.class);
        userRest.setTeam(userDto.getTeamName());
        userRest.setCreatedDate(LocalDateTime.now());

        return ResponseEntity.ok(userRest);

    }

    //Viewing all team leads
    @GetMapping("/team-leads")
    public ResponseEntity<List<UserRest>> getAllTeamLeads() {
        List<UserDto> teamLeads = userService.getAllTeamLeads();

        List<UserRest> result = teamLeads.stream()
                .map(user -> modelMapper.map(user, UserRest.class)).toList();

        return ResponseEntity.ok(result);
    }


    //Viewing and managing the data of each team lead
    @GetMapping(path = "/team-leads/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PerfRest> getUser(@PathVariable String userId,
                                            @RequestParam(required = false, defaultValue = "week") String duration) throws Exception {

        UserDto userDto = userService.getUserByUserId(userId);

//         Checking that the user has the TEAM_LEAD role
//        if (!"ROLE_TEAM_LEAD".equalsIgnoreCase(userDto.getRole())) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(null);
//        }

        // Get team performance
        TeamPerformanceDto performance = clientService.getTeamPerformance(userId, duration);

        PerfRest perfRest = modelMapper.map(userDto, PerfRest.class);
        perfRest.setTeamPerformance(performance);

        return ResponseEntity.ok(perfRest);
    }

    //Viewing and managing the data of all team members
    @GetMapping(path = "/team-members", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserRest>> getAllTeamMembers() {
        List<UserDto> teamMembers = userService.getAllTeamMembers();

        List<UserRest> response = teamMembers.stream()
                .map(dto -> modelMapper.map(dto, UserRest.class))
                .toList();

        return ResponseEntity.ok(response);
    }


    //Viewing and managing the performance of all the team members under a team lead
    @GetMapping(path = "/team-leads/{id}/members", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PerfRest>> getTeamMembers(@PathVariable String id,
                                                         @RequestParam(required = false) String duration) {
        List<UserDto> teamMembers = userService.getMembersUnderLead(id);

        List<PerfRest> response = teamMembers.stream()
                .map(dto -> {
                    PerfRest perfRest = modelMapper.map(dto, PerfRest.class);

                    TeamMemberPerformanceDto memberPerf = clientService.getMemberPerformance(dto.getUserId(), duration);

                    perfRest.setMemberPerformance(memberPerf);

                    return perfRest;
                })
                .toList();

        return ResponseEntity.ok(response);
    }



    //Viewing and managing the data of a particular team member under a team lead
    @GetMapping(path = "/team-leads/{userId}/members/{memberId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PerfRest> getTeamMemberUnderLead(@PathVariable String userId, @PathVariable String memberId,
                                                           @RequestParam(required = false) String duration) throws Exception {

        UserDto userDto = userService.getMemberUnderLead(userId, memberId);
        // Get member performance
        TeamMemberPerformanceDto performance = clientService.getMemberPerformance(memberId, duration);

        PerfRest perfRest = modelMapper.map(userDto, PerfRest.class);
        perfRest.setMemberPerformance(performance);

        return ResponseEntity.ok(perfRest);
    }


    @PutMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserDetails userDetails) throws Exception {
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto updatedUser = userService.updateUser(id, userDto);

        UserRest userRest = modelMapper.map(updatedUser, UserRest.class);
        return ResponseEntity.ok(Map.of(
                "user", userRest,
                "status", "SUCCESS",
                "message", "Changes saved successfully"));
    }


    @GetMapping
    public List<UserRest> getAllUsers(@RequestParam(value = "page", defaultValue = "0")
                                      int page, @RequestParam(value = "limit", defaultValue = "10") int limit) throws Exception {
        List<UserRest> userRest = new ArrayList<>();

        List<UserDto> userDtos = userService.getAllUsers(page, limit);
        for (UserDto userDto : userDtos) {
            UserRest userRest1 = new UserRest();
            BeanUtils.copyProperties(userDto, userRest1);
            userRest.add(userRest1);
        }
        return userRest;
    }


    //    Email verification endpoint
    @PostMapping("/email-verification")
    public OperationStatusModel verifyEmailToken(@RequestBody String token) {
        OperationStatusModel operationStatusModel = new OperationStatusModel();
        operationStatusModel.setOperationName(RequestOperationName.VERIFY_EMAIL.name());

        boolean isVerified = userService.verifyEmailToken(token);

        if (isVerified) {
            operationStatusModel.setOperationResult(RequestOperationStatus.SUCCESS.name());
        } else {
            operationStatusModel.setOperationResult(RequestOperationStatus.ERROR.name());
        }
        return operationStatusModel;

    }


    @PostMapping("/forgot-password-request")
    public ResponseEntity<?> forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
        System.out.println("hitting endpoint");

        String token = userService.initiatePasswordReset(request.getEmail());

        if (token != null) {
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "message", "Password reset instructions have been sent to your email.",
                    "status", "SUCCESS"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "No user found with the provided email.",
                    "status", "FAILED"
            ));
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Validated @RequestBody ResetPassword request) {

        try {
            userService.resetPassword(
                    request.getToken(),
                    request.getNewPassword(),
                    request.getConfirmNewPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "Password reset successful.",
                    "status", "SUCCESS"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", "FAILED"
            ));
        }
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Validated @RequestBody OtpVerificationRequest request) {
        // Validating OTP
        boolean isValid = userService.validateOtp(request.getEmail(), request.getOtp());

        if (!isValid) {
            return ResponseEntity.ok(Map.of(
                    "message", "Invalid OTP.",
                    "status", "FAILED"
            ));
        }

        // Getting user details from the database
        UserEntity userEntity = userRepository.findByEmail(request.getEmail());
        if (userEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "User not found", "status", "FAILED")
            );
        }

        // OTP is valid. Generating JWT
//        String jwt = SecurityConstants.generateToken(request.getEmail(), SecurityConstants.Expiration_Time_In_Seconds);
        String tokenSecret = (String) SpringApplicationContext.getBean("secretKey");

        byte[] secretKeyBytes = Base64.getEncoder().encode(tokenSecret.getBytes());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());
        Instant now = Instant.now();

        long expirationTime = SecurityConstants.Expiration_Time_In_Seconds;

        String token = Jwts.builder()
                .setSubject(request.getEmail())
                .claim("roles", userEntity.getRole().getName()) // Add role
                .claim("authorities", getAuthorityNames(userEntity.getRole().getAuthorities())) // Add authorities
                .setExpiration(Date.from(now.plusMillis(expirationTime)))
                .setIssuedAt(Date.from(now))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "status", "LOGIN_SUCCESS"
        ));

    }

    // Helper method to extract authority names
    private List<String> getAuthorityNames(Collection<AuthorityEntity> authorities) {
        return authorities.stream()
                .map(AuthorityEntity::getName)
                .collect(Collectors.toList());
    }


    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        try {
            Map<String, Object> response = userService.resendOtp(request.getEmail());
            HttpStatus status = response.get("status").equals("SUCCESS")
                    ? HttpStatus.OK
                    : HttpStatus.TOO_MANY_REQUESTS;
            return ResponseEntity.status(status).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully."
        ));
    }


    //Getting a user
    @GetMapping("/{userId}")
    public ResponseEntity<UserRest> getUserById(@PathVariable String userId) {
        UserDto userDto = userService.getUserByUserId(userId);
        UserRest response = modelMapper.map(userDto, UserRest.class);
        return ResponseEntity.ok(response);
    }


    @PostMapping(value = "/team", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTeam(@RequestBody TeamDetails teamDetails) throws Exception {

        TeamDto teamDto = modelMapper.map(teamDetails, TeamDto.class);

        TeamDto createdTeam = userService.createTeam(teamDto);
        TeamRest teamRest = modelMapper.map(createdTeam, TeamRest.class);
        teamRest.setTeamLeadName(teamDto.getTeamLeadName());

        return ResponseEntity.ok(Map.of(
                "teamName", teamRest,
                "message", "Team created successfully"
        ));

    }


    //Viewing and managing the data of all team members under a team lead
    @GetMapping("/team-leads/{userId}/members/data")
    public ResponseEntity<PaginatedResponse<UserRest>> getTeamMembersData(
            @PathVariable String userId, @RequestParam(value = "page", defaultValue = "1")
            int page, @RequestParam(value = "limit", defaultValue = "10") int limit) {

        Page<UserDto> allMembers = userService.getTeamMembersData(userId, page, limit);
        List<UserRest> userRest = allMembers.getContent().stream()
                .map(dto -> modelMapper.map(dto, UserRest.class)).toList();


        PaginatedResponse<UserRest> rest = new PaginatedResponse<>();
        rest.setData(userRest);
        rest.setCurrentPage(allMembers.getNumber());
        rest.setTotalPages(allMembers.getTotalPages());
        rest.setTotalItems(allMembers.getTotalElements());
        rest.setPageSize(allMembers.getSize());
        rest.setHasNext(allMembers.hasNext());
        rest.setHasPrevious(allMembers.hasPrevious());

        return ResponseEntity.ok(rest);
    }

    //Admin setting targets for teams
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/assign/team-target")
    public ResponseEntity<TeamTargetResponseDto> assignTarget(@RequestBody TeamTargetRequestDto dto) {
        TeamTargetResponseDto response = teamTargetService.assignTargetToTeam(dto);
        return ResponseEntity.ok(response);
    }

    //Admin viewing the targets set
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/team-targets")
    public ResponseEntity<List<TeamTargetResponseDto>> getAllTargets() {
        List<TeamTargetResponseDto> targets = teamTargetService.getAllTargets();
        return ResponseEntity.ok(targets);
    }

    //Team lead viewing profile
//    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")

    // Viewing the profile of all users (Admin, Team lead, Team member)
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponseDto profile = userProfileService.getProfile(principal.getUsername());
        return ResponseEntity.ok(profile);

    }

    //Team lead editing profile
    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    @PutMapping("/team-lead/edit-profile")
    public ResponseEntity<UserProfileResponseDto> updatePhoneNumber(@RequestBody @Valid UpdateUserProfileRequestDto request,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponseDto updated = userProfileService.updatePhoneNumber(principal.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    //Team lead changing password
    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    @PutMapping("team-lead/profile/change-password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequestDto request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        userProfileService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully.",
                "Status",  "SUCCESS"
        ));
    }

    //Team member viewing profile
//    @PreAuthorize("hasAuthority('ROLE_TEAM_MEMBER')")
//    @GetMapping("/member/profile")
//    public ResponseEntity<UserProfileResponseDto> getTeamMemberProfile(@AuthenticationPrincipal UserPrincipal principal) {
//        UserProfileResponseDto profile = userProfileService.getProfile(principal.getUsername());
//        return ResponseEntity.ok(profile);
//    }

    //Team member editing profile
    @PreAuthorize("hasAuthority('ROLE_TEAM_MEMBER')")
    @PutMapping("/members/edit-profile")
    public ResponseEntity<UserProfileResponseDto> updateTeamMemberPhone(@RequestBody @Valid UpdateUserProfileRequestDto request,
                                                                        @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponseDto updated = userProfileService.updatePhoneNumber(principal.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    //Team member changing password
    @PreAuthorize("hasAuthority('ROLE_TEAM_MEMBER')")
    @PutMapping("/members/profile/change-password")
    public ResponseEntity<?> changeTeamMemberPassword(@RequestBody @Valid ChangePasswordRequestDto request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        userProfileService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully.",
                "status",  "SUCCESS"
        ));
    }

    //Admin viewing profile
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    @GetMapping("/admin/profile")
//    public ResponseEntity<UserProfileResponseDto> getAdminProfile(@AuthenticationPrincipal UserPrincipal principal) {
//        UserProfileResponseDto profile = userProfileService.getProfile(principal.getUsername());
//        return ResponseEntity.ok(profile);
//    }

    //Admin editing profile
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/changeAdminNumber/profile")
    public ResponseEntity<UserProfileResponseDto> updateAdminPhone(@RequestBody @Valid UpdateUserProfileRequestDto request,
                                                                        @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponseDto updated = userProfileService.updatePhoneNumber(principal.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    //Admin changing password
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/profile/change-password")
    public ResponseEntity<?> changeAdminPassword(@RequestBody @Valid ChangePasswordRequestDto request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        userProfileService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully.",
                "status",  "SUCCESS"
        ));
    }

}
