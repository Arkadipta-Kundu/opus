package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

    @Autowired
    private EmailService emailService;

    @Transactional
    public boolean sendTaskReminder(Task task) {

        try {
            // email to send to
            String emailTo = task.getReminderEmail() != null ? task.getReminderEmail() : task.getUser().getEmail();

            // creating the email for a reminder
            String subject = "Reminder for" + task.getTaskTitle();


            // Create an email body
            String body = createReminderEmailBody(task);

            // Send the email using an existing EmailService
            emailService.sendHtmlEmail(emailTo, subject, body);

            System.out.println("Reminder email sent for task: " + task.getTaskTitle() +
                    " to: " + emailTo);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send reminder email for task: " +
                    task.getTaskTitle() + ". Error: " + e.getMessage());
            return false;
        }

    }

    private String createReminderEmailBody(Task task) {
        return """
                <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <h2 style="color: #4CAF50;">Task Reminder</h2>
                            <p>This is a friendly reminder about your task:</p>
                           \s
                            <div style="border: 1px solid #ddd; padding: 20px; margin: 15px 0; border-radius: 8px; background-color: #f9f9f9;">
                                <h3 style="margin: 0; color: #333;">%s</h3>
                                <p style="margin: 5px 0;"><strong>Description:</strong> %s</p>
                                <p style="margin: 5px 0;"><strong>Status:</strong> %s</p>
                                <p style="margin: 5px 0;"><strong>Due Date:</strong> %s</p>
                            </div>
                       \s
                            <p style="margin-top: 20px;">Don't forget to complete this task!</p>
                       \s
                            <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                            <small style="color: #888;">This reminder was sent from your Opus Task Management System.</small>
                        </body>
                        </html>
               \s""".formatted(
                task.getTaskTitle(),
                task.getTaskDesc() != null ? task.getTaskDesc() : "No description",
                task.getTaskStatus().toString(),
                task.getDate() != null ? task.getDate().toString() : "No due date"
        );
    }
}
