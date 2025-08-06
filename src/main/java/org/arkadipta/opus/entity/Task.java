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
    @Enumerated(EnumType.ORDINAL) // This maps: TODO=0, IN_PROGRESS=1, DONE=2
    private TaskStatus taskStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // foreign key in tasks table
    @JsonBackReference
    private User user;

    // ===== NEW FIELDS FOR REMINDERS =====

    @Column(name = "reminder_date_time")
    private LocalDateTime reminderDateTime; // When to send the reminder

    @Column(name = "reminder_enabled")
    private Boolean reminderEnabled = false; // Whether a reminder is set

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false; // Whether a reminder email was already sent

    @Column(name = "reminder_email")
    private String reminderEmail; // Email to send a reminder to (optional, can use user's email)
}
