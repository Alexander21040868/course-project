package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.HintRequest;
import org.example.dto.HintResponse;
import org.example.service.CodeHintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/ai")
public class AiHintController {

    private final CodeHintService codeHintService;

    public AiHintController(CodeHintService codeHintService) {
        this.codeHintService = codeHintService;
    }

    @PostMapping("/hint")
    public ResponseEntity<HintResponse> hint(@Valid @RequestBody HintRequest req, Principal principal) {
        return ResponseEntity.ok(codeHintService.hint(req, principal.getName()));
    }
}
