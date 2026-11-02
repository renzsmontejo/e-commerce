package com.example.ecommerce.order.service;

import com.example.ecommerce.order.dto.CheckoutDtos.Totals;
import com.example.ecommerce.common.Money;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CheckoutPricing {
    private final BigDecimal shippingFee;
    private final BigDecimal taxRate;
    public CheckoutPricing(@Value("${app.checkout.shipping-fee:0.00}") BigDecimal shippingFee,
                           @Value("${app.checkout.tax-rate:0}") BigDecimal taxRate) {
        if(shippingFee.signum()<0 || taxRate.signum()<0 || taxRate.compareTo(BigDecimal.ONE)>0)
            throw new IllegalArgumentException("Invalid checkout shipping fee or tax rate");
        this.shippingFee=Money.checked(shippingFee); this.taxRate=taxRate;
    }
    public Totals calculate(BigDecimal subtotal) {
        subtotal=Money.checked(subtotal);
        var tax=Money.checked(subtotal.multiply(taxRate));
        return new Totals(subtotal,Money.ZERO,shippingFee,tax,Money.checked(subtotal.add(shippingFee).add(tax)),"PHP");
    }
}
