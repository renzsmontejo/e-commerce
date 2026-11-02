package com.example.ecommerce.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAX = new BigDecimal("9999999999.99");
    private Money() {}
    public static BigDecimal checked(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Amount is outside the supported range");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal line(BigDecimal price, int quantity) {
        return checked(checked(price).multiply(BigDecimal.valueOf(quantity)));
    }
}
