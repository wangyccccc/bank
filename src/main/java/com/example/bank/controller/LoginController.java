package com.example.bank.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bank.entity.User;
import com.example.bank.model.dto.LoginDTO;
import com.example.bank.model.vo.LoginVO;
import com.example.bank.service.UserService;
import com.example.bank.utils.jwt.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/login")
public class LoginController {

    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @PostMapping
    public LoginVO login(@Validated @RequestBody LoginDTO login) throws JsonProcessingException {
        User user = Optional.ofNullable(userService.getOne(new QueryWrapper<User>().lambda()
                        .eq(User::getUsername, login.getUsername())))
                .filter(u -> bCryptPasswordEncoder.matches(login.getPassword(), u.getPassword()))
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        LoginVO result = objectMapper.readValue(objectMapper.writeValueAsString(user), LoginVO.class);
        result.setAuthorization(JwtUtils.create(result.getId(), DateUtils.addHours(new Date(), 2), "wyc"));
        return result;
    }

}
