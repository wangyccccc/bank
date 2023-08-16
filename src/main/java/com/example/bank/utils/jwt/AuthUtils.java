package com.example.bank.utils.jwt;

import com.example.bank.entity.User;
import com.example.bank.mapper.UserMapper;
import com.example.bank.utils.jwt.enums.AuthRole;
import com.example.bank.utils.jwt.vo.AuthInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUtils {

    private final UserMapper userMapper;

    private final static String SECRET = "Wyc";

    /**
     * 获取用户信息
     *
     * @param authorization authorization
     * @return 获取用户信息
     */
    public AuthInfo getUser(String authorization) {
        try {
            AuthInfo authInfo = getAuthUser(authorization);
            return JwtUtils.verify(authorization, SECRET) ? AuthInfo.PUBLIC : authInfo;
        } catch (Exception e) {
            return AuthInfo.PUBLIC;
        }
    }

    /**
     * 获取通过鉴权的用户信息
     *
     * @param authorization authorization
     * @return 获取通过鉴权的用户信息
     */
    public AuthInfo getAuthUser(String authorization) {
        authorization = Optional.ofNullable(authorization).orElse("").replace(JwtUtils.AUTHORIZATION_PREFIX, "");
        Long id = JwtUtils.getId(authorization);
        AuthRole role = Optional.ofNullable(userMapper.selectById(id))
                .map(User::getRole).map(Objects::toString).map(AuthRole::valueOf)
                .orElse(AuthRole.PUBLIC);
        return new AuthInfo()
                .setId(id)
                .setRole(role);
    }

}