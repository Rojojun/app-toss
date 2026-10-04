package com.rojojun.familyshare.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthApi {

    @GetMapping("/health")
    public ResponseEntity<Health> health() {
        return ResponseEntity.ok(new Health("UP"));
    }

    public record Health(String status) {}
}
