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
public class KrsData {
    private String krs;
    private String nip;
    private String regon;
    private String name;
    private String legalForm;
    private String pkdMain;
    private List<String> pkdCodes;
    private String city;
    private String voivodeship;
    private String registrationDate;
    private BigDecimal shareCapital;
    private List<BoardMember> board;
    private String website;
}
