package com.example.ratelimiter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/data")
    public Map<String, String> getSecureData() {
        return Map.of(
                "status", "Success",
                "message", "You safely accessed protected server data!"
        );
    }
}