package com.standup.controller;

import com.standup.common.Result;
import com.standup.context.UserContext;
import com.standup.entity.User;
import com.standup.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }
}
