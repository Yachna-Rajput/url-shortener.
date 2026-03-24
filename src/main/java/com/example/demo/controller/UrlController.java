
package com.example.demo.controller;

import com.example.demo.entity.UrlMapping;
import com.example.demo.repository.UrlRepository;
import com.example.demo.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Optional;

@RestController
@RequestMapping("/")
public class UrlController {

    @Autowired
    private UrlService urlService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UrlRepository repo;

    @GetMapping("/analytics/{shortCode}")
public String getAnalytics(@PathVariable String shortCode) {

    // 🔥 Step 1: Check Redis
    String clicks = redisTemplate.opsForValue().get("click:" + shortCode);

    if (clicks != null) {
        return "Clicks (from Redis): " + clicks;
    }

    // 🔥 Step 2: Check DB
    Optional<UrlMapping> url = repo.findByShortCode(shortCode);

    if (url.isPresent()) {
        return "Clicks (from DB): " + url.get().getClickCount();
    }

    // 🔥 Step 3: Not found
    return "URL not found";
}

    // 🔹 Create short URL
    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody String longUrl) {
        String shortCode = urlService.shortenUrl(longUrl);
        return ResponseEntity.ok(shortCode);
    }

    // 🔹 Get original URL by short code (returns plain URL)
    @GetMapping("/get/{shortCode}")
    public ResponseEntity<String> getLongUrl(@PathVariable String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);
        if (longUrl != null) {
            return ResponseEntity.ok(longUrl);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 🔹 Redirect to original URL (like real URL shortener)
    @GetMapping("/s/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);
        if (longUrl != null) {
            return ResponseEntity.status(302)
                    .header("Location", longUrl)
                    .build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}