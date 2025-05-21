package app.quantun.simpleapi.model.contract.dto;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInformation {
    @XmlElement(name = "PmtInfId", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private String pmtInfId;

    @XmlElement(name = "PmtMtd", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private String pmtMtd;

    @XmlElement(name = "NbOfTxs", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Integer nbOfTxs;

    @XmlElement(name = "CtrlF425b13d57157", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")
    private BigDecimal ctrlSum;

    @XmlElement(name = "PmtTpInf", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")
    private PaymentTypeInformation pmtTpInf;

    @XmlElement(name = "ReqdExctnDt", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate reqdExctnDt;

    @XmlElement(name = "Debtor", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private PartyIdentification debtor;

    @XmlElement(name = "DebtorAcct", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Account debtorAcct;

    @XmlElement(name = "DebtorAgt", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Agent debtorAgt;

    @XmlElement(name = "Cdtr", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private PartyIdentification cdtr;

    @XmlElement(name = "CdtrAcct", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Account cdtrAcct;

    @XmlElement(name = "CdtrAgt", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Agent cdtrAgt;

    @XmlElement(name = "PmtDtls", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private PaymentDetails pmtDtls;
}
