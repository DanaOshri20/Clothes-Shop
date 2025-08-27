package server.domain.customers;

import java.math.BigDecimal;

public class VipCustomer implements CustomerType {
    @Override public String code() { return "VIP"; }
    @Override public BigDecimal applyDiscount(BigDecimal basePrice) {
        // 12% discount
        return basePrice.multiply(new BigDecimal("0.12"));
    }
}
