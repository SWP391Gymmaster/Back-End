package com.swp391_be.repository;

import com.swp391_be.entity.Payment;
import com.swp391_be.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    List<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    List<Payment> findByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, PaymentStatus status);
}
