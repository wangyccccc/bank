package com.example.bank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.bank.utils.bank.enums.BankType;
import com.example.bank.utils.bank.enums.DepositType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName(value = "bank_deposit", autoResultMap = true)
public class BankDeposit {

    /**
     * 银行存款ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 活期
     */
    @Schema(name = "活期")
    private BigDecimal huoqi;

    /**
     * 整存整取三个月
     */
    @Schema(name = "整存整取三个月")
    private BigDecimal zczqThreeMonths;

    /**
     * 整存整取半年
     */
    @Schema(name = "整存整取半年")
    private BigDecimal zczqHalfYear;

    /**
     * 整存整取一年
     */
    @Schema(name = "整存整取一年")
    private BigDecimal zczqOneYear;

    /**
     * 整存整取二年
     */
    @Schema(name = "整存整取二年")
    private BigDecimal zczqTwoYears;

    /**
     * 整存整取三年
     */
    @Schema(name = "整存整取三年")
    private BigDecimal zczqThreeYears;

    /**
     * 整存整取五年
     */
    @Schema(name = "整存整取五年")
    private BigDecimal zczqFiveYears;

    /**
     * 零存整取一年
     */
    @Schema(name = "零存整取一年")
    private BigDecimal lczqOneYear;

    /**
     * 零存整取三年
     */
    @Schema(name = "零存整取三年")
    private BigDecimal lczqThreeYears;

    /**
     * 零存整取五年
     */
    @Schema(name = "零存整取五年")
    private BigDecimal lczqFiveYears;

    /**
     * 定活两便
     */
    @Schema(name = "定活两便")
    private String dhlb;

    /**
     * 协定存款
     */
    @Schema(name = "协定存款")
    private BigDecimal xdck;

    /**
     * 通知存款一天
     */
    @Schema(name = "通知存款一天")
    private BigDecimal tzOneDay;

    /**
     * 通知存款七天
     */
    @Schema(name = "通知存款七天")
    private BigDecimal tzSevenDay;

    /**
     * 时间
     */
    private LocalDate time;

    /**
     * 银行类型
     */
    private BankType bankType;

    /**
     * 存款类型
     */
    private DepositType depositType;

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
