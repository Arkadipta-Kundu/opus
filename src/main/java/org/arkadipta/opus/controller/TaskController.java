package org.arkadipta.opus.controller;

import org.arkadipta.opus.entity.Task;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.service.TaskService;
import org.arkadipta.opus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<Task>> getTasksForLoggedInUser(Principal principal) {
        if (principal == null)
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        String username = principal.getName();
        List<Task> tasks = taskService.getAllTasksForUser(username);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable long id) {
        Task task = taskService.getTaskByID(id);
        if (task != null) {
            return new ResponseEntity<>(task, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task, Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String username = principal.getName();
        try {
            Task createdTask = taskService.createTaskForUser(task, username);
            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        Task task = taskService.getTaskByID(id);
        if (task != null) {
            taskService.deleteTaskById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task task) {
        Task updatedTask = taskService.updateTask(id, task);
        return updatedTask != null ? new ResponseEntity<>(updatedTask, HttpStatus.OK)
                : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    /**
     * Set a reminder for an existing task
     * POST /api/tasks/{taskId}/reminder
     */
    @PostMapping("/set-reminder")
    public ResponseEntity<?> setTaskReminder(
            @RequestParam Long taskId,
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
    @DeleteMapping("/reminder/{taskId}")
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
     * Get all tasks with reminders for the current user GET /api / tasks * /with-reminders
     */
    @GetMapping("/with-reminders")
    public ResponseEntity<List<Task>> getTasksWithReminders(Principal principal) {
        try {
            // Get a current user (you might need to adjust this based on your user lookup)
            String username = principal.getName();
            User user = userService.findByUsername(username); // Adjust the method name as needed

            List<Task> tasksWithReminders = taskService.getTasksWithReminders(user.getId());
            return ResponseEntity.ok(tasksWithReminders);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
