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
public class AccountIdentification {
    @XmlElement(name = "IBAN", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02")
    private String iban;
}
