package app.quantun.simpleapi.model.contract.dto;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentTest {

    @Test
    void testUnmarshalPaymentDocument() throws JAXBException, IOException {
        // Setup JAXB context
        JAXBContext context = JAXBContext.newInstance(Document.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        // Configure adapters
        unmarshaller.setAdapter(new LocalDateAdapter());
        unmarshaller.setAdapter(new LocalDateTimeAdapter());

        // Unmarshal XML
        Document document = (Document) unmarshaller.unmarshal(
                new ClassPathResource("xsd/example.xml").getInputStream()
        );

        // Verify the unmarshalled object
        assertNotNull(document);
        assertNotNull(document.getFiToFICstmrCdtTrf());

        // Verify group header
        GroupHeader grpHdr = document.getFiToFICstmrCdtTrf().getGrpHdr();
        assertEquals("SSOXFOCIHCASXOICOIOI3990WE09R90WQE", grpHdr.getMsgId());
        assertEquals(LocalDateTime.parse("2024-10-25T10:00:00"), grpHdr.getCreDtTm());
        assertEquals(1, grpHdr.getNbOfTxs());
        assertEquals(new BigDecimal("100.00"), grpHdr.getTtlIntrBkSttlmAmt().getValue());
        assertEquals("USD", grpHdr.getTtlIntrBkSttlmAmt().getCurrency());

        // Verify payment information
        PaymentInformation pmtInf = document.getFiToFICstmrCdtTrf().getPmtInf();
        assertEquals("PAYMENT_REFERENCE", pmtInf.getPmtInfId());
        assertEquals("TRF", pmtInf.getPmtMtd());
        assertEquals(1, pmtInf.getNbOfTxs());
        assertEquals(new BigDecimal("100.00"), pmtInf.getCtrlSum());
        assertEquals(LocalDate.parse("2024-10-25"), pmtInf.getReqdExctnDt());

        // Verify debtor info
        assertEquals("DEBTOR_NAME", pmtInf.getDebtor().getNm());
        assertEquals("DEBTOR_IBAN", pmtInf.getDebtorAcct().getId().getIban());
        assertEquals("DEBTOR_BANK_BIC", pmtInf.getDebtorAgt().getFinInstnId().getBic());

        // Verify creditor info
        assertEquals("CREDITOR_NAME", pmtInf.getCdtr().getNm());
        assertEquals("CREDITOR_IBAN", pmtInf.getCdtrAcct().getId().getIban());
        assertEquals("CREDITOR_BANK_BIC", pmtInf.getCdtrAgt().getFinInstnId().getBic());

        // Verify payment details
        CreditTransferTransactionInformation txInfo = pmtInf.getPmtDtls().getCdtTrfTxInf();
        assertEquals("END_TO_END_REFERENCE", txInfo.getPmtId().getEndToEndId());
        assertEquals(new BigDecimal("100.00"), txInfo.getInstdAmt().getValue());
        assertEquals("USD", txInfo.getInstdAmt().getCurrency());
        //assertEquals("REFERENCE_TEXT", txInfo.getRmtInf().getUstrd());

        // Verify payment type information
        //assertEquals("SEPA", pmtInf.getPmtTpInf().getPmtTp().getPrpty().getTp().getCd());
        //assertEquals("SEPA", pmtInf.getPmtTpInf().getPmtTp().getPrpty().getLocalMtd().getCd());
    }
}