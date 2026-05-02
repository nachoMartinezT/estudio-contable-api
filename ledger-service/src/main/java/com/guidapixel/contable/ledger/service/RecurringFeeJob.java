package com.guidapixel.contable.ledger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringFeeJob {

    private final RecurringFeeService recurringFeeService;

    @Scheduled(cron = "0 0 1 1 * *", zone = "America/Argentina/Buenos_Aires")
    public void generateMonthlyFees() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Iniciando job de generacion de honorarios recurrentes para {}", yearMonth);

        int generated = recurringFeeService.generateMonthlyFees(yearMonth);
        log.info("Job completado. {} honorarios generados para {}", generated, yearMonth);
    }
}
