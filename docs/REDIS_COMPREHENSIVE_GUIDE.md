# Redis Comprehensive Guide for Spring Boot

## Table of Contents

1. [What is Redis?](#what-is-redis)
2. [How Redis Works](#how-redis-works)
3. [Redis Data Types and Operations](#redis-data-types-and-operations)
4. [Redis with Spring Boot - From Scratch](#redis-with-spring-boot---from-scratch)
5. [Implementing Redis in Your Opus Project](#implementing-redis-in-your-opus-project)
6. [Performance Optimization Strategies](#performance-optimization-strategies)
7. [Best Practices](#best-practices)

## What is Redis?

**Redis** (Remote Dictionary Server) is an open-source, in-memory data structure store that can be used as a database, cache, and message broker. It's known for its exceptional performance, supporting sub-millisecond response times.

### Key Features:

- **In-Memory Storage**: Data is stored in RAM for ultra-fast access
- **Persistence**: Optional data persistence to disk
- **Data Structures**: Supports various data types (strings, hashes, lists, sets, etc.)
- **Atomic Operations**: All operations are atomic
- **Pub/Sub**: Built-in publish/subscribe messaging
- **Clustering**: Horizontal scaling support
- **Lua Scripting**: Server-side scripting capabilities

### Use Cases:

- **Caching**: Session storage, page caching, API response caching
- **Real-time Analytics**: Leaderboards, counters, statistics
- **Message Queues**: Task queues, pub/sub systems
- **Session Management**: User sessions in web applications
- **Rate Limiting**: API rate limiting and throttling

## How Redis Works

### Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Client App    │───▶│   Redis Server   │───▶│  Optional Disk  │
│  (Spring Boot)  │    │   (In-Memory)    │    │   Persistence   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Memory Management

- **LRU Eviction**: Least Recently Used items are removed when memory is full
- **TTL (Time To Live)**: Automatic expiration of keys
- **Memory Optimization**: Efficient data encoding and compression

### Persistence Options

1. **RDB (Redis Database)**: Point-in-time snapshots
2. **AOF (Append Only File)**: Logs every write operation
3. **Mixed**: Combination of RDB and AOF

## Redis Data Types and Operations

### 1. Strings

Most basic Redis data type, can store text, numbers, or binary data.

```bash
# Set and get operations
SET user:1000:name "John Doe"
GET user:1000:name

# Increment operations
SET counter 100
INCR counter          # Returns 101
INCRBY counter 5      # Returns 106

# Expiration
SETEX session:abc123 3600 "user_data"  # Expires in 1 hour
TTL session:abc123                       # Check remaining time
```

### 2. Hashes

Perfect for representing objects with multiple fields.

```bash
# Set hash fields
HMSET user:1000 name "John Doe" email "john@example.com" age 30

# Get hash fields
HGET user:1000 name
HGETALL user:1000

# Check if field exists
HEXISTS user:1000 email
```

### 3. Lists

Ordered collections of strings, useful for queues and stacks.

```bash
# Add elements
LPUSH mylist "first"      # Add to left (beginning)
RPUSH mylist "last"       # Add to right (end)

# Get elements
LRANGE mylist 0 -1        # Get all elements
LPOP mylist               # Remove and return first element
```

### 4. Sets

Unordered collections of unique strings.

```bash
# Add members
SADD myset "apple" "banana" "orange"

# Get members
SMEMBERS myset
SCARD myset               # Get count

# Set operations
SINTER set1 set2          # Intersection
SUNION set1 set2          # Union
```

### 5. Sorted Sets

Sets ordered by score, perfect for leaderboards.

```bash
# Add with scores
ZADD leaderboard 100 "player1" 85 "player2" 92 "player3"

# Get by rank
ZRANGE leaderboard 0 2 WITHSCORES    # Top 3 players
ZREVRANGE leaderboard 0 2            # Reverse order (highest first)
```

## Redis with Spring Boot - From Scratch

### Step 1: Add Dependencies

Add to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Starter Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Lettuce (Redis Java client) - included by default -->
    <!-- Or use Jedis if preferred -->
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
    </dependency>

    <!-- Spring Boot Cache Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- JSON processing for object caching -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### Step 2: Configure Redis Connection

**application.properties:**

```properties
# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
spring.data.redis.timeout=60000ms

# Connection Pool Settings (Lettuce)
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
spring.data.redis.lettuce.pool.max-wait=-1ms

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.cache.redis.cache-null-values=false
```

### Step 3: Redis Configuration Class

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6379)
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        // JSON serialization
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
            new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LazyLoadingAspectSupport.AUTO_TYPE);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // Set serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());

        return builder.build();
    }

    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)));
    }
}
```

### Step 4: Using Redis for Caching

#### Declarative Caching with Annotations

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "users", key = "#email")
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @CachePut(value = "users", key = "#user.id")
    public User save(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void clearCache() {
        // Method implementation
    }
}
```

#### Programmatic Caching with RedisTemplate

```java
@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setHash(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Object getHash(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public void addToList(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

## Implementing Redis in Your Opus Project

Based on your project structure, here's how to implement Redis caching for optimal performance:

### 1. Add Redis Dependencies to Your Project

Add these dependencies to your existing `pom.xml`:

```xml
<!-- Add after your existing dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 2. Update Application Properties

Add to your `application.properties`:

```properties
# Redis Configuration
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.database=0
spring.data.redis.timeout=60000ms

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.cache.redis.cache-null-values=false

# Redis Pool Configuration
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

### 3. Create Redis Configuration

Create `src/main/java/org/arkadipta/opus/config/RedisConfig.java`:

```java
package org.arkadipta.opus.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON serialization for complex objects
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
            new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // String serialization for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Set serializers
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }
}
```

### 4. Update Your Services for Caching

#### User Service Caching

Update your existing `UserService` to include caching:

```java
package org.arkadipta.opus.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    // Your existing dependencies...

    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "users", key = "#email", unless = "#result == null")
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @CachePut(value = "users", key = "#result.id")
    public User save(User user) {
        User savedUser = userRepository.save(user);
        // Also cache by email
        return savedUser;
    }

    @CacheEvict(value = "users", key = "#id")
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void clearUserCache() {
        // This will clear all user cache entries
    }

    // Cache user verification status
    @Cacheable(value = "user-verification", key = "#email", unless = "#result == null")
    public boolean isEmailVerified(String email) {
        User user = findByEmail(email);
        return user != null && user.isEmailVerified();
    }

    @CacheEvict(value = "user-verification", key = "#email")
    public void clearEmailVerificationCache(String email) {
        // Clear verification cache when status changes
    }
}
```

#### Task Service Caching

Update your `TaskService` for caching tasks:

```java
package org.arkadipta.opus.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    // Your existing dependencies...

    @Cacheable(value = "tasks", key = "#id", unless = "#result == null")
    public Task findById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "user-tasks", key = "#userId", unless = "#result == null or #result.isEmpty()")
    public List<Task> findTasksByUserId(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    @Cacheable(value = "task-status", key = "#status.name()", unless = "#result == null or #result.isEmpty()")
    public List<Task> findTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    @CachePut(value = "tasks", key = "#result.id")
    public Task save(Task task) {
        Task savedTask = taskRepository.save(task);
        // Clear related caches
        clearUserTasksCache(savedTask.getUserId());
        clearTaskStatusCache(savedTask.getStatus());
        return savedTask;
    }

    @CacheEvict(value = "tasks", key = "#id")
    public void deleteById(Long id) {
        Task task = findById(id);
        if (task != null) {
            taskRepository.deleteById(id);
            clearUserTasksCache(task.getUserId());
            clearTaskStatusCache(task.getStatus());
        }
    }

    @CacheEvict(value = "user-tasks", key = "#userId")
    public void clearUserTasksCache(Long userId) {
        // Clear user-specific task cache
    }

    @CacheEvict(value = "task-status", key = "#status.name()")
    public void clearTaskStatusCache(TaskStatus status) {
        // Clear status-specific task cache
    }

    @CacheEvict(value = {"tasks", "user-tasks", "task-status"}, allEntries = true)
    public void clearAllTaskCaches() {
        // Clear all task-related caches
    }
}
```

#### Quote Service Caching

Cache external API responses:

```java
package org.arkadipta.opus.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

    // Cache quotes for 1 hour (configured in cache manager)
    @Cacheable(value = "quotes", key = "'daily-quote'")
    public QuoteResponse getDailyQuote() {
        // Your existing quote fetching logic
        return fetchQuoteFromExternalAPI();
    }

    @Cacheable(value = "quotes", key = "#category")
    public QuoteResponse getQuoteByCategory(String category) {
        return fetchQuoteByCategory(category);
    }

    // Your existing methods...
}
```

### 5. Session Management with Redis

Create a session service for better session handling:

```java
package org.arkadipta.opus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final String OTP_PREFIX = "otp:";
    private static final long SESSION_TIMEOUT = 24; // hours
    private static final long OTP_TIMEOUT = 10; // minutes

    public void createSession(String sessionId, Object userData) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, userData, SESSION_TIMEOUT, TimeUnit.HOURS);
    }

    public Object getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key);
    }

    public void invalidateSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    public void storeOTP(String email, String otp) {
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_TIMEOUT, TimeUnit.MINUTES);
    }

    public String getOTP(String email) {
        String key = OTP_PREFIX + email;
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void clearOTP(String email) {
        String key = OTP_PREFIX + email;
        redisTemplate.delete(key);
    }

    public boolean isOTPValid(String email, String providedOTP) {
        String storedOTP = getOTP(email);
        return storedOTP != null && storedOTP.equals(providedOTP);
    }
}
```

### 6. Rate Limiting Implementation

Create a rate limiting service:

```java
package org.arkadipta.opus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public boolean isAllowed(String identifier, int maxRequests, int timeWindowMinutes) {
        String key = RATE_LIMIT_PREFIX + identifier;

        String currentCount = (String) redisTemplate.opsForValue().get(key);

        if (currentCount == null) {
            // First request
            redisTemplate.opsForValue().set(key, "1", timeWindowMinutes, TimeUnit.MINUTES);
            return true;
        }

        int count = Integer.parseInt(currentCount);
        if (count < maxRequests) {
            redisTemplate.opsForValue().increment(key);
            return true;
        }

        return false; // Rate limit exceeded
    }

    public void resetLimit(String identifier) {
        String key = RATE_LIMIT_PREFIX + identifier;
        redisTemplate.delete(key);
    }
}
```

### 7. Update Controllers for Rate Limiting

Add rate limiting to your authentication endpoints:

```java
// In your AuthController
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private RateLimitingService rateLimitingService;

    @PostMapping("/user-varification/forget-password")
    public ResponseEntity<?> forgetPassword(@RequestParam String email) {
        // Rate limiting: 3 requests per hour per email
        if (!rateLimitingService.isAllowed("forgot_password:" + email, 3, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Too many password reset requests. Please try again later.");
        }

        // Your existing logic...
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        // Rate limiting: 5 login attempts per 15 minutes per IP
        String clientIP = getClientIP(); // Implement this method
        if (!rateLimitingService.isAllowed("login:" + clientIP, 5, 15)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Too many login attempts. Please try again later.");
        }

        // Your existing logic...
    }
}
```

## Performance Optimization Strategies

### 1. Cache Strategies

#### Cache-Aside Pattern

```java
public User getUserById(Long id) {
    // Check cache first
    User user = (User) redisTemplate.opsForValue().get("user:" + id);
    if (user == null) {
        // Cache miss - fetch from database
        user = userRepository.findById(id).orElse(null);
        if (user != null) {
            // Store in cache
            redisTemplate.opsForValue().set("user:" + id, user, Duration.ofMinutes(30));
        }
    }
    return user;
}
```

#### Write-Through Pattern

```java
public User saveUser(User user) {
    // Save to database first
    User savedUser = userRepository.save(user);

    // Update cache
    redisTemplate.opsForValue().set("user:" + savedUser.getId(), savedUser, Duration.ofMinutes(30));

    return savedUser;
}
```

#### Write-Behind Pattern

```java
@Async
public void updateUserAsync(User user) {
    // Update cache immediately
    redisTemplate.opsForValue().set("user:" + user.getId(), user, Duration.ofMinutes(30));

    // Update database asynchronously
    userRepository.save(user);
}
```

### 2. Optimizing Cache Keys

```java
public class CacheKeyGenerator {

    public static String userKey(Long userId) {
        return "user:" + userId;
    }

    public static String userEmailKey(String email) {
        return "user:email:" + email;
    }

    public static String userTasksKey(Long userId) {
        return "user:" + userId + ":tasks";
    }

    public static String tasksByStatusKey(TaskStatus status) {
        return "tasks:status:" + status.name();
    }

    public static String sessionKey(String sessionId) {
        return "session:" + sessionId;
    }
}
```

### 3. Batch Operations

```java
@Service
public class BatchCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void batchSetUsers(Map<Long, User> users) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            users.forEach((id, user) -> {
                redisTemplate.opsForValue().set("user:" + id, user, Duration.ofMinutes(30));
            });
            return null;
        });
    }

    public List<User> batchGetUsers(List<Long> userIds) {
        List<String> keys = userIds.stream()
            .map(id -> "user:" + id)
            .collect(Collectors.toList());

        List<Object> results = redisTemplate.opsForValue().multiGet(keys);
        return results.stream()
            .map(obj -> (User) obj)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

### 4. Cache Warming

```java
@Component
public class CacheWarmer {

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        // Warm up frequently accessed data
        warmUpUsers();
        warmUpTasks();
    }

    private void warmUpUsers() {
        // Load active users into cache
        List<User> activeUsers = userRepository.findActiveUsers();
        activeUsers.forEach(user -> {
            userService.findById(user.getId()); // This will cache the user
        });
    }

    private void warmUpTasks() {
        // Load recent tasks into cache
        Arrays.stream(TaskStatus.values()).forEach(status -> {
            taskService.findTasksByStatus(status); // This will cache tasks by status
        });
    }
}
```

## Best Practices

### 1. Cache Naming Conventions

- Use hierarchical naming: `user:123`, `user:123:tasks`
- Include version numbers: `user:v1:123`
- Use consistent separators (colon `:`)

### 2. TTL (Time To Live) Strategy

```java
public enum CacheTTL {
    SHORT(Duration.ofMinutes(5)),    // Frequently changing data
    MEDIUM(Duration.ofMinutes(30)),  // User sessions, temporary data
    LONG(Duration.ofHours(2)),       // User profiles, settings
    VERY_LONG(Duration.ofHours(24)); // Static data, configurations

    private final Duration duration;

    CacheTTL(Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
    }
}
```

### 3. Cache Size Management

```properties
# Memory usage settings
maxmemory 256mb
maxmemory-policy allkeys-lru

# Eviction settings
lazyfree-lazy-eviction yes
lazyfree-lazy-expire yes
```

### 4. Monitoring and Metrics

```java
@Component
public class CacheMetrics {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 60000) // Every minute
    public void logCacheMetrics() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection connection = factory.getConnection();

        Properties info = connection.info();
        String usedMemory = info.getProperty("used_memory_human");
        String connectedClients = info.getProperty("connected_clients");

        log.info("Redis Metrics - Memory: {}, Clients: {}", usedMemory, connectedClients);

        connection.close();
    }
}
```

### 5. Error Handling

```java
@Service
public class ResilientCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Optional<Object> safeGet(String key) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (Exception e) {
            log.error("Error getting cache key: {}", key, e);
            return Optional.empty();
        }
    }

    public boolean safeSet(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            return true;
        } catch (Exception e) {
            log.error("Error setting cache key: {}", key, e);
            return false;
        }
    }
}
```

### 6. Development vs Production Configuration

**application-dev.properties:**

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=60000  # Shorter TTL for development
```

**application-prod.properties:**

```properties
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.password=${REDIS_PASSWORD}
spring.cache.redis.time-to-live=600000  # Longer TTL for production
```

## Installation and Setup

### Installing Redis

#### Windows:

1. Download Redis from: https://redis.io/download
2. Or use Docker: `docker run -d -p 6379:6379 redis:alpine`

#### Linux/macOS:

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install redis-server

# macOS with Homebrew
brew install redis
```

### Starting Redis

```bash
# Start Redis server
redis-server

# Connect with Redis CLI
redis-cli

# Test connection
ping  # Should return PONG
```

## Testing Your Implementation

### 1. Unit Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=simple" // Use simple cache for testing
})
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void testUserCaching() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // First call - should hit database
        User result1 = userService.findById(1L);

        // Second call - should hit cache
        User result2 = userService.findById(1L);

        // Verify repository was called only once
        verify(userRepository, times(1)).findById(1L);
        assertEquals(result1, result2);
    }
}
```

### 2. Integration Tests

```java
@SpringBootTest
@Testcontainers
class RedisIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("test", "value");
        Object result = redisTemplate.opsForValue().get("test");
        assertEquals("value", result);
    }
}
```

This comprehensive guide should help you implement Redis caching in your Opus project effectively. Start with the basic setup and gradually implement more advanced features based on your specific needs.
