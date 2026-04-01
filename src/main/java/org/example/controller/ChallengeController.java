package org.example.controller;

import org.example.dto.ChallengeDto;
import org.example.dto.ChallengeResultDto;
import org.example.service.ChallengeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public ResponseEntity<List<ChallengeDto>> getActive(Principal principal) {
        return ResponseEntity.ok(challengeService.findActive(principal.getName()));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(@PathVariable Long id, Principal principal) {
        challengeService.join(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<List<ChallengeResultDto>> getResults(@PathVariable Long id) {
        return ResponseEntity.ok(challengeService.getResults(id));
    }
}
