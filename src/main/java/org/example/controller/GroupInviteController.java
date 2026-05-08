package org.example.controller;

import org.example.dto.GroupInviteDto;
import org.example.service.GroupInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-invites")
public class GroupInviteController {

    private final GroupInviteService inviteService;

    public GroupInviteController(GroupInviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<GroupInviteDto>> incoming(Principal principal) {
        return ResponseEntity.ok(inviteService.incoming(principal.getName()));
    }

    @GetMapping("/incoming/count")
    public ResponseEntity<Map<String, Long>> incomingCount(Principal principal) {
        return ResponseEntity.ok(Map.of("count", inviteService.pendingCount(principal.getName())));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable("id") Long id, Principal principal) {
        inviteService.accept(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> decline(@PathVariable("id") Long id, Principal principal) {
        inviteService.decline(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
