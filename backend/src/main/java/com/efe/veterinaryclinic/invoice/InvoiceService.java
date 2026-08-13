package com.efe.veterinaryclinic.invoice;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.invoice.dto.BulkMarkPaidRequest;
import com.efe.veterinaryclinic.invoice.dto.InvoiceItemRequest;
import com.efe.veterinaryclinic.invoice.dto.InvoiceRequest;
import com.efe.veterinaryclinic.invoice.dto.InvoiceResponse;
import com.efe.veterinaryclinic.invoice.dto.InvoiceStatsResponse;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.visit.Visit;
import com.efe.veterinaryclinic.visit.VisitRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.18");
    private static final int CURRENCY_SCALE = 2;

    private final InvoiceRepository invoiceRepository;
    private final VisitRepository visitRepository;
    private final PetRepository petRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, VisitRepository visitRepository,
                           PetRepository petRepository) {
        this.invoiceRepository = invoiceRepository;
        this.visitRepository = visitRepository;
        this.petRepository = petRepository;
    }

    public InvoiceResponse create(InvoiceRequest request) {
        Visit visit = findVisitOrThrow(request.visitId());

        List<InvoiceItem> items = request.items().stream()
                .map(this::toInvoiceItem)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(InvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vatAmount = subtotal.multiply(VAT_RATE).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vatAmount);

        Invoice invoice = new Invoice(visit, items, subtotal, VAT_RATE, vatAmount, total);
        Invoice saved = invoiceRepository.save(invoice);

        return InvoiceResponse.from(saved);
    }

    public InvoiceResponse getById(Long id) {
        return InvoiceResponse.from(findInvoiceOrThrow(id));
    }

    public PageResponse<InvoiceResponse> list(InvoiceStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Invoice> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and(InvoiceSpecifications.hasStatus(status));
        }
        if (from != null) {
            spec = spec.and(InvoiceSpecifications.issuedAtFrom(from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and(InvoiceSpecifications.issuedAtBefore(to.plusDays(1).atStartOfDay()));
        }

        return PageResponse.from(invoiceRepository.findAll(spec, pageable).map(InvoiceResponse::from));
    }

    public List<InvoiceResponse> getByOwnerId(Long ownerId) {
        return invoiceRepository.findByVisit_Pet_Owner_IdOrderByIssuedAtDesc(ownerId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    public PageResponse<InvoiceResponse> listByPet(Long petId, Pageable pageable) {
        findPetOrThrow(petId);

        return PageResponse.from(invoiceRepository.findByVisit_Pet_Id(petId, pageable).map(InvoiceResponse::from));
    }

    public InvoiceStatsResponse getStats() {
        long totalInvoices = invoiceRepository.count();
        long draft = invoiceRepository.countByStatus(InvoiceStatus.DRAFT);
        long sent = invoiceRepository.countByStatus(InvoiceStatus.SENT);
        long paid = invoiceRepository.countByStatus(InvoiceStatus.PAID);

        return new InvoiceStatsResponse(totalInvoices, draft, sent, paid);
    }

    public InvoiceResponse markSent(Long id) {
        Invoice invoice = findInvoiceOrThrow(id);
        invoice.updateStatus(InvoiceStatus.SENT);

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public InvoiceResponse markPaid(Long id) {
        Invoice invoice = findInvoiceOrThrow(id);
        invoice.updateStatus(InvoiceStatus.PAID);

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public List<InvoiceResponse> bulkMarkPaid(BulkMarkPaidRequest request) {
        List<Invoice> invoices = request.invoiceIds().stream()
                .map(this::findInvoiceOrThrow)
                .toList();

        invoices.forEach(invoice -> invoice.updateStatus(InvoiceStatus.PAID));

        return invoiceRepository.saveAll(invoices).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    private InvoiceItem toInvoiceItem(InvoiceItemRequest itemRequest) {
        BigDecimal lineTotal = itemRequest.unitPrice()
                .multiply(BigDecimal.valueOf(itemRequest.quantity()))
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        return new InvoiceItem(itemRequest.description(), itemRequest.category(), itemRequest.quantity(),
                itemRequest.unitPrice(), lineTotal);
    }

    private Invoice findInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id " + id));
    }

    private Visit findVisitOrThrow(Long visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found with id " + visitId));
    }

    private Pet findPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id " + petId));
    }
}
