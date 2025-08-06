package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.TaskRepository;
import org.arkadipta.opus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {
    // private final Map<Long, Task> taskDB = new HashMap<>();
    // private long taskIdSeq = 1;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Cacheable(value = "tasks", key = "'all_tasks'")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Cacheable(value = "tasks", key = "#userName + '_tasks'")
    public List<Task> getAllTasksForUser(String userName) {
        User user = userRepository.findByUserName(userName);
        if (user != null) {
            return user.getTasks();
        }
        return new ArrayList<>();
    }

    @Cacheable(value = "tasks", key = "#id")
    public Task getTaskByID(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @CachePut(value = "tasks", key = "#result.taskId")
    @CacheEvict(value = "tasks", key = "'all_tasks'")
    public Task createTask(Task task) {
        // Set the date if not provided
        if (task.getDate() == null) {
            task.setDate(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    @CachePut(value = "tasks", key = "#result.taskId")
    @CacheEvict(value = "tasks", key = "#userName +'_tasks'")
    public Task createTaskForUser(Task task, String userName) {
        // 1. Find the user first and check if exists
        User user = userRepository.findByUserName(userName);
        if (user == null) {
            throw new RuntimeException("User not found with username: " + userName);
        }

        // 2. Set up the bidirectional relationship
        task.setUser(user); // Set the user on the task (foreign key)
        task.setDate(LocalDateTime.now());

        // 3. Save the task (this will automatically update the relationship)
        Task savedTask = taskRepository.save(task);

        return savedTask;
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "tasks", key = "'all_tasks'")
    })
    public void deleteTaskById(Long id) {
        taskRepository.deleteById(id);
    }

    @CachePut(value = "tasks", key = "#result.taskId")
    @CacheEvict(value = "tasks", key = "'all_tasks'")
    public Task updateTask(Long id, Task updatedTask) {
        Task task = taskRepository.findById(id).orElse(null);
        if (task != null) {
            task.setTaskTitle(updatedTask.getTaskTitle());
            task.setTaskDesc(updatedTask.getTaskDesc());
            task.setTaskStatus(updatedTask.getTaskStatus());
            return taskRepository.save(task);
        }
        return null;
    }

    /**
     * Sets a reminder for a specific task
     */
    @CachePut(value = "tasks", key = "#taskId")
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'all_tasks'"),
            @CacheEvict(value = "tasks", key = "'user_' + #result.user.id + '_reminders'")
    })
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
    @CachePut(value = "tasks", key = "#taskId")
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'all_tasks'"),
            @CacheEvict(value = "tasks", key = "'user_' + #result.user.id + '_reminders'")
    })
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
    @Cacheable(value = "tasks", key = "'user_' + #userId + '_reminders'")
    public List<Task> getTasksWithReminders(Long userId) {
        return taskRepository.findTasksWithRemindersByUserId(userId);
    }

}
