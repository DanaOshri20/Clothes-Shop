package server.domain.sales;

import server.domain.customers.Customer;
import server.domain.invantory.Product;

import java.math.BigDecimal;

/**
 * Handles sales logic: calculates total, discount, etc.
 */
public class SalesService {

    /** Summary object for a single sale */
    public static class SaleSummary {
        private final BigDecimal basePrice;
        private final BigDecimal discountValue;
        private final BigDecimal finalPrice;
        private final String customerType;

        public SaleSummary(BigDecimal basePrice, BigDecimal discountValue, BigDecimal finalPrice, String customerType) {
            this.basePrice = basePrice;
            this.discountValue = discountValue;
            this.finalPrice = finalPrice;
            this.customerType = customerType;
        }

        public BigDecimal basePrice() { return basePrice; }
        public BigDecimal discountValue() { return discountValue; }
        public BigDecimal finalPrice() { return finalPrice; }
        public String customerType() { return customerType; }
    }

    /**
     * Execute a sale and return summary.
     */
    public SaleSummary sell(Product product, int quantity, Customer customer) {
        BigDecimal basePrice = product.price().multiply(BigDecimal.valueOf(quantity));

        // let the customer type calculate discount
        BigDecimal discountValue = customer.type().applyDiscount(basePrice);
        BigDecimal finalPrice = basePrice.subtract(discountValue);

        return new SaleSummary(basePrice, discountValue, finalPrice, customer.type().code());
    }
}
