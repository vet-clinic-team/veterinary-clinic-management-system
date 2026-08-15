package com.efe.veterinaryclinic.config;

import com.efe.veterinaryclinic.invoice.InvoiceItemCategory;
import com.efe.veterinaryclinic.invoice.InvoiceService;
import com.efe.veterinaryclinic.invoice.dto.InvoiceItemRequest;
import com.efe.veterinaryclinic.invoice.dto.InvoiceRequest;
import com.efe.veterinaryclinic.invoice.dto.InvoiceResponse;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.owner.OwnerService;
import com.efe.veterinaryclinic.owner.dto.OwnerRequest;
import com.efe.veterinaryclinic.owner.dto.OwnerResponse;
import com.efe.veterinaryclinic.pet.PetService;
import com.efe.veterinaryclinic.pet.PetWeightRecordService;
import com.efe.veterinaryclinic.pet.dto.PetRequest;
import com.efe.veterinaryclinic.pet.dto.PetResponse;
import com.efe.veterinaryclinic.pet.dto.PetWeightRecordRequest;
import com.efe.veterinaryclinic.security.Role;
import com.efe.veterinaryclinic.vaccination.VaccinationService;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationRequest;
import com.efe.veterinaryclinic.vet.VetService;
import com.efe.veterinaryclinic.vet.dto.VetRequest;
import com.efe.veterinaryclinic.vet.dto.VetResponse;
import com.efe.veterinaryclinic.visit.VisitService;
import com.efe.veterinaryclinic.visit.VisitStatus;
import com.efe.veterinaryclinic.visit.dto.MedicalNotesUpdateRequest;
import com.efe.veterinaryclinic.visit.dto.VisitRequest;
import com.efe.veterinaryclinic.visit.dto.VisitResponse;
import com.efe.veterinaryclinic.visit.dto.VisitStatusUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds a realistic demo dataset covering the full acceptance flow (docs/backend-spec.md §15):
 * owners, pets, vets, visits in various states, vaccinations, weight history, and invoices.
 * Runs in its own transaction (unlike {@link DataSeeder}) because visit/response mapping
 * lazily loads Pet, which needs an open Hibernate session outside the request-scoped one.
 * Skipped entirely if owners already exist, so restarts never duplicate data.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean demoDataEnabled;
    private final OwnerRepository ownerRepository;
    private final OwnerService ownerService;
    private final PetService petService;
    private final VetService vetService;
    private final VisitService visitService;
    private final VaccinationService vaccinationService;
    private final PetWeightRecordService petWeightRecordService;
    private final InvoiceService invoiceService;

    public DemoDataSeeder(@Value("${app.seed.demo-data.enabled:true}") boolean demoDataEnabled,
                           OwnerRepository ownerRepository,
                           OwnerService ownerService,
                           PetService petService,
                           VetService vetService,
                           VisitService visitService,
                           VaccinationService vaccinationService,
                           PetWeightRecordService petWeightRecordService,
                           InvoiceService invoiceService) {
        this.demoDataEnabled = demoDataEnabled;
        this.ownerRepository = ownerRepository;
        this.ownerService = ownerService;
        this.petService = petService;
        this.vetService = vetService;
        this.visitService = visitService;
        this.vaccinationService = vaccinationService;
        this.petWeightRecordService = petWeightRecordService;
        this.invoiceService = invoiceService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoDataEnabled || ownerRepository.count() > 0) {
            log.info("Demo data seeding skipped (enabled={}, existingOwners={})", demoDataEnabled, ownerRepository.count());
            return;
        }

        log.info("Demo data seeding started");

        LocalDateTime now = LocalDateTime.now();

        VetResponse vetAhmet = vetService.create(
                new VetRequest("Ahmet Yildiz", "Internal Medicine", "VET-1001", "Mon-Fri 09:00-17:00"));
        VetResponse vetElif = vetService.create(
                new VetRequest("Elif Sahin", "Surgery", "VET-1002", "Mon-Fri 10:00-18:00"));

        OwnerResponse mehmet = ownerService.create(
                new OwnerRequest("Mehmet", "Demir", "+90 555 111 2233", "mehmet.demir@example.com", "Istanbul, Turkey"));
        OwnerResponse ayse = ownerService.create(
                new OwnerRequest("Ayse", "Yilmaz", "+90 555 222 3344", "ayse.yilmaz@example.com", "Ankara, Turkey"));
        OwnerResponse can = ownerService.create(
                new OwnerRequest("Can", "Ozturk", "+90 555 333 4455", "can.ozturk@example.com", "Izmir, Turkey"));
        OwnerResponse zeynep = ownerService.create(
                new OwnerRequest("Zeynep", "Kaya", "+90 555 444 5566", "zeynep.kaya@example.com", "Bursa, Turkey"));
        OwnerResponse emre = ownerService.create(
                new OwnerRequest("Emre", "Celik", "+90 555 555 6677", "emre.celik@example.com", "Antalya, Turkey"));

        PetResponse boncuk = petService.create(new PetRequest(mehmet.id(), "Boncuk", "DOG", "Golden Retriever", null,
                now.minusYears(3).toLocalDate(), "FEMALE", 22.5, "Penicillin", null));
        PetResponse duman = petService.create(new PetRequest(mehmet.id(), "Duman", "CAT", "British Shorthair", null,
                now.minusMonths(4).toLocalDate(), "MALE", 1.8, null, null));
        PetResponse pamuk = petService.create(new PetRequest(ayse.id(), "Pamuk", "CAT", "Persian", null,
                now.minusYears(5).toLocalDate(), "FEMALE", 4.2, null, null));
        PetResponse karabas = petService.create(new PetRequest(can.id(), "Karabas", "DOG", "Kangal", null,
                now.minusYears(7).toLocalDate(), "MALE", 30.0, null, null));
        PetResponse sisi = petService.create(new PetRequest(zeynep.id(), "Sisi", "RABBIT", null,
                "Netherland Dwarf rabbit", now.minusYears(1).toLocalDate(), "FEMALE", 1.5, null, null));
        PetResponse zorro = petService.create(new PetRequest(zeynep.id(), "Zorro", "DOG", "Labrador", null,
                now.minusYears(2).toLocalDate(), "MALE", 18.0, null, null));
        PetResponse minnos = petService.create(new PetRequest(emre.id(), "Minnos", "CAT", "Tabby", null,
                now.minusYears(1).minusMonths(6).toLocalDate(), "FEMALE", 3.5, null, null));

        VisitResponse boncukSkinVisit = createCompletedVisit(boncuk.id(), vetAhmet.id(),
                now.minusDays(20).withHour(10).withMinute(0), "Annual checkup", "Mild skin infection",
                "Prescribed Penicillin ointment for skin infection", now.minusDays(6).toLocalDate());
        createCompletedVisit(duman.id(), vetElif.id(), now.minusDays(5).withHour(11).withMinute(0),
                "Kitten wellness check", "Healthy", "No issues noted", null);
        VisitResponse pamukVisit = createCompletedVisit(pamuk.id(), vetAhmet.id(),
                now.minusDays(45).withHour(14).withMinute(0), "Routine checkup", "Healthy",
                "Routine bloodwork normal", null);
        VisitResponse karabasVisit = createCompletedVisit(karabas.id(), vetElif.id(),
                now.minusYears(2).minusMonths(3).withHour(9).withMinute(30), "Old checkup", "Healthy",
                "No issues noted", null);
        VisitResponse zorroVisit = createCompletedVisit(zorro.id(), vetAhmet.id(),
                now.minusDays(10).withHour(9).withMinute(0), "Limping", "Minor sprain",
                "Rest recommended, re-check in 2 weeks", now.plusDays(4).toLocalDate());
        VisitResponse minnosVisit = createCompletedVisit(minnos.id(), vetElif.id(),
                now.minusDays(2).withHour(15).withMinute(0), "Vomiting", "Mild gastritis",
                "Prescribed bland diet", null);

        visitService.create(new VisitRequest(sisi.id(), vetAhmet.id(), LocalDate.now().atTime(16, 0), "Nail trim"));
        createCheckedInVisit(zorro.id(), vetElif.id(), LocalDate.now().atTime(11, 0), "Post-sprain recheck");
        visitService.create(new VisitRequest(boncuk.id(), vetElif.id(),
                now.plusDays(3).withHour(10).withMinute(30), "Follow-up dental check"));
        createCancelledVisit(pamuk.id(), vetAhmet.id(), now.minusDays(60).withHour(10).withMinute(0), "No-show");

        vaccinationService.create(new VaccinationRequest(boncuk.id(), "ANNUAL", now.minusDays(400), "LOT-A1", "Ahmet Yildiz"), Role.VET);
        vaccinationService.create(new VaccinationRequest(boncuk.id(), "THREE_YEAR", now.minusDays(30), "LOT-B2", "Ahmet Yildiz"), Role.VET);
        vaccinationService.create(new VaccinationRequest(duman.id(), "ANNUAL", now.minusDays(10), "LOT-C3", "Elif Sahin"), Role.VET);
        vaccinationService.create(new VaccinationRequest(pamuk.id(), "ANNUAL", now.minusDays(340), "LOT-D4", "Ahmet Yildiz"), Role.VET);
        vaccinationService.create(new VaccinationRequest(zorro.id(), "THREE_YEAR", now.minusYears(1), "LOT-E5", "Elif Sahin"), Role.VET);
        vaccinationService.create(new VaccinationRequest(minnos.id(), "ANNUAL", now.minusDays(5), "LOT-F6", "Elif Sahin"), Role.VET);

        petWeightRecordService.create(boncuk.id(), new PetWeightRecordRequest(21.0, now.minusDays(180), "Initial weigh-in"));
        petWeightRecordService.create(boncuk.id(), new PetWeightRecordRequest(21.8, now.minusDays(90), null));
        petWeightRecordService.create(boncuk.id(), new PetWeightRecordRequest(22.5, now.minusDays(20), "At skin infection visit"));
        petWeightRecordService.create(zorro.id(), new PetWeightRecordRequest(17.0, now.minusDays(120), "Initial weigh-in"));
        petWeightRecordService.create(zorro.id(), new PetWeightRecordRequest(18.0, now.minusDays(10), "At sprain visit"));

        // left as DRAFT to demo the not-yet-sent state
        invoiceService.create(new InvoiceRequest(boncukSkinVisit.id(), List.of(
                new InvoiceItemRequest("Skin infection consultation", InvoiceItemCategory.CONSULTATION, 1, new BigDecimal("450.00")),
                new InvoiceItemRequest("Penicillin ointment", InvoiceItemCategory.OTHER, 1, new BigDecimal("120.00")))));

        InvoiceResponse pamukInvoice = invoiceService.create(new InvoiceRequest(pamukVisit.id(), List.of(
                new InvoiceItemRequest("Routine checkup", InvoiceItemCategory.CONSULTATION, 1, new BigDecimal("500.00")),
                new InvoiceItemRequest("Bloodwork panel", InvoiceItemCategory.OTHER, 1, new BigDecimal("350.00")),
                new InvoiceItemRequest("Annual booster", InvoiceItemCategory.VACCINATION, 1, new BigDecimal("200.00")))));
        invoiceService.markPaid(pamukInvoice.id());

        InvoiceResponse karabasInvoice = invoiceService.create(new InvoiceRequest(karabasVisit.id(), List.of(
                new InvoiceItemRequest("Checkup", InvoiceItemCategory.CONSULTATION, 1, new BigDecimal("300.00")))));
        invoiceService.markPaid(karabasInvoice.id());

        InvoiceResponse zorroInvoice = invoiceService.create(new InvoiceRequest(zorroVisit.id(), List.of(
                new InvoiceItemRequest("Sprain examination", InvoiceItemCategory.CONSULTATION, 1, new BigDecimal("400.00")))));
        invoiceService.markPaid(zorroInvoice.id());

        InvoiceResponse minnosInvoice = invoiceService.create(new InvoiceRequest(minnosVisit.id(), List.of(
                new InvoiceItemRequest("Consultation - vomiting", InvoiceItemCategory.CONSULTATION, 1, new BigDecimal("400.00")),
                new InvoiceItemRequest("Bland diet prescription", InvoiceItemCategory.OTHER, 1, new BigDecimal("150.00")))));
        invoiceService.markSent(minnosInvoice.id());

        log.info("Demo data seeding completed");
    }

    private VisitResponse createCompletedVisit(Long petId, Long vetId, LocalDateTime scheduledAt, String chiefComplaint,
                                                String diagnosis, String treatmentNotes, LocalDate followUpDate) {
        VisitResponse created = visitService.create(new VisitRequest(petId, vetId, scheduledAt, chiefComplaint));
        visitService.updateStatus(created.id(), new VisitStatusUpdateRequest(VisitStatus.COMPLETED));
        return visitService.updateMedicalNotes(created.id(),
                new MedicalNotesUpdateRequest(diagnosis, treatmentNotes, followUpDate), Role.VET);
    }

    private void createCheckedInVisit(Long petId, Long vetId, LocalDateTime scheduledAt, String chiefComplaint) {
        VisitResponse created = visitService.create(new VisitRequest(petId, vetId, scheduledAt, chiefComplaint));
        visitService.updateStatus(created.id(), new VisitStatusUpdateRequest(VisitStatus.CHECKED_IN));
    }

    private void createCancelledVisit(Long petId, Long vetId, LocalDateTime scheduledAt, String chiefComplaint) {
        VisitResponse created = visitService.create(new VisitRequest(petId, vetId, scheduledAt, chiefComplaint));
        visitService.updateStatus(created.id(), new VisitStatusUpdateRequest(VisitStatus.CANCELLED));
    }
}
