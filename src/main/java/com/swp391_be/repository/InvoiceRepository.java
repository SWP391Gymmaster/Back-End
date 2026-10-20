package com.swp391_be.repository;

import com.swp391_be.entity.Invoice;
import com.swp391_be.entity.InvoiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByMemberId(Long memberId);

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    List<Invoice> findByMemberIdOrderByIssuedAtDesc(Long memberId);

    List<Invoice> findAllByOrderByIssuedAtDesc();

    List<Invoice> findByStatusOrderByIssuedAtDesc(InvoiceStatus status);

    List<Invoice> findByMemberIdAndStatusOrderByIssuedAtDesc(Long memberId, InvoiceStatus status);
}
