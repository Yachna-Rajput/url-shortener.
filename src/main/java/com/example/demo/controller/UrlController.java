
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
@RequestMapping("")
@CrossOrigin(origins = "*")
public class UrlController {

    @Autowired
    private UrlService urlService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UrlRepository repo;

    // 🔹 Create short URL (FIXED)
    @PostMapping("/api/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody java.util.Map<String, String> request) {

        String longUrl = request.get("url");
        String shortCode = urlService.shortenUrl(longUrl);

        String shortUrl = "http://localhost:8080/s/" + shortCode;

        return ResponseEntity.ok(shortUrl);
    }

    // 🔹 Analytics
    @GetMapping("/analytics/{shortCode}")
    public String getAnalytics(@PathVariable String shortCode) {

        String clicks = redisTemplate.opsForValue().get("click:" + shortCode);

        if (clicks != null) {
            return "Clicks (from Redis): " + clicks;
        }

        Optional<UrlMapping> url = repo.findByShortCode(shortCode);

        if (url.isPresent()) {
            return "Clicks (from DB): " + url.get().getClickCount();
        }

        return "URL not found";
    }

    // 🔹 Redirect
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