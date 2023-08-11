package com.example.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bank.entity.BankDeposit;
import com.example.bank.mapper.BankDepositMapper;
import com.example.bank.service.BankDepositService;
import com.example.bank.utils.bank.enums.BankType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BankDepositServiceImpl extends ServiceImpl<BankDepositMapper, BankDeposit> implements BankDepositService {

    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void init() {
        long count = this.count(new QueryWrapper<BankDeposit>().lambda());
        if (count > 0) {
            return;
        }
        this.sync();
    }

    @Scheduled(cron = "0 0 0 ? * *")
    @Transactional(rollbackFor = Exception.class)
    public void sync() {
        this.remove(new QueryWrapper<BankDeposit>().lambda()
                .isNotNull(BankDeposit::getId));
        List<BankDeposit> bankDeposits = Arrays.stream(BankType.values())
                .map(b -> {
                    try {
                        List<BankDeposit> results = new ArrayList<>(b.parserPerson());
                        results.add(b.parserUnit());
                        return results;
                    } catch (Exception e) {
                        throw new RuntimeException(e.getLocalizedMessage(), e);
                    }
                }).flatMap(Collection::stream)
                .filter(Objects::nonNull).peek(b -> {
                    b.setId(IdWorker.getId());
                    b.setCreateTime(LocalDateTime.now());
                    b.setCreateBy(0L);
                })
                .toList();
        this.saveBatch(bankDeposits);
    }

}

