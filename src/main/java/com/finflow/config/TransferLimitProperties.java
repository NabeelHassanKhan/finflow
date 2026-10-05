package com.finflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "finflow.transfer")
@Getter
@Setter
public class TransferLimitProperties {

    private BigDecimal dailyLimit;
}