package com.example.bank.utils.bank.enums;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.bank.entity.BankDeposit;
import com.example.bank.utils.ReflectUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum BankType {

    /**
     * 工商银行
     */
    GONGSHANG() {
        @Override
        public List<BankDeposit> parserPerson() throws Exception {
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
            List<BankDeposit> results = new ArrayList<>(times.size());
            for (LocalDate time : times) {
                client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .build();
                baseBuilder = HttpRequest.newBuilder()
                        .uri(new URI("https://papi.icbc.com.cn/interestRate/deposit/queryRMBDepositInfo?type=CH&date=" + time.toString()))
                        .timeout(Duration.ofSeconds(10))
                        .version(HttpClient.Version.HTTP_2);
                request = baseBuilder
                        .GET()
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONObject data = JSON.parseObject(response.body())
                        .getJSONObject("data");
                BankDeposit bankDeposit = new BankDeposit();
                bankDeposit.setHuoqi(data.getBigDecimal("huoQi"));
                bankDeposit.setZczqThreeMonths(data.getBigDecimal("zczqThreeMonths"));
                bankDeposit.setZczqHalfYear(data.getBigDecimal("zczqHalfYear"));
                bankDeposit.setZczqOneYear(data.getBigDecimal("zczqOneYear"));
                bankDeposit.setZczqTwoYears(data.getBigDecimal("zczqTwoYears"));
                bankDeposit.setZczqThreeYears(data.getBigDecimal("zczqThreeYears"));
                bankDeposit.setZczqFiveYears(data.getBigDecimal("zczqFiveYears"));
                bankDeposit.setLczqOneYear(data.getBigDecimal("lczqOneYear"));
                bankDeposit.setLczqThreeYears(data.getBigDecimal("lczqThreeYears"));
                bankDeposit.setLczqFiveYears(data.getBigDecimal("lczqFiveYears"));
                bankDeposit.setDhlb(data.getString("dhlb"));
                bankDeposit.setXdck(data.getBigDecimal("xdck"));
                bankDeposit.setTzOneDay(data.getBigDecimal("tzOneDay"));
                bankDeposit.setTzSevenDay(data.getBigDecimal("tzSevenDay"));
                bankDeposit.setTime(time);
                bankDeposit.setBankType(GONGSHANG);
                bankDeposit.setDepositType(DepositType.PERSON);
                results.add(bankDeposit);
            }
            return results.stream().sorted((a, b) -> -a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        }

        @Override
        public List<BankDeposit> parserUnit() {
            return Collections.emptyList();
        }
    },

    /**
     * 中国银行
     */
    ZHONGGUO() {
        @Override
        public List<BankDeposit> parserPerson() throws Exception {
            List<BankDeposit> results = new ArrayList<>();
            Document document = Jsoup.parse(new URL("https://www.bankofchina.com/fimarkets/lilv/fd31/index.html"), 5000);
            Element span = document.getElementsByClass("turn_page").get(0).select("span").get(0);
            int number = Integer.parseInt(span.text());
            for (int i = 1; i <= number; i++) {
                String page = i == 1 ? "" : "_" + (i - 1);
                document = Jsoup.parse(new URL("https://www.bankofchina.com/fimarkets/lilv/fd31/index" + page + ".html"), 5000);
                Elements as = Objects.requireNonNull(document.getElementsByClass("list").last()).select("a");
                for (Element a : as) {
                    BankDeposit bankDeposit = new BankDeposit();
                    LocalDate time = LocalDate.parse(a.text().replace("人民币存款利率表", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    document = Jsoup.parse(new URL("https://www.bankofchina.com/fimarkets/lilv/fd31" + a.attr("href").replace("./", "/")), 5000);
                    Elements trs = document.getElementsByClass("sub_con").get(0).select("tr");
                    switch (time.toString()) {
                        case "1998-07-01", "1998-03-25", "1997-10-23", "1996-08-23", "1996-05-01" -> {
                            for (int j = 0; j < trs.size(); j++) {
                                Element element = trs.get(j);
                                switch (j) {
                                    case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                    case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                    case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                    case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                    case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                    default -> {
                                    }
                                }
                            }
                        }
                        case "2002-02-21", "1999-06-10" -> {
                            for (int j = 0; j < trs.size(); j++) {
                                Element element = trs.get(j);
                                switch (j) {
                                    case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                    case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                    case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                    case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                    case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                    case 20 -> bankDeposit.setXdck(new BigDecimal(element.child(1).text()));
                                    case 22 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                    case 23 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                    default -> {
                                    }
                                }
                            }
                        }
                        case "2007-05-19", "2006-08-19" -> {
                            for (int j = 0; j < trs.size(); j++) {
                                Element element = trs.get(j);
                                switch (j) {
                                    case 3 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                    case 4 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                    case 5 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 6 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                    case 7 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 8 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 10 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 11 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 12 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 13 -> bankDeposit.setDhlb(element.child(1).text());
                                    case 14 -> bankDeposit.setXdck(new BigDecimal(element.child(1).text()));
                                    case 16 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                    case 17 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                    default -> {
                                    }
                                }
                            }
                        }
                        default -> {
                            for (int j = 0; j < trs.size(); j++) {
                                Element element = trs.get(j);
                                switch (j) {
                                    case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                    case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                    case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                    case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                    case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                    case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                    case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                    case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                    case 16 -> bankDeposit.setXdck(new BigDecimal(element.child(1).text()));
                                    case 18 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                    case 19 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                    default -> {
                                    }
                                }
                            }
                        }
                    }
                    bankDeposit.setTime(time);
                    bankDeposit.setBankType(ZHONGGUO);
                    bankDeposit.setDepositType(DepositType.PERSON);
                    results.add(bankDeposit);
                }
            }
            return results.stream().sorted((a, b) -> -a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        }

        @Override
        public List<BankDeposit> parserUnit() {
            return Collections.emptyList();
        }
    },

    /**
     * 建设银行
     */
    JIANSHE() {
        @Override
        public List<BankDeposit> parserPerson() throws Exception {
            List<BankDeposit> results = new ArrayList<>();
            Document document = Jsoup.parse(new URL("http://www2.ccb.com/chn/personal/interestv3/rmbdeposit.shtml"), 5000);
            Elements lis = document.getElementsByClass("list_ul").get(0).select("li");
            for (Element li : lis) {
                BankDeposit bankDeposit = new BankDeposit();
                LocalDate time = LocalDate.parse(li.text(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                document = Jsoup.parse(new URL("http://www2.ccb.com" + li.attr("name")), 5000);
                Elements trs = Objects.requireNonNull(document.getElementById("ti")).select("tr");
                switch (time.toString()) {
                    case "2023-06-08" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 1 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 3 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 4 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                case 11 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 12 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 13 -> bankDeposit.setDhlb(element.child(1).text());
                                case 14 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(2).text()));
                                case 15 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                    case "2007-09-15" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                case 18 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                case 19 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                    case "2007-03-18" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                case 20 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                case 21 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                    case "1999-06-10", "1998-07-01", "1998-03-25", "1997-10-23", "1996-08-23", "1996-05-01" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                case 21 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                case 22 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                    case "1955-10-01" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setDhlb(element.child(1).text());
                                default -> {
                                }
                            }
                        }
                    }
                    default -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 12 -> bankDeposit.setLczqOneYear(new BigDecimal(element.child(1).text()));
                                case 13 -> bankDeposit.setLczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 14 -> bankDeposit.setLczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 15 -> bankDeposit.setDhlb(element.child(1).text());
                                case 17 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                case 18 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                }
                bankDeposit.setTime(time);
                bankDeposit.setBankType(JIANSHE);
                bankDeposit.setDepositType(DepositType.PERSON);
                results.add(bankDeposit);
            }
            return results.stream().sorted((a, b) -> -a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        }

        @Override
        public List<BankDeposit> parserUnit() throws Exception {
            List<BankDeposit> results = new ArrayList<>();
            Document document = Jsoup.parse(new URL("http://www2.ccb.com/chn/personal/interestv3/rmbdeposit_dw.shtml"), 5000);
            Elements lis = document.getElementsByClass("list_ul").get(0).select("li");
            for (Element li : lis) {
                BankDeposit bankDeposit = new BankDeposit();
                LocalDate time = LocalDate.parse(li.text(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                document = Jsoup.parse(new URL("http://www2.ccb.com" + li.attr("name")), 5000);
                Elements trs = Objects.requireNonNull(document.getElementById("ti")).select("tr");
                switch (time.toString()) {
                    case "2023-06-08" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 1 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 3 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 4 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setXdck(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(2).text()));
                                case 11 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                    case "1998-07-01", "1998-03-25", "1997-10-23", "1996-08-23", "1996-05-01" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                    case "1955-10-01" -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            if (i == 2) {
                                bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                            }
                        }
                    }
                    default -> {
                        for (int i = 0; i < trs.size(); i++) {
                            Element element = trs.get(i);
                            switch (i) {
                                case 2 -> bankDeposit.setHuoqi(new BigDecimal(element.child(1).text()));
                                case 5 -> bankDeposit.setZczqThreeMonths(new BigDecimal(element.child(1).text()));
                                case 6 -> bankDeposit.setZczqHalfYear(new BigDecimal(element.child(1).text()));
                                case 7 -> bankDeposit.setZczqOneYear(new BigDecimal(element.child(1).text()));
                                case 8 -> bankDeposit.setZczqTwoYears(new BigDecimal(element.child(1).text()));
                                case 9 -> bankDeposit.setZczqThreeYears(new BigDecimal(element.child(1).text()));
                                case 10 -> bankDeposit.setZczqFiveYears(new BigDecimal(element.child(1).text()));
                                case 11 -> bankDeposit.setXdck(new BigDecimal(element.child(1).text()));
                                case 13 -> bankDeposit.setTzOneDay(new BigDecimal(element.child(1).text()));
                                case 14 -> bankDeposit.setTzSevenDay(new BigDecimal(element.child(1).text()));
                                default -> {
                                }
                            }
                        }
                    }
                }
                bankDeposit.setTime(time);
                bankDeposit.setBankType(JIANSHE);
                bankDeposit.setDepositType(DepositType.UNIT);
                results.add(bankDeposit);
            }
            return results.stream().sorted((a, b) -> -a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        }
    },

    /**
     * 交通银行
     */
    JIAOTONG() {
        @Override
        public List<BankDeposit> parserPerson() throws Exception {
            List<BankDeposit> results = new ArrayList<>();
            Document document = Jsoup.parse(new URL("http://www.bankcomm.com/BankCommSite/getRMBDepositRateDate.do"), 5000);
            Elements trs = document.getElementsByClass("rateTab").get(0).select("tr");
            Elements rateDates = Objects.requireNonNull(document.getElementById("rateDate")).select("option");
            for (Element rateDate : rateDates) {
                BankDeposit bankDeposit = new BankDeposit();
                LocalDate time = LocalDate.parse(rateDate.attr("value"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                for (int i = 0; i < trs.size(); i++) {
                    Element element = trs.get(i);
                    switch (i) {
                        case 4 ->
                                bankDeposit.setHuoqi(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 7 ->
                                bankDeposit.setZczqThreeMonths(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 8 ->
                                bankDeposit.setZczqHalfYear(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 9 ->
                                bankDeposit.setZczqOneYear(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 10 ->
                                bankDeposit.setZczqTwoYears(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 11 ->
                                bankDeposit.setZczqThreeYears(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 12 ->
                                bankDeposit.setZczqFiveYears(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 14 ->
                                bankDeposit.setLczqOneYear(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 15 ->
                                bankDeposit.setLczqThreeYears(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 16 ->
                                bankDeposit.setLczqFiveYears(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 17 ->
                                bankDeposit.setDhlb(element.select("td").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).findFirst().orElse(null));
                        case 18 ->
                                bankDeposit.setXdck(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 20 ->
                                bankDeposit.setTzOneDay(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        case 21 ->
                                bankDeposit.setTzSevenDay(element.select("span").stream().filter(s -> s.attr("rel").equals(time.toString())).map(Element::text).map(BigDecimal::new).findFirst().orElse(null));
                        default -> {
                        }
                    }
                }
                bankDeposit.setTime(time);
                bankDeposit.setBankType(JIAOTONG);
                bankDeposit.setDepositType(DepositType.PERSON);
                results.add(bankDeposit);
            }
            // 如果没有数据，则自动获取上一次的数据
            for (int i = 0; i < results.size(); i++) {
                BankDeposit bankDeposit = results.get(i);
                final int skipNum = i + 1;
                List<Field> fields = Arrays.stream(BankDeposit.class.getDeclaredFields())
                        .filter(f -> f.getAnnotation(Schema.class) != null)
                        .toList();
                for (Field field : fields) {
                    Object value = ReflectUtils.getValue(bankDeposit, field);
                    if (value != null) {
                        continue;
                    }
                    BigDecimal val = results.stream()
                            .skip(skipNum)
                            .map(b -> ReflectUtils.getValue(b, field))
                            .filter(Objects::nonNull)
                            .findFirst().map(Objects::toString).map(BigDecimal::new).orElse(null);
                    ReflectUtils.setValue(bankDeposit, field, val);
                }
            }
            return results.stream().sorted((a, b) -> -a.getTime().compareTo(b.getTime())).collect(Collectors.toList());
        }

        @Override
        public List<BankDeposit> parserUnit() {
            return Collections.emptyList();
        }
    },

    /**
     * 农业银行
     */
    NONGYE() {
        @Override
        public List<BankDeposit> parserPerson() throws Exception {
            LocalDate date = LocalDate.of(2023, 6, 8);
            Document document = Jsoup.parse(new URL("https://www.abchina.com/cn/PersonalServices/Quotation/bwbll/201511/t20151126_807920.htm"), 5000);
            Elements trs = document.getElementsByClass("DataList").get(0).select("tr");
            BankDeposit bankDeposit = new BankDeposit();
            for (int i = 0; i < trs.size(); i++) {
                Element element = trs.get(i);
                switch (i) {
                    case 2 ->
                            bankDeposit.setHuoqi(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 5 ->
                            bankDeposit.setZczqThreeMonths(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 6 ->
                            bankDeposit.setZczqHalfYear(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 7 ->
                            bankDeposit.setZczqOneYear(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 8 ->
                            bankDeposit.setZczqTwoYears(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 9 ->
                            bankDeposit.setZczqThreeYears(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 10 ->
                            bankDeposit.setZczqFiveYears(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 12 ->
                            bankDeposit.setLczqOneYear(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 13 ->
                            bankDeposit.setLczqThreeYears(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 14 ->
                            bankDeposit.setLczqFiveYears(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 15 -> bankDeposit.setDhlb(Objects.requireNonNull(element.select("td").last()).text());
                    case 16 ->
                            bankDeposit.setXdck(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 18 ->
                            bankDeposit.setTzOneDay(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    case 19 ->
                            bankDeposit.setTzSevenDay(new BigDecimal(Objects.requireNonNull(element.select("td").last()).text()));
                    default -> {
                    }
                }
            }
            bankDeposit.setTime(date);
            bankDeposit.setBankType(NONGYE);
            bankDeposit.setDepositType(DepositType.PERSON);
            return List.of(bankDeposit);
        }

        @Override
        public List<BankDeposit> parserUnit() {
            return Collections.emptyList();
        }
    },

    ;

    /**
     * 解析城乡居民存款挂牌利率表
     *
     * @return 解析城乡居民存款挂牌利率表
     */
    public abstract List<BankDeposit> parserPerson() throws Exception;

    /**
     * 解析单位存款挂牌利率表
     *
     * @return 解析单位存款挂牌利率表
     */
    public abstract List<BankDeposit> parserUnit() throws Exception;

}
