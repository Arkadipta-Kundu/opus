package org.arkadipta.opus.repository;

import org.arkadipta.opus.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserId(Long userId);

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
