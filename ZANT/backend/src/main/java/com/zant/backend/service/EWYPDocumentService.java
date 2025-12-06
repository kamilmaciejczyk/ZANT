package com.zant.backend.service;

import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.zant.backend.model.ewyp.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
public class EWYPDocumentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FONT_RESOURCE_PATH = "fonts/DejaVuSans.ttf";
    private final PdfFont pdfFont;

    public EWYPDocumentService() {
        this.pdfFont = loadFont();
    }

    /**
     * Generate EWYP report in DOCX format
     */
    public byte[] generateDocx(EWYPReport report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             XWPFDocument document = new XWPFDocument()) {

            // Title
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("EWYP");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            XWPFParagraph subtitleParagraph = document.createParagraph();
            subtitleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitleParagraph.createRun();
            subtitleRun.setText("Zawiadomienie o wypadku");
            subtitleRun.setFontSize(16);
            subtitleRun.setBold(true);

            addEmptyLine(document);

            // Dane osoby poszkodowanej
            addSectionTitle(document, "Dane osoby poszkodowanej");
            if (report.getInjuredPerson() != null) {
                InjuredPerson injured = report.getInjuredPerson();
                addField(document, "PESEL", injured.getPesel());
                addField(document, "Rodzaj, seria i numer dokumentu", 
                    injured.getIdDocumentType() + " " + injured.getIdDocumentNumber());
                addField(document, "Imię", injured.getFirstName());
                addField(document, "Nazwisko", injured.getLastName());
                addField(document, "Data urodzenia", formatDate(injured.getBirthDate()));
                addField(document, "Miejsce urodzenia", injured.getBirthPlace());
                addField(document, "Numer telefonu", injured.getPhoneNumber());

                // Address
                if (injured.getAddress() != null) {
                    Address address = injured.getAddress();
                    addSubsectionTitle(document, "Adres zamieszkania osoby poszkodowanej");
                    addField(document, "Ulica", address.getStreet());
                    addField(document, "Numer domu", address.getHouseNumber());
                    addField(document, "Numer lokalu", address.getApartmentNumber());
                    addField(document, "Kod pocztowy", address.getPostalCode());
                    addField(document, "Miejscowość", address.getCity());
                    addField(document, "Nazwa państwa", address.getCountry());
                }

                // Polish address or stay
                if (injured.getLastPolishAddressOrStay() != null) {
                    PolishAddress polishAddr = injured.getLastPolishAddressOrStay();
                    if (polishAddr.getStreet() != null && !polishAddr.getStreet().isEmpty()) {
                        addSubsectionTitle(document, "Adres ostatniego miejsca zamieszkania w Polsce / adres miejsca pobytu");
                        addField(document, "Ulica", polishAddr.getStreet());
                        addField(document, "Numer domu", polishAddr.getHouseNumber());
                        addField(document, "Numer lokalu", polishAddr.getApartmentNumber());
                        addField(document, "Kod pocztowy", polishAddr.getPostalCode());
                        addField(document, "Miejscowość", polishAddr.getCity());
                    }
                }
            }

            addEmptyLine(document);

            // Dane osoby zgłaszającej
            if (report.getReporter() != null && report.getReporter().getIsDifferentFromInjuredPerson() != null 
                && report.getReporter().getIsDifferentFromInjuredPerson()) {
                addSectionTitle(document, "Dane osoby, która zawiadamia o wypadku");
                Reporter reporter = report.getReporter();
                addField(document, "PESEL", reporter.getPesel());
                addField(document, "Rodzaj, seria i numer dokumentu", 
                    reporter.getIdDocumentType() + " " + reporter.getIdDocumentNumber());
                addField(document, "Imię", reporter.getFirstName());
                addField(document, "Nazwisko", reporter.getLastName());
                addField(document, "Data urodzenia", formatDate(reporter.getBirthDate()));
                addField(document, "Numer telefonu", reporter.getPhoneNumber());

                if (reporter.getAddress() != null) {
                    Address reporterAddress = reporter.getAddress();
                    addSubsectionTitle(document, "Adres zamieszkania osoby, która zawiadamia o wypadku");
                    addField(document, "Ulica", reporterAddress.getStreet());
                    addField(document, "Numer domu", reporterAddress.getHouseNumber());
                    addField(document, "Numer lokalu", reporterAddress.getApartmentNumber());
                    addField(document, "Kod pocztowy", reporterAddress.getPostalCode());
                    addField(document, "Miejscowość", reporterAddress.getCity());
                    addField(document, "Nazwa państwa", reporterAddress.getCountry());
                }

                addEmptyLine(document);
            }

            // Informacja o wypadku
            if (report.getAccidentInfo() != null) {
                addSectionTitle(document, "Informacja o wypadku");
                AccidentInfo accident = report.getAccidentInfo();
                addField(document, "1. Data wypadku", formatDate(accident.getAccidentDate()));
                addField(document, "   Godzina wypadku", accident.getAccidentTime());
                addField(document, "2. Miejsce wypadku", accident.getPlaceOfAccident());
                addField(document, "3. Planowana godzina rozpoczęcia pracy w dniu wypadku", accident.getPlannedWorkStartTime());
                addField(document, "   Planowana godzina zakończenia pracy w dniu wypadku", accident.getPlannedWorkEndTime());
                addField(document, "4. Rodzaj doznanych urazów", accident.getInjuriesDescription());
                addField(document, "5. Szczegółowy opis okoliczności, miejsca i przyczyn wypadku", 
                    accident.getCircumstancesAndCauses());
                addField(document, "6. Czy była udzielona pierwsza pomoc medyczna", 
                    accident.getFirstAidGiven() != null && accident.getFirstAidGiven() ? "TAK" : "NIE");
                if (accident.getFirstAidGiven() != null && accident.getFirstAidGiven()) {
                    addField(document, "   Nazwa i adres placówki służby zdrowia", accident.getFirstAidFacility());
                }
                addField(document, "7. Organ, który prowadził postępowanie w sprawie wypadku", 
                    accident.getInvestigatingAuthority());
                addField(document, "8. Czy wypadek powstał podczas obsługi maszyn, urządzeń", 
                    accident.getAccidentDuringMachineOperation() != null && accident.getAccidentDuringMachineOperation() 
                        ? "TAK" : "NIE");
                if (accident.getAccidentDuringMachineOperation() != null && accident.getAccidentDuringMachineOperation()) {
                    addField(document, "   Opis stanu technicznego i użytkowania", 
                        accident.getMachineConditionDescription());
                }
                addField(document, "9. Czy maszyna, urządzenie posiada atest/deklarację zgodności", 
                    formatBoolean(accident.getMachineHasCertificate()));
                addField(document, "10. Czy maszyna, urządzenie zostało wpisane do ewidencji środków trwałych", 
                    formatBoolean(accident.getMachineInFixedAssetsRegister()));

                addEmptyLine(document);
            }

            // Dane świadków wypadku
            if (report.getWitnesses() != null && !report.getWitnesses().isEmpty()) {
                addSectionTitle(document, "Dane świadków wypadku");
                int witnessNum = 1;
                for (WitnessInfo witness : report.getWitnesses()) {
                    addSubsectionTitle(document, "Świadek wypadku – " + witnessNum);
                    addField(document, "Imię", witness.getFirstName());
                    addField(document, "Nazwisko", witness.getLastName());
                    addField(document, "Ulica", witness.getStreet());
                    addField(document, "Numer domu", witness.getHouseNumber());
                    addField(document, "Numer lokalu", witness.getApartmentNumber());
                    addField(document, "Kod pocztowy", witness.getPostalCode());
                    addField(document, "Miejscowość", witness.getCity());
                    addField(document, "Nazwa państwa", witness.getCountry());
                    witnessNum++;
                }
                addEmptyLine(document);
            }

            // Załączniki
            if (report.getAttachments() != null) {
                addSectionTitle(document, "Załączniki");
                Attachments attachments = report.getAttachments();
                addCheckbox(document, attachments.getHasHospitalCardCopy(), 
                    "kserokopia karty informacyjnej ze szpitala/ zaświadczenia o udzieleniu pierwszej pomocy");
                addCheckbox(document, attachments.getHasProsecutorDecisionCopy(), 
                    "kserokopia postanowienia prokuratury");
                addCheckbox(document, attachments.getHasPowerOfAttorneyCopy(),
                    "skan pełnomocnictwa");
                addCheckbox(document, attachments.getHasDeathDocsCopy(), 
                    "kserokopia dokumentów dotyczących zgonu");
                addCheckbox(document, attachments.getHasOtherDocuments(), "inne dokumenty");
                if (attachments.getHasOtherDocuments() != null && attachments.getHasOtherDocuments() 
                    && attachments.getOtherDocuments() != null) {
                    for (Object doc : attachments.getOtherDocuments()) {
                        addField(document, "  - ", doc.toString());
                    }
                }
                addEmptyLine(document);
            }

            // Sposób odbioru odpowiedzi
            addSectionTitle(document, "Sposób odbioru odpowiedzi");
            addField(document, "", formatResponseDeliveryMethod(report.getResponseDeliveryMethod()));
            addEmptyLine(document);

            // Podpis
            if (report.getSignature() != null) {
                addSectionTitle(document, "Oświadczenie i podpis");
                XWPFParagraph declarationParagraph = document.createParagraph();
                XWPFRun declarationRun = declarationParagraph.createRun();
                declarationRun.setText("Oświadczam, że dane zawarte w zawiadomieniu podaję zgodnie z prawdą, " +
                    "co potwierdzam złożonym podpisem.");
                declarationRun.setFontSize(10);

                Signature signature = report.getSignature();
                addField(document, "Data", formatDate(signature.getDeclarationDate()));
                addField(document, "Czytelny podpis", signature.getSignatureName());
            }

            document.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating DOCX document", e);
        }
    }

    /**
     * Generate EWYP report in PDF format
     */
    public byte[] generatePdf(EWYPReport report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setFont(pdfFont);

            // Title
            Paragraph title = new Paragraph("EWYP")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(20);
            document.add(title);

            Paragraph subtitle = new Paragraph("Zawiadomienie o wypadku")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16);
            document.add(subtitle);

            document.add(new Paragraph("\n"));

            // Dane osoby poszkodowanej
            addPdfSectionTitle(document, "Dane osoby poszkodowanej");
            if (report.getInjuredPerson() != null) {
                InjuredPerson injured = report.getInjuredPerson();
                addPdfField(document, "PESEL", injured.getPesel());
                addPdfField(document, "Rodzaj, seria i numer dokumentu", 
                    injured.getIdDocumentType() + " " + injured.getIdDocumentNumber());
                addPdfField(document, "Imię", injured.getFirstName());
                addPdfField(document, "Nazwisko", injured.getLastName());
                addPdfField(document, "Data urodzenia", formatDate(injured.getBirthDate()));
                addPdfField(document, "Miejsce urodzenia", injured.getBirthPlace());
                addPdfField(document, "Numer telefonu", injured.getPhoneNumber());

                if (injured.getAddress() != null) {
                    Address address = injured.getAddress();
                    addPdfSubsectionTitle(document, "Adres zamieszkania");
                    addPdfField(document, "Ulica", address.getStreet());
                    addPdfField(document, "Numer domu", address.getHouseNumber());
                    addPdfField(document, "Numer lokalu", address.getApartmentNumber());
                    addPdfField(document, "Kod pocztowy", address.getPostalCode());
                    addPdfField(document, "Miejscowość", address.getCity());
                    addPdfField(document, "Nazwa państwa", address.getCountry());
                }
            }

            document.add(new Paragraph("\n"));

            // Informacja o wypadku
            if (report.getAccidentInfo() != null) {
                addPdfSectionTitle(document, "Informacja o wypadku");
                AccidentInfo accident = report.getAccidentInfo();
                addPdfField(document, "Data wypadku", formatDate(accident.getAccidentDate()));
                addPdfField(document, "Godzina wypadku", accident.getAccidentTime());
                addPdfField(document, "Miejsce wypadku", accident.getPlaceOfAccident());
                addPdfField(document, "Planowana godzina rozpoczęcia pracy", accident.getPlannedWorkStartTime());
                addPdfField(document, "Planowana godzina zakończenia pracy", accident.getPlannedWorkEndTime());
                addPdfField(document, "Rodzaj doznanych urazów", accident.getInjuriesDescription());
                addPdfField(document, "Okoliczności i przyczyny wypadku", accident.getCircumstancesAndCauses());
                addPdfField(document, "Czy była udzielona pierwsza pomoc", 
                    accident.getFirstAidGiven() != null && accident.getFirstAidGiven() ? "TAK" : "NIE");
                if (accident.getFirstAidGiven() != null && accident.getFirstAidGiven()) {
                    addPdfField(document, "Placówka służby zdrowia", accident.getFirstAidFacility());
                }
                addPdfField(document, "Organ prowadzący postępowanie", accident.getInvestigatingAuthority());
            }

            document.add(new Paragraph("\n"));

            // Świadkowie
            if (report.getWitnesses() != null && !report.getWitnesses().isEmpty()) {
                addPdfSectionTitle(document, "Świadkowie wypadku");
                int witnessNum = 1;
                for (WitnessInfo witness : report.getWitnesses()) {
                    addPdfSubsectionTitle(document, "Świadek " + witnessNum);
                    addPdfField(document, "Imię i nazwisko", 
                        witness.getFirstName() + " " + witness.getLastName());
                    addPdfField(document, "Adres", 
                        witness.getStreet() + " " + witness.getHouseNumber() + 
                        (witness.getApartmentNumber() != null ? "/" + witness.getApartmentNumber() : "") +
                        ", " + witness.getPostalCode() + " " + witness.getCity());
                        witnessNum++;
                }
            }

            document.add(new Paragraph("\n"));

            // Załączniki
            if (report.getAttachments() != null) {
                addPdfSectionTitle(document, "Załączniki");
                Attachments attachments = report.getAttachments();
                addPdfCheckbox(document, attachments.getHasHospitalCardCopy(),
                    "kserokopia karty informacyjnej ze szpitala/ zaświadczenia o udzieleniu pierwszej pomocy");
                addPdfCheckbox(document, attachments.getHasProsecutorDecisionCopy(),
                    "kserokopia postanowienia prokuratury");
                addPdfCheckbox(document, attachments.getHasPowerOfAttorneyCopy(),
                    "skan pełnomocnictwa");
                addPdfCheckbox(document, attachments.getHasDeathDocsCopy(),
                    "kserokopia dokumentów dotyczących zgonu");
                addPdfCheckbox(document, attachments.getHasOtherDocuments(), "inne dokumenty");
                if (attachments.getHasOtherDocuments() != null && attachments.getHasOtherDocuments()
                    && attachments.getOtherDocuments() != null) {
                    for (Object doc : attachments.getOtherDocuments()) {
                        addPdfField(document, "  - ", doc.toString());
                    }
                }
            }

            document.add(new Paragraph("\n"));

            // Podpis
            if (report.getSignature() != null) {
                addPdfSectionTitle(document, "Oświadczenie i podpis");
                Paragraph declaration = new Paragraph(
                    "Oświadczam, że dane zawarte w zawiadomieniu podaję zgodnie z prawdą, " +
                    "co potwierdzam złożonym podpisem.")
                    .setFontSize(10);
                document.add(declaration);
                
                Signature signature = report.getSignature();
                addPdfField(document, "Data", formatDate(signature.getDeclarationDate()));
                addPdfField(document, "Czytelny podpis", signature.getSignatureName());
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF document", e);
        }
    }

    // Helper methods for DOCX
    private void addSectionTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontSize(14);
        run.setUnderline(UnderlinePatterns.SINGLE);
    }

    private void addSubsectionTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontSize(12);
    }

    private void addField(XWPFDocument document, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun labelRun = paragraph.createRun();
        labelRun.setText(label + ": ");
        labelRun.setBold(true);
        labelRun.setFontSize(11);
        
        XWPFRun valueRun = paragraph.createRun();
        valueRun.setText(value);
        valueRun.setFontSize(11);
    }

    private void addCheckbox(XWPFDocument document, Boolean checked, String label) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText((checked != null && checked ? "☑" : "☐") + " " + label);
        run.setFontSize(11);
    }

    private void addEmptyLine(XWPFDocument document) {
        document.createParagraph();
    }

    // Helper methods for PDF
    private void addPdfSectionTitle(Document document, String title) {
        Paragraph paragraph = new Paragraph(title)
            .setBold()
            .setFontSize(14)
            .setUnderline();
        document.add(paragraph);
    }

    private void addPdfSubsectionTitle(Document document, String title) {
        Paragraph paragraph = new Paragraph(title)
            .setBold()
            .setFontSize(12);
        document.add(paragraph);
    }

    private void addPdfField(Document document, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        Paragraph paragraph = new Paragraph()
            .add(new Text(label + ": ").setBold())
            .add(new Text(value))
            .setFontSize(11);
        document.add(paragraph);
    }
    
    private void addPdfCheckbox(Document document, Boolean checked, String label) {
        Paragraph paragraph = new Paragraph((checked != null && checked ? "☑" : "☐") + " " + label)
            .setFontSize(11);
        document.add(paragraph);
    }

    // Utility methods
    private PdfFont loadFont() {
        try {
            ClassPathResource fontResource = new ClassPathResource(FONT_RESOURCE_PATH);
            byte[] fontBytes;
            try (InputStream inputStream = fontResource.getInputStream()) {
                fontBytes = inputStream.readAllBytes();
            }
            FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
            return PdfFontFactory.createFont(
                fontProgram,
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load PDF font for Polish characters", e);
        }
    }

    private String formatDate(String date) {
        if (date == null || date.isEmpty()) {
            return "";
        }
        return date;
    }

    private String formatBoolean(Boolean value) {
        if (value == null) {
            return "";
        }
        return value ? "TAK" : "NIE";
    }

    private String formatResponseDeliveryMethod(String method) {
        if (method == null) {
            return "";
        }
        switch (method) {
            case "PICKUP_AT_ZUS":
                return "W placówce ZUS (osobiście lub przez osobę upoważnioną)";
            case "BY_MAIL_TO_ADDRESS":
                return "Pocztą na adres korespondencyjny";
            case "TO_PUE_ACCOUNT":
                return "Na konto na Platformie Usług Elektronicznych (PUE ZUS)";
            default:
                return method;
        }
    }
}
