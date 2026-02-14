package com.taskmanagement.service;

import com.taskmanagement.model.dto.UserCreateDto;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User createUser(UserCreateDto userCreate) {
        User user = new User();

        user.name = userCreate.getName();
        user.email = userCreate.getEmail();
        user.active = true;
        user.createdAt = new Date().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return userRepository.save(user);
    }
}
