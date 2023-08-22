package com.example.bank.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bank.entity.BankDeposit;
import com.example.bank.entity.User;
import com.example.bank.group.QueryGroup;
import com.example.bank.model.dto.BankDepositDTO;
import com.example.bank.model.vo.BankDepositVO;
import com.example.bank.service.BankDepositService;
import com.example.bank.service.UserService;
import com.example.bank.utils.ReflectUtils;
import com.example.bank.utils.bank.enums.BankType;
import com.example.bank.utils.bank.enums.DepositType;
import com.example.bank.utils.jwt.JwtUtils;
import com.example.bank.utils.jwt.enums.AuthRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "银行利率查询")
@RestController
@RequiredArgsConstructor
@RequestMapping("/bank-deposit")
public class BankDepositController {

    private final UserService userService;
    private final BankDepositService bankDepositService;

    /**
     * 将时间设置为最近的上一次利率发布时间
     *
     * @param dto 查询信息
     */
    private void setTime(BankDepositDTO dto) {
        if (dto.getTime() == null) {
            return;
        }
        List<LocalDate> times = bankDepositService.list(new QueryWrapper<BankDeposit>()
                        .select("time")
                        .lambda()
                        .eq(dto.getBankType() != null, BankDeposit::getBankType, dto.getBankType())
                        .groupBy(BankDeposit::getTime)).stream()
                .map(BankDeposit::getTime).sorted((a, b) -> -a.compareTo(b))
                .toList();
        for (LocalDate time : times) {
            if (time.equals(dto.getTime()) || time.isBefore(dto.getTime())) {
                dto.setTime(time);
                return;
            }
        }
    }

    /**
     * 员工获取对应的银行信息进行查询条件填充
     *
     * @param dto     查询信息
     * @param request request
     */
    private void setBankType(BankDepositDTO dto, HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        Long id = JwtUtils.getId(authorization);
        BankType bankType = Optional.ofNullable(userService.getOne(new QueryWrapper<User>().lambda()
                        .eq(User::getId, id))
                )
                .map(User::getBankType)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        dto.setBankType(bankType);
    }

    @Operation(summary = "查询个人员工银行利率")
    @PreAuthorize("hasRole('" + AuthRole.EMPLOYEES_ROLE + "')")
    @GetMapping("person/employees")
    public Object personEmployees(@Validated BankDepositDTO dto, HttpServletRequest request) {
        this.setBankType(dto, request);
        return this.personUser(dto);
    }

    @Operation(summary = "查询个人用户银行利率")
    @PreAuthorize("hasRole('" + AuthRole.USER_ROLE + "')")
    @GetMapping("person/user")
    public Object personUser(@Validated BankDepositDTO dto) {
        if (dto.getField() == null
                && dto.getBankType() == null
                && dto.getTime() == null
                && dto.getMoreThan() == null
                && dto.getLessThan() == null) {
            throw new RuntimeException("至少需要一个查询条件");
        }
        this.setTime(dto);
        List<BankDeposit> bankDeposits = bankDepositService.list(new QueryWrapper<BankDeposit>().lambda()
                .eq(dto.getTime() != null, BankDeposit::getTime, dto.getTime())
                .eq(dto.getBankType() != null, BankDeposit::getBankType, dto.getBankType())
                .eq(BankDeposit::getDepositType, DepositType.PERSON)
        );
        return this.getRate(dto, bankDeposits);
    }

    @Operation(summary = "查询单位员工银行利率")
    @PreAuthorize("hasRole('" + AuthRole.EMPLOYEES_ROLE + "')")
    @GetMapping("unit/employees")
    public Object unitEmployees(@Validated BankDepositDTO dto, HttpServletRequest request) {
        this.setBankType(dto, request);
        return this.unitUser(dto);
    }

    @Operation(summary = "查询单位用户银行利率")
    @PreAuthorize("hasRole('" + AuthRole.USER_ROLE + "')")
    @GetMapping("unit/user")
    public Object unitUser(@Validated BankDepositDTO dto) {
        if (dto.getField() == null
                && dto.getBankType() == null
                && dto.getTime() == null
                && dto.getMoreThan() == null
                && dto.getLessThan() == null) {
            throw new RuntimeException("至少需要一个查询条件");
        }
        this.setTime(dto);
        List<BankDeposit> bankDeposits = bankDepositService.list(new QueryWrapper<BankDeposit>().lambda()
                .eq(dto.getTime() != null, BankDeposit::getTime, dto.getTime())
                .eq(dto.getBankType() != null, BankDeposit::getBankType, dto.getBankType())
                .eq(BankDeposit::getDepositType, DepositType.UNIT)
        );
        return this.getRate(dto, bankDeposits);
    }

    /**
     * 获取指定利率
     *
     * @param dto          查询信息
     * @param bankDeposits 结果列表
     * @return 获取指定利率
     */
    private Object getRate(BankDepositDTO dto, List<BankDeposit> bankDeposits) {
        if (bankDeposits == null || bankDeposits.size() == 0) {
            // 如果数据库查出来没有数据，则没必要在往下走了，直接退出 return
            return null;
        }
        List<Field> fields = Arrays.stream(BankDeposit.class.getDeclaredFields())
                // 过滤出利率字段，利率字段，我在上面标记了 Schema 注解
                .filter(f -> f.getAnnotation(Schema.class) != null)
                .filter(f -> {
                    if (StringUtils.isBlank(dto.getField())) {
                        // 看下请求有没有传 field 参数，没有的话则默认返回所有字段
                        return true;
                    }
                    // 如果有的传的话，则匹配对应的字段并返回
                    return dto.getField().equals(Optional.ofNullable(f.getAnnotation(Schema.class)).map(Schema::name).orElse(null));
                })
                .toList();
        // resultMap 是最终的数据结构，往里面 put 一直填充数据，返回结构就是第一层银行，第二层时间，第三层数据
        // 这里是第一层
        Map<BankType, Map<LocalDate, Object>> resultMap = new HashMap<>();
        Map<BankType, List<BankDeposit>> bankMap = bankDeposits.stream()
                // 将数据库查出来的数据，按照 bankType 银行类型进行分组
                .collect(Collectors.groupingBy(BankDeposit::getBankType));
        for (Map.Entry<BankType, List<BankDeposit>> entry : bankMap.entrySet()) {
            // 分组数据进行循环处理
            List<BankDeposit> results = entry.getValue();
            Map<LocalDate, Object> timeMap = new HashMap<>();
            for (BankDeposit result : results) {
                for (Field field : fields) {
                    // 获取单个对象的字段对应的值
                    Object value = Optional.of(result).map(b -> ReflectUtils.getValue(b, field)).orElse(null);
                    if (dto.getMoreThan() != null) {
                         if (value == null || !NumberUtils.isCreatable(value.toString())) {
                             // 如果有传大于条件，并且值为空或者值不是数字，则通过反射赋值为 null，为 null 的话，jackson 就不会进行展示字段（jackson 我配置了默认不显示 null 数据），相辅相成
                             ReflectUtils.setValue(result, field, null);
                            continue;
                        }
                        if (new BigDecimal(value.toString()).compareTo(dto.getMoreThan()) <= 0) {
                            // 如果有传大于条件，并且条件不满足，则通过反射赋值为 null，为 null 的话，jackson 就不会进行展示字段（jackson 我配置了默认不显示 null 数据），相辅相成
                            ReflectUtils.setValue(result, field, null);
                            continue;
                        }
                    }
                    if (dto.getLessThan() != null) {
                        if (value == null || !NumberUtils.isCreatable(value.toString())) {
                            // 如果有传小于条件，并且值为空或者值不是数字，则通过反射赋值为 null，为 null 的话，jackson 就不会进行展示字段（jackson 我配置了默认不显示 null 数据），相辅相成
                            ReflectUtils.setValue(result, field, null);
                            continue;
                        }
                        if (new BigDecimal(value.toString()).compareTo(dto.getLessThan()) >= 0) {
                            // 如果有传小于条件，并且条件不满足，则通过反射赋值为 null，为 null 的话，jackson 就不会进行展示字段（jackson 我配置了默认不显示 null 数据），相辅相成
                            ReflectUtils.setValue(result, field, null);
                        }
                    }
                }
                // 将数据 put 到最后结构里面，key 为 时间，value 为对应数据，这是第三层结构
                timeMap.put(result.getTime(), result);
                // 将不展示的多余字段赋值为 null，不进行展示
                result.setId(null);
                result.setTime(null);
                result.setBankType(null);
                result.setDepositType(null);
                result.setCreateTime(null);
                result.setCreateBy(null);
            }
            // 将数据 put 到最后结构里面，key 为 银行类型，value 为时间数据map，这是第二层结构
            resultMap.put(entry.getKey(), timeMap);
        }
        // 返回整个结构
        return resultMap;
    }

}