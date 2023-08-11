package com.example.bank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bank.entity.enums.UserRole;
import com.example.bank.utils.bank.enums.BankType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "user", autoResultMap = true)
public class User implements Serializable {

    /**
     * 用户ID
     */
    @TableId(type = IdType.INPUT)
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
     * 密码
     */
    private String password;

    /**
     * 角色
     */
    private UserRole role;

    /**
     * 银行类型
     */
    private BankType bankType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 修改者
     */
    private Long updateBy;

}
