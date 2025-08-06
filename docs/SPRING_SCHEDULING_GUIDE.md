# Spring Boot Scheduling Guide for Opus Application

## Table of Contents

1. [What is Spring Scheduling?](#what-is-spring-scheduling)
2. [Setup and Configuration](#setup-and-configuration)
3. [Types of Scheduling](#types-of-scheduling)
4. [Scheduling Annotations Explained](#scheduling-annotations-explained)
5. [Cron Expressions Guide](#cron-expressions-guide)
6. [Dynamic Scheduling](#dynamic-scheduling)
7. [Practical Implementation: Email Task Reminders](#practical-implementation-email-task-reminders)
8. [Best Practices](#best-practices)
9. [Testing and Monitoring](#testing-and-monitoring)

## What is Spring Scheduling?

Spring Scheduling allows you to execute methods automatically at specified times or intervals. It's perfect for:

- **Background Tasks**: Cleanup operations, data synchronization
- **Periodic Reports**: Daily/weekly email reports
- **Maintenance Jobs**: Database cleanup, log rotation
- **Reminders**: Email notifications, task reminders
- **Data Processing**: Batch processing, file imports

## Setup and Configuration

### 1. Enable Scheduling

Add `@EnableScheduling` to your main application class or configuration:

```java
@SpringBootApplication
@EnableScheduling  // Enable Spring's scheduling features
public class OpusApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpusApplication.class, args);
    }
}
```

### 2. Configure Thread Pool (Optional but Recommended)

```java
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(5); // 5 threads for scheduled tasks
    }
}
```

## Types of Scheduling

### 1. Fixed Rate Scheduling

Executes at fixed intervals, regardless of execution time.

```java
@Scheduled(fixedRate = 5000) // Every 5 seconds
public void fixedRateTask() {
    System.out.println("Fixed rate task - " + new Date());
}
```

### 2. Fixed Delay Scheduling

Waits for specified delay AFTER the previous execution completes.

```java
@Scheduled(fixedDelay = 5000) // 5 seconds after previous execution ends
public void fixedDelayTask() {
    System.out.println("Fixed delay task - " + new Date());
}
```

### 3. Initial Delay

Adds delay before the first execution.

```java
@Scheduled(fixedRate = 10000, initialDelay = 5000) // Wait 5s, then every 10s
public void taskWithInitialDelay() {
    System.out.println("Task with initial delay - " + new Date());
}
```

### 4. Cron Expression Scheduling

Most flexible - uses cron expressions for complex scheduling.

```java
@Scheduled(cron = "0 0 8 * * MON-FRI") // Every weekday at 8 AM
public void weekdayMorningTask() {
    System.out.println("Good morning! - " + new Date());
}
```

## Scheduling Annotations Explained

### @Scheduled

The main annotation for scheduling methods.

**Parameters:**

- `fixedRate`: Fixed interval in milliseconds
- `fixedDelay`: Delay after previous execution in milliseconds
- `initialDelay`: Initial delay before first execution
- `cron`: Cron expression for complex scheduling
- `zone`: Time zone for cron expressions

**Examples:**

```java
// Every 30 seconds
@Scheduled(fixedRate = 30000)

// 10 seconds after previous execution ends
@Scheduled(fixedDelay = 10000)

// Every day at 2 AM
@Scheduled(cron = "0 0 2 * * *")

// Every Monday at 9 AM in New York timezone
@Scheduled(cron = "0 0 9 * * MON", zone = "America/New_York")
```

### @Async (Optional)

Makes scheduled methods run asynchronously.

```java
@Scheduled(fixedRate = 5000)
@Async
public void asyncTask() {
    // This runs in a separate thread
}
```

## Cron Expressions Guide

### Cron Format

```
┌───────────── second (0-59)
│ ┌─────────── minute (0-59)
│ │ ┌───────── hour (0-23)
│ │ │ ┌─────── day of month (1-31)
│ │ │ │ ┌───── month (1-12)
│ │ │ │ │ ┌─── day of week (0-7) (0 or 7 = Sunday)
│ │ │ │ │ │
* * * * * *
```

### Common Cron Examples

| Expression             | Description                                    |
| ---------------------- | ---------------------------------------------- |
| `0 0 12 * * *`         | Every day at 12:00 PM                          |
| `0 15 10 * * *`        | Every day at 10:15 AM                          |
| `0 0 * * * *`          | Every hour                                     |
| `0 */15 * * * *`       | Every 15 minutes                               |
| `0 0 8-18 * * MON-FRI` | Every hour from 8 AM to 6 PM, Monday to Friday |
| `0 0 9 * * MON`        | Every Monday at 9 AM                           |
| `0 0 0 1 * *`          | First day of every month at midnight           |
| `0 0 0 1 1 *`          | Every year on January 1st at midnight          |

### Special Characters

- `*`: Any value
- `?`: No specific value (day fields only)
- `-`: Range (e.g., `1-5`)
- `,`: List (e.g., `MON,WED,FRI`)
- `/`: Step values (e.g., `*/15` = every 15)
- `L`: Last (e.g., `L` in day = last day of month)
- `W`: Weekday nearest to given day
- `#`: Nth occurrence (e.g., `FRI#2` = 2nd Friday)

### Advanced Cron Examples

```java
// Every 30 seconds
@Scheduled(cron = "*/30 * * * * *")

// Every weekday at 8:30 AM
@Scheduled(cron = "0 30 8 * * MON-FRI")

// Last day of every month at 11:59 PM
@Scheduled(cron = "0 59 23 L * *")

// Second Friday of every month at 2 PM
@Scheduled(cron = "0 0 14 * * FRI#2")

// Every 2 hours between 9 AM and 5 PM on weekdays
@Scheduled(cron = "0 0 9-17/2 * * MON-FRI")
```

## Dynamic Scheduling

For scheduling tasks at runtime (not compile-time), use `TaskScheduler`:

### 1. Basic Dynamic Scheduling

```java
@Service
public class DynamicSchedulingService {

    @Autowired
    private TaskScheduler taskScheduler;

    public ScheduledFuture<?> scheduleTask(Runnable task, Date startTime) {
        return taskScheduler.schedule(task, startTime);
    }

    public ScheduledFuture<?> scheduleTaskWithCron(Runnable task, String cronExpression) {
        return taskScheduler.schedule(task, new CronTrigger(cronExpression));
    }

    public ScheduledFuture<?> schedulePeriodicTask(Runnable task, long period) {
        return taskScheduler.scheduleAtFixedRate(task, period);
    }
}
```

### 2. Task Scheduler Configuration

```java
@Configuration
public class TaskSchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("dynamic-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
```

## Practical Implementation: Email Task Reminders

Let's implement a system where users can schedule email reminders for their tasks.

### 1. Create Task Reminder Entity

```java
@Entity
@Table(name = "task_reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime reminderTime;

    @Column(nullable = false)
    private String reminderMessage;

    @Enumerated(EnumType.STRING)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime sentAt;
}
```

### 2. Create Reminder Status Enum

```java
public enum ReminderStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED
}
```

### 3. Create Repository

```java
@Repository
public interface TaskReminderRepository extends JpaRepository<TaskReminder, Long> {

    List<TaskReminder> findByStatusAndReminderTimeBefore(
        ReminderStatus status, LocalDateTime time);

    List<TaskReminder> findByTaskIdAndStatus(Long taskId, ReminderStatus status);

    List<TaskReminder> findByUserIdAndStatus(Long userId, ReminderStatus status);
}
```

### 4. Create Task Reminder Service

```java
@Service
@Slf4j
public class TaskReminderService {

    @Autowired
    private TaskReminderRepository reminderRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TaskScheduler taskScheduler;

    @Cacheable(value = "reminders", key = "#userId")
    public List<TaskReminder> getUserReminders(Long userId) {
        return reminderRepository.findByUserIdAndStatus(userId, ReminderStatus.PENDING);
    }

    @CachePut(value = "reminders", key = "#result.user.id")
    public TaskReminder scheduleReminder(Task task, User user,
                                       LocalDateTime reminderTime, String message) {

        TaskReminder reminder = new TaskReminder();
        reminder.setTask(task);
        reminder.setUser(user);
        reminder.setReminderTime(reminderTime);
        reminder.setReminderMessage(message);
        reminder.setStatus(ReminderStatus.PENDING);

        TaskReminder savedReminder = reminderRepository.save(reminder);

        // Schedule the actual email sending
        scheduleEmailReminder(savedReminder);

        log.info("Scheduled reminder {} for task {} at {}",
                savedReminder.getId(), task.getTaskId(), reminderTime);

        return savedReminder;
    }

    private void scheduleEmailReminder(TaskReminder reminder) {
        Date scheduleTime = Date.from(reminder.getReminderTime()
                .atZone(ZoneId.systemDefault()).toInstant());

        Runnable emailTask = () -> sendReminderEmail(reminder);

        taskScheduler.schedule(emailTask, scheduleTime);
    }

    private void sendReminderEmail(TaskReminder reminder) {
        try {
            String subject = "Task Reminder: " + reminder.getTask().getTaskTitle();
            String body = buildEmailBody(reminder);

            emailService.sendEmail(
                reminder.getUser().getEmail(),
                subject,
                body
            );

            // Update reminder status
            reminder.setStatus(ReminderStatus.SENT);
            reminder.setSentAt(LocalDateTime.now());
            reminderRepository.save(reminder);

            log.info("Sent reminder email for task {} to {}",
                    reminder.getTask().getTaskId(),
                    reminder.getUser().getEmail());

        } catch (Exception e) {
            reminder.setStatus(ReminderStatus.FAILED);
            reminderRepository.save(reminder);

            log.error("Failed to send reminder email for task {}: {}",
                     reminder.getTask().getTaskId(), e.getMessage());
        }
    }

    private String buildEmailBody(TaskReminder reminder) {
        Task task = reminder.getTask();
        return String.format("""
            Hi %s,

            This is a reminder about your task:

            Task: %s
            Description: %s
            Status: %s
            Due Date: %s

            Custom Message: %s

            Best regards,
            Opus Task Management System
            """,
            reminder.getUser().getName(),
            task.getTaskTitle(),
            task.getTaskDesc(),
            task.getTaskStatus(),
            task.getDate(),
            reminder.getReminderMessage()
        );
    }

    @CacheEvict(value = "reminders", key = "#userId")
    public void cancelReminder(Long reminderId, Long userId) {
        TaskReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found"));

        if (!reminder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this reminder");
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);

        log.info("Cancelled reminder {} for user {}", reminderId, userId);
    }
}
```

### 5. Create Scheduled Cleanup Service

```java
@Service
@Slf4j
public class ReminderCleanupService {

    @Autowired
    private TaskReminderRepository reminderRepository;

    // Clean up old reminders every day at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldReminders() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        List<TaskReminder> oldReminders = reminderRepository
                .findByStatusAndReminderTimeBefore(ReminderStatus.SENT, cutoffDate);

        reminderRepository.deleteAll(oldReminders);

        log.info("Cleaned up {} old reminders", oldReminders.size());
    }

    // Process pending reminders every minute
    @Scheduled(fixedRate = 60000) // Every minute
    public void processPendingReminders() {
        LocalDateTime now = LocalDateTime.now();

        List<TaskReminder> overdueReminders = reminderRepository
                .findByStatusAndReminderTimeBefore(ReminderStatus.PENDING, now);

        if (!overdueReminders.isEmpty()) {
            log.info("Found {} overdue reminders to process", overdueReminders.size());

            // In case some scheduled tasks failed, we can process them here
            // This is a backup mechanism
        }
    }
}
```

### 6. Create REST Controller

```java
@RestController
@RequestMapping("/api/reminders")
@Slf4j
public class TaskReminderController {

    @Autowired
    private TaskReminderService reminderService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @PostMapping("/schedule")
    public ResponseEntity<TaskReminder> scheduleReminder(
            @RequestBody ReminderRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            Task task = taskService.getTaskById(request.getTaskId());

            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate reminder time is in the future
            if (request.getReminderTime().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().build();
            }

            TaskReminder reminder = reminderService.scheduleReminder(
                task, user, request.getReminderTime(), request.getMessage());

            return ResponseEntity.ok(reminder);

        } catch (Exception e) {
            log.error("Error scheduling reminder: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/my-reminders")
    public ResponseEntity<List<TaskReminder>> getMyReminders(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);

        List<TaskReminder> reminders = reminderService.getUserReminders(user.getId());
        return ResponseEntity.ok(reminders);
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> cancelReminder(
            @PathVariable Long reminderId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);

            reminderService.cancelReminder(reminderId, user.getId());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error cancelling reminder: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
```

### 7. Create Request DTO

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderRequest {

    @NotNull
    private Long taskId;

    @NotNull
    @Future
    private LocalDateTime reminderTime;

    @NotBlank
    @Size(max = 500)
    private String message;
}
```

## Best Practices

### 1. Error Handling

```java
@Scheduled(fixedRate = 30000)
public void robustScheduledTask() {
    try {
        // Your task logic here
        performTask();

    } catch (Exception e) {
        log.error("Scheduled task failed: {}", e.getMessage(), e);
        // Don't let exceptions kill the scheduler

        // Optionally send alerts
        sendErrorAlert(e);
    }
}
```

### 2. Conditional Scheduling

```java
@Scheduled(fixedRate = 60000)
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public void conditionalTask() {
    // Only runs if scheduling.enabled=true in properties
}
```

### 3. Monitoring and Metrics

```java
@Service
public class ScheduledTaskMetrics {

    private final AtomicLong taskExecutionCount = new AtomicLong(0);
    private final AtomicLong lastExecutionTime = new AtomicLong(0);

    @Scheduled(fixedRate = 30000)
    public void monitoredTask() {
        long startTime = System.currentTimeMillis();

        try {
            // Task logic
            performTask();

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            taskExecutionCount.incrementAndGet();
            lastExecutionTime.set(executionTime);

            log.info("Task executed in {}ms. Total executions: {}",
                    executionTime, taskExecutionCount.get());
        }
    }

    public long getExecutionCount() {
        return taskExecutionCount.get();
    }

    public long getLastExecutionTime() {
        return lastExecutionTime.get();
    }
}
```

### 4. Configuration Properties

```properties
# application.properties

# Enable/disable scheduling
scheduling.enabled=true

# Thread pool configuration
spring.task.scheduling.pool.size=5
spring.task.scheduling.thread-name-prefix=scheduled-task-

# Reminder cleanup settings
app.reminder.cleanup.enabled=true
app.reminder.cleanup.retention-days=30
```

## Testing and Monitoring

### 1. Test Scheduled Methods

```java
@TestMethodOrder(OrderAnnotation.class)
class ScheduledTaskTest {

    @Autowired
    private ReminderCleanupService cleanupService;

    @Test
    @Order(1)
    void testCleanupOldReminders() {
        // Setup test data

        // Execute the scheduled method directly
        cleanupService.cleanupOldReminders();

        // Verify results
        // Assert cleanup worked correctly
    }

    @Test
    @Order(2)
    void testProcessPendingReminders() {
        cleanupService.processPendingReminders();
        // Verify processing logic
    }
}
```

### 2. Monitor Scheduled Tasks

```java
@RestController
@RequestMapping("/api/admin/scheduling")
public class SchedulingMonitorController {

    @Autowired
    private ScheduledTaskMetrics metrics;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", true);
        status.put("executionCount", metrics.getExecutionCount());
        status.put("lastExecutionTime", metrics.getLastExecutionTime());
        status.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(status);
    }
}
```

### 3. Logging Configuration

```properties
# Enhanced logging for scheduled tasks
logging.level.org.springframework.scheduling=DEBUG
logging.level.com.yourapp.scheduling=DEBUG

# Log pattern to include thread names
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

## Quick Reference

### Common Scheduling Patterns

| Use Case                       | Annotation                               | Example                      |
| ------------------------------ | ---------------------------------------- | ---------------------------- |
| **Every X seconds**            | `@Scheduled(fixedRate = X000)`           | `fixedRate = 30000`          |
| **X seconds after completion** | `@Scheduled(fixedDelay = X000)`          | `fixedDelay = 10000`         |
| **Daily at specific time**     | `@Scheduled(cron = "0 0 H * * *")`       | `cron = "0 0 8 * * *"`       |
| **Weekdays only**              | `@Scheduled(cron = "0 0 H * * MON-FRI")` | `cron = "0 0 9 * * MON-FRI"` |
| **Every X minutes**            | `@Scheduled(cron = "0 */X * * * *")`     | `cron = "0 */15 * * * *"`    |

### Troubleshooting Checklist

- ✅ Is `@EnableScheduling` present?
- ✅ Is the method in a Spring-managed bean (`@Service`, `@Component`)?
- ✅ Is the method public?
- ✅ Does the method return void?
- ✅ Does the method have no parameters?
- ✅ Are cron expressions valid?
- ✅ Is the application time zone correct?
- ✅ Are there any exceptions in logs?

This comprehensive guide covers everything you need to implement effective scheduling in your Opus application, from basic fixed-rate tasks to complex dynamic email reminders!
