package com.leadstracker.leadstracker.controller;

import com.leadstracker.leadstracker.DTO.NotificationDto;
import com.leadstracker.leadstracker.DTO.SimpleClientDto;
import com.leadstracker.leadstracker.DTO.SimpleUserDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.request.FollowUpRequest;
import com.leadstracker.leadstracker.security.UserPrincipal;
import com.leadstracker.leadstracker.services.NotificationService;
import com.leadstracker.leadstracker.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/notifications")
public class Notification {

    @Autowired
    UserService userService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    ModelMapper modelMapper;

    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    @GetMapping("/team-lead")
    public ResponseEntity<Map<String, Object>> getTeamLeadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String teamLeadId = userService.getUserByEmail(userPrincipal.getUsername()).getUserId();

        List<NotificationDto> notifications = notificationService.getNotificationsForTeamLead(teamLeadId)
                .stream()
                .map(notification -> {
                    NotificationDto dto = modelMapper.map(notification, NotificationDto.class);
                    dto.setId(notification.getId());

                    if (notification.getAdmin() != null) {
                        dto.setAdmin(modelMapper.map(notification.getAdmin(), SimpleUserDto.class));
                    }

                    if (notification.getTeamLead() != null) {
                        dto.setTeamLead(modelMapper.map(notification.getTeamLead(), SimpleUserDto.class));
                    }

                    if (notification.getClient() != null) {
                        dto.setClient(modelMapper.map(notification.getClient(), SimpleClientDto.class));
                    }

                    if (notification.getClient() != null) {
                        ClientEntity client = notification.getClient();
                        SimpleClientDto clientDto = new SimpleClientDto();
                        clientDto.setClientId(client.getClientId());
                        clientDto.setFirstName(client.getFirstName());
                        clientDto.setLastName(client.getLastName());
                        clientDto.setPhoneNumber(client.getPhoneNumber());
                        clientDto.setClientStatus(client.getClientStatus().name());

                        // Setting userId of the user who created the client
                        if (client.getCreatedBy() != null) {
                            clientDto.setUserId(client.getCreatedBy().getUserId());
                        }

                        // Converting Date to LocalDateTime
                        if (client.getCreatedDate() != null) {
                            clientDto.setCreatedDate(client.getCreatedDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime());
                        }

                        dto.setClient(clientDto);
                    }

                    dto.setMessage(dto.generateMessage());
                    return dto;
                })  .collect(Collectors.toList());


        long unreadCount = notifications.stream()
                .filter(n -> !n.isResolved())
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    // endpoint for client follow-up
    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    @PostMapping("/{notificationId}/follow-up")
    public ResponseEntity<String> handleFollowUp(
            @PathVariable Long notificationId,
            @RequestBody FollowUpRequest followUpRequest) {

        notificationService.handleFollowUp(
                notificationId,
                followUpRequest.getActionTaken(),
                followUpRequest.getNewStatus()
        );

        return ResponseEntity.ok("Follow-up recorded");
    }


    @GetMapping("/admin/notifications/unresolved")
    public ResponseEntity<List<NotificationDto>> getUnresolvedNotificationsForAdmin() {
        List<NotificationDto> notifications = notificationService.getUnresolvedNotificationsForAdmin();
        return ResponseEntity.ok(notifications);
    }


}
