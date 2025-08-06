package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = "users", key = "'all_users'")
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER"));
        return userRepository.save(user);
    }

    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = "users", key = "'all_users'")
    public User createAdminUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("ADMIN"));
        return userRepository.save(user);
    }

    @Cacheable(value = "users", key = "#Id")
    public User getById(Long Id) {
        return userRepository.findById(Id).orElse(null);
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "#Id"),
            @CacheEvict(value = "users", key = "'all_users'")
    })
    public void deleteUser(Long Id) {
        userRepository.deleteById(Id);
    }

    @Cacheable(value = "users", key = "'all_users'")
    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    @CachePut(value = "users", key = "#result.id")
    public User updateUser(User updatedUser, Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setName(updatedUser.getName());
            user.setUserName(updatedUser.getUserName());
            // Encode password if it's being updated
            if (!updatedUser.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            user.setEmail(updatedUser.getEmail());
            return userRepository.save(user);
        } else {
            return null;
        }
    }

    public boolean isEmailVerified(String email) {
        User user = userRepository.findByEmail(email);
        return user != null && user.isEmailVerified();
    }

    public User findByUsername(String username) {
        return userRepository.findByUserName(username);
    }
}
