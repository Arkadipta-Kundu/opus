package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.TaskRepository;
import org.arkadipta.opus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {
//    private final Map<Long, Task> taskDB = new HashMap<>();
//    private  long taskIdSeq = 1;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    public List<Task> getAllTasks(){
        return taskRepository.findAll();
    }

    public List<Task> getAllTasksForUser(String userName) {
        User user = userRepository.findByUserName(userName);
        if (user != null) {
            return user.getTasks();
        }
        return new ArrayList<>();
    }

    public Task getTaskByID(Long id){
        return taskRepository.findById(id).orElse(null);
    }

    public Task createTask(Task task) {
        // Set the date if not provided
        if (task.getDate() == null) {
            task.setDate(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    public Task createTaskForUser(Task task, String userName) {
        // 1. Find the user first and check if exists
        User user = userRepository.findByUserName(userName);
        if (user == null) {
            throw new RuntimeException("User not found with username: " + userName);
        }

        // 2. Set up the bidirectional relationship
        task.setUser(user);  // Set the user on the task (foreign key)
        task.setDate(LocalDateTime.now());

        // 3. Save the task (this will automatically update the relationship)
        Task savedTask = taskRepository.save(task);

        return savedTask;
    }

    public void deleteTaskById(Long id){
        taskRepository.deleteById(id);
    }

    public Task updateTask(Long id , Task updatedTask){
        Task task = taskRepository.findById(id).orElse(null);
        if (task != null){
            task.setTaskTitle(updatedTask.getTaskTitle());
            task.setTaskDesc(updatedTask.getTaskDesc());
            task.setTaskStatus(updatedTask.getTaskStatus());
            return taskRepository.save(task);
        }
        return null;
    }
}
