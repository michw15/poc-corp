package pl.corpai.orchestrator.service;

import org.springframework.stereotype.Service;
import pl.corpai.common.dto.BoardMember;
import pl.corpai.common.dto.KrsData;
import pl.corpai.common.dto.SanitizedCompanyPayload;
import pl.corpai.common.dto.ScrapingHints;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataSanitizer {

    public SanitizedCompanyPayload sanitize(KrsData krsData) {
        List<String> boardInitials = new ArrayList<>();
        if (krsData.getBoard() != null) {
            for (BoardMember member : krsData.getBoard()) {
                boardInitials.add(toInitials(member.getName()));
            }
        }

        String searchQuery = buildSearchQuery(krsData.getName(), krsData.getCity());
        ScrapingHints scrapingHints = ScrapingHints.builder()
                .searchQuery(searchQuery)
                .website(krsData.getWebsite())
                .build();

        return SanitizedCompanyPayload.builder()
                .krsNumber(krsData.getKrs())
                .companyName(krsData.getName())
                .nip(krsData.getNip())
                .pkdMain(krsData.getPkdMain())
                .pkdCodes(krsData.getPkdCodes())
                .city(krsData.getCity())
                .voivodeship(krsData.getVoivodeship())
                .registrationDate(krsData.getRegistrationDate())
                .shareCapital(krsData.getShareCapital())
                .boardInitials(boardInitials)
                .scrapingHints(scrapingHints)
                .build();
    }

    private String toInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0))).append(".");
            }
        }
        return initials.toString();
    }

    private String buildSearchQuery(String companyName, String city) {
        StringBuilder query = new StringBuilder();
        if (companyName != null && !companyName.isEmpty()) {
            query.append(companyName);
        }
        if (city != null && !city.isEmpty()) {
            query.append(" ").append(city);
        }
        return query.toString().trim();
    }
}
