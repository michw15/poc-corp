package pl.corpai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrbrData {
    private String nip;
    private List<CrbrBeneficiary> beneficiaries;
}
