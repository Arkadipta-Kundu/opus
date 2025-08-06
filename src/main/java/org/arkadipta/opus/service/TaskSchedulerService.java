package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.arkadipta.opus.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskSchedulerService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ReminderService reminderService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkAndSendReminders(){

        System.out.println("Checking for pending reminders at: " + LocalDateTime.now());

        try {
            // find tasks need reminders
            List<Task> needReminder = findTasksNeedingReminders();

            if (needReminder.isEmpty()) {
                System.out.println("No reminders to send at this time.");
                return;
            }

            System.out.println("Found " + needReminder.size() + " tasks needing reminders");

            // Send reminders for each task
            for (Task task : needReminder) {
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
}
