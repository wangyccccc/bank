package com.example.bank.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class LoginDTO {

    /**
     * 用户名
     */
    @NotBlank
    @Length(max = 20)
    private String username;

    /**
     * 密码
     */
    @NotBlank
    @Length(max = 20)
    private String password;

}
