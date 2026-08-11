package com.efe.veterinaryclinic.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    long countByStatusNot(InvoiceStatus status);

    long countByStatus(InvoiceStatus status);

    List<Invoice> findByIssuedAtGreaterThanEqual(LocalDateTime from);

    List<Invoice> findByStatusAndVisit_Vet_IdAndIssuedAtGreaterThanEqual(
            InvoiceStatus status, Long vetId, LocalDateTime from);

    List<Invoice> findByVisit_Pet_Owner_IdOrderByIssuedAtDesc(Long ownerId);
}
