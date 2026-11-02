package com.example.ecommerce.order;
import com.example.ecommerce.order.service.CheckoutPricing;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class CheckoutPricingTests {
    @Test void calculatesMoneyWithTwoDecimalRounding() {
        var totals=new CheckoutPricing(new BigDecimal("50.00"),new BigDecimal("0.12")).calculate(new BigDecimal("123.45"));
        assertEquals(new BigDecimal("14.81"),totals.taxAmount()); assertEquals(new BigDecimal("188.26"),totals.totalAmount());
        assertEquals(new BigDecimal("0.00"),totals.discountAmount()); assertEquals("PHP",totals.currency());
    }
    @Test void rejectsInvalidConfigurationAndOverflow() {
        assertThrows(IllegalArgumentException.class,()->new CheckoutPricing(BigDecimal.ONE,new BigDecimal("1.01")));
        assertThrows(IllegalArgumentException.class,()->new CheckoutPricing(new BigDecimal("-1"),BigDecimal.ZERO));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new CheckoutPricing(BigDecimal.ONE,BigDecimal.ZERO).calculate(new BigDecimal("9999999999.99")));
    }
}
