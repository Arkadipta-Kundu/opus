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
}
