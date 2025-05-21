package app.quantun.simpleapi.model.contract.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "Document", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @XmlElement(name = "FIToFICstmrCdtTrf", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private FIToFICstmrCdtTrf fiToFICstmrCdtTrf;
}
