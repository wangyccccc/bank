package com.example.bank.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bank.entity.BankDeposit;
import com.example.bank.entity.User;
import com.example.bank.group.QueryGroup;
import com.example.bank.model.dto.BankDepositDTO;
import com.example.bank.service.BankDepositService;
import com.example.bank.service.UserService;
import com.example.bank.utils.ReflectUtils;
import com.example.bank.utils.bank.enums.BankType;
import com.example.bank.utils.bank.enums.DepositType;
import com.example.bank.utils.jwt.JwtUtils;
import com.example.bank.utils.jwt.enums.AuthRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bank-deposit")
public class BankDepositController {

    private final UserService userService;
    private final BankDepositService bankDepositService;

    @PreAuthorize("hasRole('" + AuthRole.USER_ROLE + "')")
    @GetMapping("person/user")
    public Object personUser(@Validated({Default.class}) BankDepositDTO dto) {
        this.setTime(dto);
        BankDeposit bankDeposit = bankDepositService.getOne(new QueryWrapper<BankDeposit>().lambda()
                .eq(BankDeposit::getTime, dto.getTime())
                .eq(BankDeposit::getBankType, dto.getBankType())
                .eq(BankDeposit::getDepositType, DepositType.PERSON)
        );
        return this.getRate(dto, bankDeposit);
    }

    @PreAuthorize("hasRole('" + AuthRole.EMPLOYEES_ROLE + "')")
    @GetMapping("person/employees")
    public Object personEmployees(@Validated BankDepositDTO dto, HttpServletRequest request) {
        this.setBankType(dto, request);
        return this.personUser(dto);
    }

    @PreAuthorize("hasRole('" + AuthRole.USER_ROLE + "')")
    @GetMapping("unit/user")
    public Object unitUser(@Validated({QueryGroup.class, Default.class}) BankDepositDTO dto) {
        this.setTime(dto);
        BankDeposit bankDeposit = bankDepositService.getOne(new QueryWrapper<BankDeposit>().lambda()
                .eq(BankDeposit::getTime, dto.getTime())
                .eq(BankDeposit::getBankType, dto.getBankType())
                .eq(BankDeposit::getDepositType, DepositType.UNIT)
        );
        return this.getRate(dto, bankDeposit);
    }

    @PreAuthorize("hasRole('" + AuthRole.EMPLOYEES_ROLE + "')")
    @GetMapping("unit/employees")
    public Object unitEmployees(@Validated BankDepositDTO dto, HttpServletRequest request) {
        this.setBankType(dto, request);
        return this.unitUser(dto);
    }

    /**
     * 员工获取对应的银行信息进行拆训条件填充
     *
     * @param dto     查询信息
     * @param request request
     */
    public void setBankType(BankDepositDTO dto, HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        Long id = JwtUtils.getId(authorization);
        BankType bankType = Optional.ofNullable(userService.getOne(new QueryWrapper<User>().lambda()
                        .eq(User::getId, id))
                )
                .map(User::getBankType)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        dto.setBankType(bankType);
    }

    /**
     * 获取指定利率
     *
     * @param dto         查询信息
     * @param bankDeposit 结果信息
     * @return 获取指定利率
     */
    public Object getRate(BankDepositDTO dto, BankDeposit bankDeposit) {
        if (StringUtils.isBlank(dto.getField())) {
            // 如果不查询直接返回整个结果
            return bankDeposit;
        }
        // 过滤出只需要对应的利率信息
        Field field = Arrays.stream(BankDeposit.class.getDeclaredFields())
                .filter(f -> dto.getField().equals(Optional.ofNullable(f.getAnnotation(Schema.class)).map(Schema::name).orElse(null)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("查询字段不存在"));
        return Optional.ofNullable(bankDeposit).map(b -> ReflectUtils.getValue(b, field)).orElse(null);
    }

    /**
     * 将时间设置为最近的上一次利率发布时间
     *
     * @param dto 查询信息
     */
    public void setTime(BankDepositDTO dto) {
        List<LocalDate> times = bankDepositService.list(new QueryWrapper<BankDeposit>().lambda()
                        .groupBy(BankDeposit::getTime)
                        .select(BankDeposit::getTime)).stream()
                .map(BankDeposit::getTime).sorted((a, b) -> -a.compareTo(b))
                .toList();
        for (LocalDate time : times) {
            if (time.equals(dto.getTime()) || time.isBefore(dto.getTime())) {
                dto.setTime(time);
                return;
            }
        }
    }

}