package pl.corpai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrbrBeneficiary {
    private String firstName;
    private String lastName;
    private BigDecimal sharePercent;
    private String controlType;
}
