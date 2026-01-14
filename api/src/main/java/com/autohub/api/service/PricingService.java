package com.autohub.api.service;

import com.autohub.api.model.Cart;
import com.autohub.api.model.CartItem;
import com.autohub.api.model.Coupon;
import com.autohub.api.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PricingService {

    private final CouponRepository couponRepository;

    public PricingService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public PricingResult calculatePricing(Cart cart) {

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            subtotal = subtotal.add(
                    item.getPart().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = null;

        if (cart.getAppliedCoupon() != null) {
            Coupon coupon = validateCoupon(cart.getAppliedCoupon(), subtotal);
            discount = calculateDiscount(coupon, subtotal);
            couponCode = coupon.getCode();
        }

        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        return new PricingResult(subtotal, discount, total, couponCode);
    }

    private Coupon validateCoupon(Coupon coupon, BigDecimal subtotal) {

        Coupon persisted = couponRepository.findByCodeIgnoreCase(coupon.getCode())
                .orElseThrow(() -> new RuntimeException("Invalid coupon code"));

        if (!persisted.isActive()) {
            throw new RuntimeException("Coupon is not active");
        }

        if (persisted.getExpiryDate() != null &&
                persisted.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Coupon has expired");
        }

        if (persisted.getMinSpend() != null &&
                subtotal.compareTo(persisted.getMinSpend()) < 0) {
            throw new RuntimeException("Minimum spend not met for coupon");
        }

        return persisted;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {

        BigDecimal discount;

        if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = subtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
        } else if ("FIXED".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = coupon.getDiscountValue();
        } else {
            throw new RuntimeException("Unsupported coupon discount type");
        }

        return discount.min(subtotal);
    }

    public static class PricingResult {
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal total;
        private final String couponCode;

        public PricingResult(BigDecimal subtotal,
                             BigDecimal discount,
                             BigDecimal total,
                             String couponCode) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.couponCode = couponCode;
        }

        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getTotal() { return total; }
        public String getCouponCode() { return couponCode; }
    }
}
