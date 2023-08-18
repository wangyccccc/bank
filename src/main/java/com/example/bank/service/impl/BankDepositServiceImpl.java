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

    @Scheduled(cron = "0 * * ? * *")
    @Transactional(rollbackFor = Exception.class)
    public void sync() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest.Builder baseBuilder = HttpRequest.newBuilder()
                .uri(new URI("https://papi.icbc.com.cn/interestRate/deposit/queryRMBDepositDateList?type=CH"))
                .timeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2);
        HttpRequest request = baseBuilder
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = JSON.parseObject(response.body())
                .getJSONArray("data").toJSONString();
        List<LocalDate> times = JSON.parseArray(json, LocalDate.class);
        LocalDate time = times.get(0);
        // 获取最新利率发布时间
        for (BankType bankType : BankType.values()) {
            long count = this.count(new QueryWrapper<BankDeposit>().lambda()
                    .eq(BankDeposit::getBankType, bankType)
                    .eq(BankDeposit::getTime, time));
            if (count > 0) {
                continue;
            }
            List<BankDeposit> results = new ArrayList<>();
            try {
                results.addAll(bankType.parserPerson());
                results.addAll(bankType.parserUnit());
            } catch (Exception e) {
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
            results = results.stream()
                    .filter(Objects::nonNull)
                    .filter(r -> r.getTime().equals(time))
                    .peek(b -> {
                        b.setId(IdWorker.getId());
                        b.setCreateTime(LocalDateTime.now());
                        b.setCreateBy(0L);
                    })
                    .toList();
            if (results.size() > 0) {
                this.saveBatch(results);
            }
        }
    }

}
