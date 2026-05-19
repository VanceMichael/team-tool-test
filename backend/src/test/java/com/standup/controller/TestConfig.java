package com.standup.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.standup.entity.User;
import com.standup.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public CommandLineRunner testDataReader(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        return args -> {
            insertUserIfNotExists(userMapper, passwordEncoder, "leader1", "123456", "组长张三", "LEADER");
            insertUserIfNotExists(userMapper, passwordEncoder, "user2", "123456", "成员二", "MEMBER");
            insertUserIfNotExists(userMapper, passwordEncoder, "member1", "123456", "成员李四", "MEMBER");
            insertUserIfNotExists(userMapper, passwordEncoder, "member2", "123456", "成员王五", "MEMBER");
            insertUserIfNotExists(userMapper, passwordEncoder, "admin1", "123456", "管理员", "ADMIN");
        };
    }

    private void insertUserIfNotExists(UserMapper userMapper, PasswordEncoder passwordEncoder,
                                       String username, String rawPassword, String nickname, String role) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (count > 0) return;

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setAvatar("");
        user.setRole(role);
        user.setDeleted(0);
        userMapper.insert(user);
    }
}
