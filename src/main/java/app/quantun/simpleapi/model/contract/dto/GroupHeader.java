package app.quantun.simpleapi.model.contract.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupHeader {
    @XmlElement(name = "MsgId", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private String msgId;

    @XmlElement(name = "CreDtTm", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime creDtTm;

    @XmlElement(name = "NbOfTxs", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Integer nbOfTxs;

    @XmlElement(name = "TtlIntrBkSttlmAmt", namespace = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.02", required = true)
    private Amount ttlIntrBkSttlmAmt;
}
