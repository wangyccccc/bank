package com.example.bank.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bank.entity.User;
import com.example.bank.entity.enums.UserRole;
import com.example.bank.mapper.UserMapper;
import com.example.bank.service.UserService;
import com.example.bank.utils.bank.enums.BankType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void post() {
        long count = this.count();
        if (count > 0) {
            return;
        }
        User user = new User();
        user.setId(IdWorker.getId());
        user.setName("用户");
        user.setUsername("user1");
        user.setPassword(bCryptPasswordEncoder.encode("123456"));
        user.setRole(UserRole.USER);
        user.setCreateTime(LocalDateTime.now());
        user.setCreateBy(0L);
        this.save(user);
        User employees = new User();
        employees.setId(IdWorker.getId());
        employees.setName("员工");
        employees.setUsername("employees1");
        employees.setPassword(bCryptPasswordEncoder.encode("654321"));
        employees.setRole(UserRole.EMPLOYEES);
        employees.setBankType(BankType.JIANSHE);
        employees.setCreateTime(LocalDateTime.now());
        employees.setCreateBy(0L);
        this.save(employees);
    }

}

