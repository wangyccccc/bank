package com.example.bank.utils.jwt.vo;

import com.example.bank.utils.jwt.enums.AuthRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class AuthInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 8147159971329518558L;

    public AuthInfo(Long id, AuthRole role) {
        this.id = id;
        this.role = role;
    }

    /**
     * ID
     */
    private Long id;

    /**
     * 登录角色
     */
    private AuthRole role;

    /**
     * 公开用户
     */
    public static final AuthInfo PUBLIC = new AuthInfo(0L, AuthRole.PUBLIC);

}
