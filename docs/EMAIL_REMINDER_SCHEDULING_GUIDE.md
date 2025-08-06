# Email Reminder Scheduling Guide for Opus Application

## Table of Contents

1. [Overview](#overview)
2. [How Spring Boot Scheduling Works](#how-spring-boot-scheduling-works)
3. [Simple Implementation Plan](#simple-implementation-plan)
4. [Step-by-Step Implementation](#step-by-step-implementation)
5. [Database Changes](#database-changes)
6. [Service Layer](#service-layer)
7. [Scheduler Service](#scheduler-service)
8. [Controller Updates](#controller-updates)
9. [Testing the Feature](#testing-the-feature)
10. [How It All Works Together](#how-it-all-works-together)

## Overview

This guide will help you implement a simple email reminder system for tasks where:

- Users can set a custom reminder date and time for any task
- The system automatically sends email reminders at the specified time
- Uses Spring Boot's built-in scheduling features
- Keeps everything simple and easy to understand

## How Spring Boot Scheduling Works

### Basic Concepts:

1. **@EnableScheduling**: Annotation to enable scheduling in your Spring Boot app
2. **@Scheduled**: Annotation to mark methods that should run automatically
3. **Cron Expressions**: Define when scheduled methods should run
4. **FixedRate vs FixedDelay**: Different ways to schedule recurring tasks

### Simple Example:

```java
@Component
public class SimpleScheduler {

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void doSomething() {
        System.out.println("This runs every minute!");
    }

    @Scheduled(cron = "0 0 9 * * *") // Runs every day at 9 AM
    public void dailyTask() {
        System.out.println("Good morning! It's 9 AM");
    }
}
```

## Simple Implementation Plan

### What We'll Build:

1. **Add reminder fields** to Task entity (reminder date/time, email sent flag)
2. **Create ReminderService** to handle email sending logic
3. **Create TaskSchedulerService** that checks for due reminders every minute
4. **Update TaskService** to handle reminder settings
5. **Update TaskController** to accept reminder data
6. **Enable scheduling** in main application

### The Flow:

1. User creates/updates task with reminder time
2. Task is saved with reminder details
3. Scheduler runs every minute checking for due reminders
4. When reminder time is reached, email is sent
5. Task is marked as "reminder sent" to avoid duplicates

## Step-by-Step Implementation

### Step 1: Enable Scheduling in Main Application

**File: `OpusApplication.java`**

```java
package org.arkadipta.opus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling // Add this annotation to enable scheduling
public class OpusApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpusApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**What this does:**

- `@EnableScheduling` tells Spring Boot to look for `@Scheduled` methods and run them automatically
- This is required for any scheduling to work

### Step 2: Update Task Entity

**File: `Task.java`** (Add these fields to your existing Task entity)

```java
package org.arkadipta.opus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    private String taskTitle;
    private String taskDesc;
    private LocalDateTime date;

    @Enumerated(EnumType.ORDINAL)
    private TaskStatus taskStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    // ===== NEW FIELDS FOR REMINDERS =====

    @Column(name = "reminder_date_time")
    private LocalDateTime reminderDateTime; // When to send the reminder

    @Column(name = "reminder_enabled")
    private Boolean reminderEnabled = false; // Whether reminder is set

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false; // Whether reminder email was already sent

    @Column(name = "reminder_email")
    private String reminderEmail; // Email to send reminder to (optional, can use user's email)
}
```

**What these fields do:**

- `reminderDateTime`: Stores exactly when to send the reminder
- `reminderEnabled`: Boolean flag to enable/disable reminder for this task
- `reminderSent`: Prevents sending the same reminder multiple times
- `reminderEmail`: Optional custom email (if null, use user's email)

### Step 3: Create ReminderService

**File: `ReminderService.java`** (New file in service package)

```java
package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {

    @Autowired
    private EmailService emailService;

    /**
     * Sends a reminder email for a specific task
     * @param task The task to send reminder for
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendTaskReminder(Task task) {
        try {
            // Determine which email to use
            String emailTo = task.getReminderEmail() != null ?
                           task.getReminderEmail() :
                           task.getUser().getEmail();

            // Create email subject
            String subject = "Task Reminder: " + task.getTaskTitle();

            // Create email body
            String body = createReminderEmailBody(task);

            // Send the email using existing EmailService
            emailService.sendEmail(emailTo, subject, body);

            System.out.println("Reminder email sent for task: " + task.getTaskTitle() +
                             " to: " + emailTo);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to send reminder email for task: " +
                             task.getTaskTitle() + ". Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates the HTML body for the reminder email
     */
    private String createReminderEmailBody(Task task) {
        return """
            <html>
            <body>
                <h2>Task Reminder</h2>
                <p>This is a friendly reminder about your task:</p>

                <div style="border: 1px solid #ddd; padding: 15px; margin: 10px 0; border-radius: 5px;">
                    <h3>%s</h3>
                    <p><strong>Description:</strong> %s</p>
                    <p><strong>Status:</strong> %s</p>
                    <p><strong>Due Date:</strong> %s</p>
                </div>

                <p>Don't forget to complete this task!</p>

                <hr>
                <small>This reminder was sent from your Opus Task Management System.</small>
            </body>
            </html>
            """.formatted(
                task.getTaskTitle(),
                task.getTaskDesc() != null ? task.getTaskDesc() : "No description",
                task.getTaskStatus().toString(),
                task.getDate() != null ? task.getDate().toString() : "No due date"
            );
    }
}
```

**What this service does:**

- `sendTaskReminder()`: Main method to send reminder emails
- Uses your existing `EmailService` to actually send the email
- Creates a nice HTML email with task details
- Handles errors gracefully
- Determines which email to use (custom or user's email)

### Step 4: Create TaskSchedulerService

**File: `TaskSchedulerService.java`** (New file in service package)

```java
package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.arkadipta.opus.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskSchedulerService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ReminderService reminderService;

    /**
     * This method runs every minute to check for tasks that need reminder emails
     * Cron expression: "0 * * * * *" means run at second 0 of every minute
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkAndSendReminders() {
        System.out.println("Checking for pending reminders at: " + LocalDateTime.now());

        try {
            // Find all tasks that need reminders
            List<Task> tasksNeedingReminders = findTasksNeedingReminders();

            if (tasksNeedingReminders.isEmpty()) {
                System.out.println("No reminders to send at this time.");
                return;
            }

            System.out.println("Found " + tasksNeedingReminders.size() + " tasks needing reminders");

            // Send reminders for each task
            for (Task task : tasksNeedingReminders) {
                boolean emailSent = reminderService.sendTaskReminder(task);

                if (emailSent) {
                    // Mark as sent to avoid sending again
                    task.setReminderSent(true);
                    taskRepository.save(task);
                    System.out.println("✅ Reminder sent and marked for task: " + task.getTaskTitle());
                } else {
                    System.out.println("❌ Failed to send reminder for task: " + task.getTaskTitle());
                }
            }

        } catch (Exception e) {
            System.err.println("Error in reminder scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Finds tasks that need reminders sent
     * Criteria:
     * 1. Reminder is enabled
     * 2. Reminder hasn't been sent yet
     * 3. Current time is >= reminder time
     * 4. Task is not completed (optional)
     */
    private List<Task> findTasksNeedingReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Custom query to find tasks needing reminders
        return taskRepository.findTasksNeedingReminders(now);
    }

    /**
     * Manual method to test the reminder system
     * You can call this from a controller for testing
     */
    public void testReminderSystem() {
        System.out.println("🧪 Testing reminder system manually...");
        checkAndSendReminders();
    }
}
```

**What this scheduler does:**

- `@Scheduled(cron = "0 * * * * *")`: Runs every minute at the 0-second mark
- Finds all tasks that need reminders
- Sends emails for those tasks
- Marks tasks as "reminder sent" to avoid duplicates
- Includes error handling and logging
- Has a test method for manual testing

### Step 5: Update TaskRepository

**File: `TaskRepository.java`** (Add this method to your existing repository)

```java
package org.arkadipta.opus.repository;

import org.arkadipta.opus.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // ===== EXISTING METHODS (keep these) =====
    // Your existing methods stay here...

    // ===== NEW METHOD FOR REMINDERS =====

    /**
     * Finds tasks that need reminder emails sent
     * Criteria:
     * 1. reminderEnabled = true
     * 2. reminderSent = false
     * 3. reminderDateTime <= current time
     * 4. taskStatus != DONE (optional - you can remove this condition)
     */
    @Query("""
        SELECT t FROM Task t
        WHERE t.reminderEnabled = true
        AND t.reminderSent = false
        AND t.reminderDateTime <= :currentTime
        AND t.taskStatus != org.arkadipta.opus.entity.TaskStatus.DONE
        ORDER BY t.reminderDateTime ASC
        """)
    List<Task> findTasksNeedingReminders(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Optional: Find all tasks with reminders set for a user
     */
    @Query("""
        SELECT t FROM Task t
        WHERE t.user.id = :userId
        AND t.reminderEnabled = true
        ORDER BY t.reminderDateTime ASC
        """)
    List<Task> findTasksWithRemindersByUserId(@Param("userId") Long userId);
}
```

**What these queries do:**

- `findTasksNeedingReminders()`: Finds tasks that need reminders sent right now
- Filters by enabled reminders, not yet sent, and due time
- Excludes completed tasks (you can remove this if you want)
- `findTasksWithRemindersByUserId()`: Optional method to show user their scheduled reminders

### Step 6: Update TaskService

**File: `TaskService.java`** (Add these methods to your existing service)

```java
// Add these imports at the top
import java.time.LocalDateTime;

// Add these methods to your existing TaskService class

/**
 * Sets a reminder for a specific task
 */
public Task setTaskReminder(Long taskId, LocalDateTime reminderDateTime, String customEmail) {
    Task task = taskRepository.findById(taskId).orElse(null);
    if (task == null) {
        throw new RuntimeException("Task not found with id: " + taskId);
    }

    // Set reminder details
    task.setReminderDateTime(reminderDateTime);
    task.setReminderEnabled(true);
    task.setReminderSent(false); // Reset in case it was previously sent
    task.setReminderEmail(customEmail); // Can be null to use user's email

    return taskRepository.save(task);
}

/**
 * Removes reminder from a task
 */
public Task removeTaskReminder(Long taskId) {
    Task task = taskRepository.findById(taskId).orElse(null);
    if (task == null) {
        throw new RuntimeException("Task not found with id: " + taskId);
    }

    // Disable reminder
    task.setReminderEnabled(false);
    task.setReminderDateTime(null);
    task.setReminderEmail(null);

    return taskRepository.save(task);
}

/**
 * Gets all tasks with reminders for a user
 */
public List<Task> getTasksWithReminders(Long userId) {
    return taskRepository.findTasksWithRemindersByUserId(userId);
}

/**
 * Updates an existing task with reminder (for create/update operations)
 */
public Task createTaskWithReminder(Task task, LocalDateTime reminderDateTime, String customEmail) {
    // Set the task date if not provided
    if (task.getDate() == null) {
        task.setDate(LocalDateTime.now());
    }

    // Save the task first
    Task savedTask = taskRepository.save(task);

    // Set reminder if provided
    if (reminderDateTime != null) {
        savedTask.setReminderDateTime(reminderDateTime);
        savedTask.setReminderEnabled(true);
        savedTask.setReminderSent(false);
        savedTask.setReminderEmail(customEmail);
        savedTask = taskRepository.save(savedTask);
    }

    return savedTask;
}
```

**What these methods do:**

- `setTaskReminder()`: Adds or updates a reminder for existing task
- `removeTaskReminder()`: Disables reminder for a task
- `getTasksWithReminders()`: Shows user all their tasks with reminders
- `createTaskWithReminder()`: Creates new task with reminder in one step

### Step 7: Update TaskController

**File: `TaskController.java`** (Add these endpoints to your existing controller)

```java
// Add these imports at the top
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.*;

// Add these endpoints to your existing TaskController class

/**
 * Set a reminder for an existing task
 * POST /api/tasks/{taskId}/reminder
 */
@PostMapping("/{taskId}/reminder")
public ResponseEntity<?> setTaskReminder(
        @PathVariable Long taskId,
        @RequestParam String reminderDateTime, // Format: "2024-08-06T14:30:00"
        @RequestParam(required = false) String customEmail) {

    try {
        // Parse the date time string
        LocalDateTime reminderTime = LocalDateTime.parse(reminderDateTime);

        // Validate that reminder time is in the future
        if (reminderTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                .body("Reminder time must be in the future");
        }

        // Set the reminder
        Task updatedTask = taskService.setTaskReminder(taskId, reminderTime, customEmail);

        return ResponseEntity.ok(Map.of(
            "message", "Reminder set successfully",
            "task", updatedTask,
            "reminderTime", reminderTime.toString()
        ));

    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body("Error setting reminder: " + e.getMessage());
    }
}

/**
 * Remove reminder from a task
 * DELETE /api/tasks/{taskId}/reminder
 */
@DeleteMapping("/{taskId}/reminder")
public ResponseEntity<?> removeTaskReminder(@PathVariable Long taskId) {
    try {
        Task updatedTask = taskService.removeTaskReminder(taskId);
        return ResponseEntity.ok(Map.of(
            "message", "Reminder removed successfully",
            "task", updatedTask
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body("Error removing reminder: " + e.getMessage());
    }
}

/**
 * Get all tasks with reminders for current user
 * GET /api/tasks/with-reminders
 */
@GetMapping("/with-reminders")
public ResponseEntity<List<Task>> getTasksWithReminders(Principal principal) {
    try {
        // Get current user (you might need to adjust this based on your user lookup)
        String username = principal.getName();
        User user = userService.findByUsername(username); // Adjust method name as needed

        List<Task> tasksWithReminders = taskService.getTasksWithReminders(user.getId());
        return ResponseEntity.ok(tasksWithReminders);

    } catch (Exception e) {
        return ResponseEntity.badRequest().build();
    }
}

/**
 * Create a new task with reminder
 * POST /api/tasks/with-reminder
 */
@PostMapping("/with-reminder")
public ResponseEntity<?> createTaskWithReminder(
        @RequestBody Task task,
        @RequestParam(required = false) String reminderDateTime,
        @RequestParam(required = false) String customEmail,
        Principal principal) {

    try {
        // Set user for the task
        String username = principal.getName();
        User user = userService.findByUsername(username);
        task.setUser(user);

        // Parse reminder time if provided
        LocalDateTime reminderTime = null;
        if (reminderDateTime != null && !reminderDateTime.isEmpty()) {
            reminderTime = LocalDateTime.parse(reminderDateTime);

            if (reminderTime.isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                    .body("Reminder time must be in the future");
            }
        }

        // Create task with reminder
        Task createdTask = taskService.createTaskWithReminder(task, reminderTime, customEmail);

        return ResponseEntity.ok(Map.of(
            "message", "Task created successfully" +
                      (reminderTime != null ? " with reminder" : ""),
            "task", createdTask
        ));

    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body("Error creating task: " + e.getMessage());
    }
}

/**
 * Test endpoint to manually trigger reminder check (for testing)
 * POST /api/tasks/test-reminders
 */
@PostMapping("/test-reminders")
public ResponseEntity<String> testReminders() {
    try {
        taskSchedulerService.testReminderSystem();
        return ResponseEntity.ok("Reminder system test triggered. Check console logs.");
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body("Error testing reminders: " + e.getMessage());
    }
}
```

**What these endpoints do:**

- `POST /{taskId}/reminder`: Set reminder for existing task
- `DELETE /{taskId}/reminder`: Remove reminder from task
- `GET /with-reminders`: List all tasks with reminders for current user
- `POST /with-reminder`: Create new task with reminder in one step
- `POST /test-reminders`: Manual trigger for testing (remove in production)

## Database Changes

You'll need to add the new columns to your tasks table. Run this SQL:

```sql
-- Add new columns for reminders
ALTER TABLE tasks
ADD COLUMN reminder_date_time TIMESTAMP NULL,
ADD COLUMN reminder_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN reminder_sent BOOLEAN DEFAULT FALSE,
ADD COLUMN reminder_email VARCHAR(255) NULL;

-- Add index for performance (optional but recommended)
CREATE INDEX idx_tasks_reminder_check
ON tasks (reminder_enabled, reminder_sent, reminder_date_time)
WHERE reminder_enabled = true AND reminder_sent = false;
```

## Testing the Feature

### 1. Using Postman/API Testing

**Create task with reminder:**

```
POST http://localhost:8080/api/tasks/with-reminder
Content-Type: application/json

Body:
{
    "taskTitle": "Test Reminder Task",
    "taskDesc": "This task should send a reminder",
    "taskStatus": "TODO"
}

Parameters:
- reminderDateTime: 2024-08-06T15:30:00
- customEmail: your-email@example.com (optional)
```

**Set reminder for existing task:**

```
POST http://localhost:8080/api/tasks/1/reminder

Parameters:
- reminderDateTime: 2024-08-06T15:35:00
- customEmail: your-email@example.com (optional)
```

**Test the scheduler manually:**

```
POST http://localhost:8080/api/tasks/test-reminders
```

### 2. Testing Steps

1. **Create a task** with reminder set to 2-3 minutes in the future
2. **Check console logs** - you should see scheduler running every minute
3. **Wait for reminder time** - email should be sent automatically
4. **Check database** - `reminder_sent` should be `true` after email is sent
5. **Verify email** was received

### 3. Console Output You Should See

```
Checking for pending reminders at: 2024-08-06T15:29:00
No reminders to send at this time.

Checking for pending reminders at: 2024-08-06T15:30:00
Found 1 tasks needing reminders
Reminder email sent for task: Test Reminder Task to: user@example.com
✅ Reminder sent and marked for task: Test Reminder Task
```

## How It All Works Together

### The Complete Flow:

1. **User creates task** with reminder via API

   ```
   Task saved with: reminderDateTime=2024-08-06T15:30:00, reminderEnabled=true, reminderSent=false
   ```

2. **Scheduler runs every minute** (`TaskSchedulerService`)

   ```
   @Scheduled method checks database every minute at second 0
   ```

3. **When reminder time arrives**:

   ```
   - Query finds tasks where reminderDateTime <= now
   - ReminderService sends email using EmailService
   - Task is marked as reminderSent = true
   ```

4. **Email is sent** with task details:

   ```
   Subject: "Task Reminder: Test Reminder Task"
   Body: HTML email with task details
   ```

5. **Task is updated** to prevent duplicate emails:
   ```
   reminderSent = true (won't be found in future queries)
   ```

### Key Components:

- **Task Entity**: Stores reminder data
- **TaskSchedulerService**: Runs every minute checking for due reminders
- **ReminderService**: Handles email creation and sending
- **TaskService**: Business logic for managing reminders
- **TaskController**: API endpoints for users
- **TaskRepository**: Database queries for finding due reminders

### Scheduling Details:

- **Cron Expression**: `"0 * * * * *"` = every minute at second 0
- **Why every minute?**: Simple and responsive enough for most use cases
- **Performance**: Query only finds tasks needing reminders (efficient)
- **Error Handling**: Continues working even if one email fails

### Email Details:

- **Uses existing EmailService**: No need to change email configuration
- **HTML formatting**: Nice looking emails with task details
- **Custom email support**: Can send to different email than user's
- **Error resilience**: Logs errors but continues processing other reminders

This implementation is simple, easy to understand, and fully functional for learning Spring Boot scheduling! 🎯
