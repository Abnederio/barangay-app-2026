package com.turgo.barangayapp.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "http://localhost:4200")
public class PublicController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health(){
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
