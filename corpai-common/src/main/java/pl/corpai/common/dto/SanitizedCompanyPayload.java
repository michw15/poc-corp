package pl.corpai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanitizedCompanyPayload {
    private String krsNumber;
    private String companyName;
    private String nip;
    private String pkdMain;
    private List<String> pkdCodes;
    private String city;
    private String voivodeship;
    private String registrationDate;
    private BigDecimal shareCapital;
    private List<String> boardInitials;
    private ScrapingHints scrapingHints;
}
