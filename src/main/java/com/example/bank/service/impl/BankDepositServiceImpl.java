package com.example.bank.service.impl;

import com.alibaba.fastjson2.JSON;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
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
            // 如果有数据则直接跳过，后续只需要爬取最新数据
            return;
        }
        List<BankDeposit> bankDeposits = Arrays.stream(BankType.values())
                .map(b -> {
                    try {
                        List<BankDeposit> results = new ArrayList<>();
                        results.addAll(b.parserPerson());
                        results.addAll(b.parserUnit());
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
        if (bankDeposits.size() > 0) {
            this.saveBatch(bankDeposits);
        }
    }
//每24小时维护一遍
    @Scheduled(cron = "0 0 0 ? * *")
    @Transactional(rollbackFor = Exception.class)
    public void sync() throws Exception {
        List<BankDeposit> results = new ArrayList<>();
        for (BankType bankType : BankType.values()) {
            save(results, bankType, bankType.parserPerson());
            save(results, bankType, bankType.parserUnit());
            if (results.size() > 0) {
                this.saveBatch(results);
            }
        }
    }

    private void save(List<BankDeposit> results, BankType bankType, List<BankDeposit> bankDeposits) {
        if (bankDeposits.size() == 0) {
            return;
        }
        // 取出最新数据
        BankDeposit bankDeposit = bankDeposits.get(0);
        long count = this.count(new QueryWrapper<BankDeposit>().lambda()
                .eq(BankDeposit::getBankType, bankType)
                .eq(BankDeposit::getTime, bankDeposit.getTime())
                .eq(BankDeposit::getDepositType, bankDeposit.getDepositType())
        );
        if (count > 0) {
            // 有数据则忽略
            return;
        }
        results.add(bankDeposit);
    }

}