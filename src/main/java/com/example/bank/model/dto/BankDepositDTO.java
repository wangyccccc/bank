package com.example.bank.model.dto;

import com.example.bank.group.QueryGroup;
import com.example.bank.utils.bank.enums.BankType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BankDepositDTO {

    /**
     * 时间
     */
    @NotNull
    private LocalDate time;

    /**
     * 银行类型
     */
    @NotNull(groups = {QueryGroup.class})
    private BankType bankType;

    /**
     * 字段名称
     */
    private String field;

}
