package com.standup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.standup.dto.LoginRequest;
import com.standup.dto.LoginResponse;
import com.standup.dto.RegisterRequest;
import com.standup.entity.User;

public interface UserService extends IService<User> {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    User getByUsername(String username);
}
