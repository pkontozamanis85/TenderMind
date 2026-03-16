package TenderMind.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchDto {
    private String title;
    private List<String> cpvItems;
    private List<String> organizations;
    private String signer;
    private String contractType;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Double totalCostFrom;
    private Double totalCostTo;
    private LocalDate cancelDateFrom;
    private LocalDate cancelDateTo;
    private String referenceNumber;
    private String procedureType;
    private OffsetDateTime finalDateFrom;
    private OffsetDateTime finalDateTo;
    private String aaht;
    private Boolean isModified;
}