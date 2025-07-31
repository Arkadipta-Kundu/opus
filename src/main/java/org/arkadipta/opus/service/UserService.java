package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.Task;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User getById(Long Id) {
        return userRepository.findById(Id).orElse(null);
    }

    public void deleteUser(Long Id) {
        userRepository.deleteById(Id);
    }


    public List<Task> getAllTasksForUser(String userName) {
        User user = userRepository.findByUserName(userName);
        if (user != null) {
            return user.getTasks();
        }
        return new ArrayList<>();
    }

    public User updateUser(User updatedUser, Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setName(updatedUser.getName());
            user.setUserName(updatedUser.getUserName());
            user.setPassword(updatedUser.getPassword());
            user.setEmail(updatedUser.getEmail());
            return userRepository.save(user);
        } else {
            return null;
        }
    }
}
