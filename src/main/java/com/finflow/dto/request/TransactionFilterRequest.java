package com.finflow.dto.request;

import com.finflow.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class TransactionFilterRequest {

    private TransactionType transactionType; // optional

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate; // optional, format: yyyy-MM-dd

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;   // optional, format: yyyy-MM-dd
}