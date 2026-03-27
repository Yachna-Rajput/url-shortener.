
package com.example.demo.service;

import java.util.Optional;
import com.example.demo.entity.UrlMapping;
import com.example.demo.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

@Service
public class UrlService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UrlRepository repo;

    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Duration CACHE_TIME = Duration.ofHours(1); // Redis cache expiry

    

    // 🔥 Convert number → short code
    private String encode(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62.charAt((int) (num % 62)));
            num /= 62;
        }
        return sb.reverse().toString();
    }

    // 🔥 Create short URL
  
    public String shortenUrl(String longUrl) {

    UrlMapping url = new UrlMapping();
    url.setLongUrl(longUrl);

    // ✅ Save to get ID
    UrlMapping savedUrl = repo.save(url);

    // ✅ Generate short code from ID
    String shortCode = encode(savedUrl.getId());
    savedUrl.setShortCode(shortCode);

    // ❌ DO NOT call save again
    // ❌ repo.save(savedUrl);
    // ❌ repo.flush();

    // ✅ Redis cache
    try {
        redisTemplate.opsForValue().set(shortCode, longUrl, CACHE_TIME);
    } catch (Exception e) {
        System.out.println("Redis save failed");
    }

    return shortCode;
}
   
    
   
    public String getLongUrl(String shortCode) {
              System.out.println("🚀 METHOD CALLED with: " + shortCode);

    // 🔥 1. Try Redis
    try {
        String longUrl = redisTemplate.opsForValue().get(shortCode);

        if (longUrl != null) {

            // 🔥 IMPORTANT: increment click
            redisTemplate.opsForValue().increment("click:" + shortCode);

            System.out.println("🔥 From Redis: " + longUrl);
            return longUrl;
        }

    } catch (Exception e) {
        System.out.println("Redis not working...");
    }

    // 🔥 2. DB fallback
    Optional<UrlMapping> optionalUrl = repo.findByShortCode(shortCode);

    if (optionalUrl.isPresent()) {

        UrlMapping url = optionalUrl.get();

        // 🔥 DB click update
        url.setClickCount(url.getClickCount() + 1);
        repo.save(url);

        String longUrl = url.getLongUrl();
        System.out.println("💾 From DB: " + longUrl);

        // 🔥 Save URL in Redis
      
        try {
            redisTemplate.opsForValue().set(shortCode, longUrl);
            System.out.println("✅ Saved to Redis: " + shortCode); // 👈 ADD THIS LINE
        } catch (Exception e) {
            System.out.println("Redis save failed");
    }

        // 🔥 IMPORTANT: increment click in Redis
        redisTemplate.opsForValue().increment("click:" + shortCode);

        return longUrl;
    }

    return null;
}
}