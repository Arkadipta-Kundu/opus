# Spring Boot Caching Guide for Opus Application

## Table of Contents

1. [What is Caching?](#what-is-caching)
2. [Setup in Opus Application](#setup-in-opus-application)
3. [Cache Annotations Explained](#cache-annotations-explained)
4. [Understanding `value` and `key` Parameters](#understanding-value-and-key-parameters)
5. [Implementation Examples for Opus](#implementation-examples-for-opus)
6. [Best Practices](#best-practices)
7. [Testing Cache](#testing-cache)

## What is Caching?

Caching is a technique that stores frequently accessed data in memory to improve application performance by reducing:

- Database queries
- External API calls
- Expensive computations

In Spring Boot, caching is implemented using annotations that automatically handle cache storage and retrieval.

## Setup in Opus Application

### 1. Enable Caching

✅ **Already done** - You have `@EnableCaching` in your SpringSecurityConfig.

```java
@Configuration
@EnableCaching  // This enables Spring's caching features
public class SpringSecurityConfig {
    // ... your config
}
```

### 2. Add Cache Manager (Optional)

Spring Boot provides a default simple cache manager, but you can configure custom ones:

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList("users", "tasks", "quotes"));
        return cacheManager;
    }
}
```

### 3. Redis Cache Configuration (Recommended)

✅ **You already have this configured!** Your RedisConfig provides Redis-based caching:

```java
@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure ObjectMapper to handle Java 8 time types (LocalDateTime, etc.)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();

        // Create custom serializer with configured ObjectMapper
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Cache expires after 1 hour
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
}
```

**Benefits of Redis Caching:**

- ✅ **Persistent**: Survives application restarts
- ✅ **Shared**: Multiple app instances can share the same cache
- ✅ **Scalable**: Can handle large amounts of cached data
- ✅ **Configurable TTL**: Automatic expiration after specified time
- ✅ **Java 8 Time Support**: Properly handles LocalDateTime, LocalDate, etc.

**Required Dependencies:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

## Cache Annotations Explained

### @Cacheable

- **Purpose**: Caches the result of a method
- **When to use**: On GET methods that retrieve data
- **Behavior**: If cache exists, returns cached data; otherwise executes method and caches result

### @CachePut

- **Purpose**: Always executes method and updates cache with new result
- **When to use**: On UPDATE methods
- **Behavior**: Method always runs, cache is updated with new value

### @CacheEvict

- **Purpose**: Removes data from cache
- **When to use**: On DELETE methods or when data becomes invalid
- **Behavior**: Removes specified cache entries

### @Caching

- **Purpose**: Groups multiple cache operations
- **When to use**: When you need multiple cache operations on one method

## Understanding `value` and `key` Parameters

### The `value` Parameter

- **What it is**: The **cache name** - like a bucket or container for cached data
- **Think of it as**: A database table name for your cache
- **Example**: `value = "users"` means all user-related cache entries go in the "users" cache

### The `key` Parameter

- **What it is**: The **unique identifier** for a specific cache entry within that cache
- **Think of it as**: A primary key in a database table
- **Example**: `key = "#id"` means use the method parameter `id` as the cache key

### Simple Analogy

```
Cache Name (value) = "users"     // Like a folder
Cache Key (key) = "123"          // Like a file in that folder
Cached Data = User object with ID 123
```

### Examples:

```java
@Cacheable(value = "users", key = "#id")
public User getUserById(Long id) {
    // Cache name: "users"
    // Cache key: actual value of id parameter (e.g., "123")
    // Full cache location: users::123
}

@Cacheable(value = "tasks", key = "#userId + '_' + #status")
public List<Task> getTasksByUserAndStatus(Long userId, String status) {
    // Cache name: "tasks"
    // Cache key: combination like "123_PENDING"
    // Full cache location: tasks::123_PENDING
}
```

## Implementation Examples for Opus

### 1. UserService Implementation

```java
@Service
public class UserService {

    // GET Methods - Use @Cacheable
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "users", key = "'all_users'")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Cacheable(value = "users", key = "#username")
    public User getUserByUsername(String username) {
        return userRepository.findByUserName(username);
    }

    // UPDATE Methods - Use @CachePut
    @CachePut(value = "users", key = "#result.id")
    public User updateUser(User updatedUser, Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setName(updatedUser.getName());
            user.setUserName(updatedUser.getUserName());
            user.setEmail(updatedUser.getEmail());
            return userRepository.save(user);
        }
        return null;
    }

    // CREATE Methods - Use @CachePut
    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = "users", key = "'all_users'") // Clear all users cache
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER"));
        return userRepository.save(user);
    }

    // DELETE Methods - Use @CacheEvict
    @CacheEvict(value = "users", key = "#id")
    @CacheEvict(value = "users", key = "'all_users'") // Also clear all users cache
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

### 2. TaskService Implementation

```java
@Service
public class TaskService {

    // GET Methods
    @Cacheable(value = "tasks", key = "'all_tasks'")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Cacheable(value = "tasks", key = "#id")
    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "tasks", key = "#userName + '_tasks'")
    public List<Task> getAllTasksForUser(String userName) {
        User user = userRepository.findByUserName(userName);
        if (user != null) {
            return user.getTasks();
        }
        return new ArrayList<>();
    }

    // CREATE Methods
    @CachePut(value = "tasks", key = "#result.id")
    @CacheEvict(value = "tasks", key = "'all_tasks'")
    public Task createTask(Task task) {
        if (task.getDate() == null) {
            task.setDate(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    // UPDATE Methods
    @CachePut(value = "tasks", key = "#result.id")
    @CacheEvict(value = "tasks", key = "'all_tasks'")
    public Task updateTask(Long id, Task updatedTask) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task != null) {
            task.setTitle(updatedTask.getTitle());
            task.setDescription(updatedTask.getDescription());
            task.setStatus(updatedTask.getStatus());
            return taskRepository.save(task);
        }
        return null;
    }

    // DELETE Methods
    @CacheEvict(value = "tasks", allEntries = true) // Clear entire tasks cache
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
}
```

### 3. QuoteService Implementation

```java
@Service
public class QuoteService {

    // Cache external API calls for better performance
    @Cacheable(value = "quotes", key = "'random_quote'")
    public QuoteResponse getRandomQuote() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<QuoteResponse[]> response = restTemplate.exchange(
                API_URL, HttpMethod.GET, entity, QuoteResponse[].class);

            QuoteResponse[] quotes = response.getBody();
            if (quotes != null && quotes.length > 0) {
                return quotes[0];
            }
        } catch (Exception e) {
            System.err.println("Error fetching quote: " + e.getMessage());
        }
        return createFallbackQuote();
    }

    // Manually evict cache to get fresh quotes periodically
    @CacheEvict(value = "quotes", allEntries = true)
    public void clearQuoteCache() {
        // This method can be called to refresh quotes
    }
}
```

## Key Parameter Patterns

### Common Key Patterns:

1. **Single Parameter**: `key = "#id"`
2. **Multiple Parameters**: `key = "#userId + '_' + #status"`
3. **Object Properties**: `key = "#user.id"`
4. **Method Result**: `key = "#result.id"` (for @CachePut)
5. **Static String**: `key = "'all_users'"`
6. **Complex Expression**: `key = "#user.id + '_' + #user.role"`

### SpEL (Spring Expression Language) Examples:

```java
// Use parameter
@Cacheable(value = "users", key = "#id")

// Use multiple parameters
@Cacheable(value = "search", key = "#query + '_' + #page + '_' + #size")

// Use object property
@Cacheable(value = "profiles", key = "#user.id")

// Use method result (for @CachePut)
@CachePut(value = "users", key = "#result.id")

// Conditional caching
@Cacheable(value = "users", key = "#id", condition = "#id > 0")

// Use static value
@Cacheable(value = "config", key = "'app_settings'")
```

## Best Practices

### 1. Cache Naming Strategy

- Use descriptive cache names: `"users"`, `"tasks"`, `"quotes"`
- Group related data in same cache
- Keep names consistent across application

### 2. Key Design

- Make keys unique and meaningful
- Include relevant parameters in composite keys
- Use consistent delimiters (e.g., underscore `_`)

### 3. Cache Strategy

- **Read-heavy data**: Use `@Cacheable`
- **Frequently updated data**: Use `@CachePut`
- **Data invalidation**: Use `@CacheEvict`
- **External API calls**: Always cache with reasonable expiration

### 4. Cache Invalidation

```java
// Clear specific entry
@CacheEvict(value = "users", key = "#id")

// Clear all entries in cache
@CacheEvict(value = "users", allEntries = true)

// Multiple cache operations
@Caching(evict = {
    @CacheEvict(value = "users", key = "#user.id"),
    @CacheEvict(value = "profiles", key = "#user.id")
})
```

### 5. Error Handling

```java
@Cacheable(value = "users", key = "#id", unless = "#result == null")
public User getUserById(Long id) {
    // Don't cache null results
    return userRepository.findById(id).orElse(null);
}
```

## Testing Cache

### 1. Add Logging to See Cache Behavior

```properties
# application.properties
logging.level.org.springframework.cache=DEBUG
```

### 2. Create Test Controller

```java
@RestController
public class CacheTestController {

    @Autowired
    private UserService userService;

    @GetMapping("/test-cache/{id}")
    public ResponseEntity<String> testCache(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        User user = userService.getUserById(id);
        long endTime = System.currentTimeMillis();

        return ResponseEntity.ok("User: " + user.getName() +
                                ", Time taken: " + (endTime - startTime) + "ms");
    }
}
```

### 3. Test Cache Behavior

1. **First call**: Should take longer (database query)
2. **Second call**: Should be faster (cached result)
3. **After update**: Cache should be refreshed
4. **After delete**: Cache should be cleared

## Quick Reference

| Operation  | Annotation                  | When to Use    | Key Strategy                  |
| ---------- | --------------------------- | -------------- | ----------------------------- |
| **Read**   | `@Cacheable`                | GET methods    | Use method parameters         |
| **Create** | `@CachePut` + `@CacheEvict` | POST methods   | Use result ID + clear lists   |
| **Update** | `@CachePut`                 | PUT methods    | Use entity ID                 |
| **Delete** | `@CacheEvict`               | DELETE methods | Use entity ID + clear related |

### Example Method Signatures:

```java
// Basic patterns for Opus app
@Cacheable(value = "users", key = "#id")
@CachePut(value = "users", key = "#result.id")
@CacheEvict(value = "users", key = "#id")
@CacheEvict(value = "users", allEntries = true)
```

This guide should help you implement caching effectively in your Opus application! Start with the UserService example and gradually add caching to other services.
