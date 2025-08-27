package server.domain.customers;

import java.math.BigDecimal;

/** Strategy for customer purchase discount. */
public interface CustomerType {
    String code(); // NEW | RETURNING | VIP

    /**
     * Returns the absolute discount value for a given base price.
     * e.g., if base = 100 and discount = 5%, return 5.00
     */
    BigDecimal applyDiscount(BigDecimal basePrice);
}
