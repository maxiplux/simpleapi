package app.quantun.simpleapi.model.contract.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransferTransactionInformation {
    @XmlElement(name = "PmtId", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private PaymentIdentification pmtId;

    @XmlElement(name = "InstdAmt", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Amount instdAmt;

    @XmlElement(name = "RmtInf", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")
    private RemittanceInformation rmtInf;
}
