package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.SubmissionHistoryDto;
import org.example.dto.SubmissionRequest;
import org.example.dto.SubmissionResponse;
import org.example.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> submit(@Valid @RequestBody SubmissionRequest req,
                                                     Principal principal) {
        return ResponseEntity.ok(submissionService.submit(req, principal.getName()));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<SubmissionHistoryDto>> getTaskHistory(@PathVariable Long taskId,
                                                                     Principal principal) {
        return ResponseEntity.ok(submissionService.getTaskHistory(taskId, principal.getName()));
    }
}
