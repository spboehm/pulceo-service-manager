package dev.pulceo.prm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

        @GetMapping(value = {"/health", "/healthz", "/psm/health"})
        public ResponseEntity<String> health() {
            return ResponseEntity.status(200).body("OK");
        }
}
