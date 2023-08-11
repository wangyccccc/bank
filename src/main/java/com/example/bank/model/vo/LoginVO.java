package com.example.bank.model.vo;

import com.example.bank.entity.enums.UserRole;
import lombok.Data;

@Data
public class LoginVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色
     */
    private UserRole role;

    /**
     * authorization
     */
    private String authorization;

}