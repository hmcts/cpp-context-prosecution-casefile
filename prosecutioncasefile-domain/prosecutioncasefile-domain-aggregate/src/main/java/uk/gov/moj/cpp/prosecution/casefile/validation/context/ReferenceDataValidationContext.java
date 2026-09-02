package uk.gov.moj.cpp.prosecution.casefile.validation.context;


import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ReferenceDataValidationContext {
    final List<OffenceReferenceData> offenceCodeReferenceData;
    final List<ReferenceDataCountryNationality> nationalityReferenceData;

    public static ReferenceDataValidationContext withOffenceCodeReferenceDataOnly(final List<OffenceReferenceData> offenceCodeReferenceData) {

        return new ReferenceDataValidationContext(offenceCodeReferenceData, EMPTY_LIST);
    }

    public static ReferenceDataValidationContext newInstance(final List<OffenceReferenceData> offenceCodeReferenceData,
                                                             final List<ReferenceDataCountryNationality> nationalityReferenceData) {

        return new ReferenceDataValidationContext(offenceCodeReferenceData, nationalityReferenceData);
    }

    private ReferenceDataValidationContext(final List<OffenceReferenceData> offenceCodeReferenceData,
                                           final List<ReferenceDataCountryNationality> nationalityReferenceData) {

        this.offenceCodeReferenceData = offenceCodeReferenceData;
        this.nationalityReferenceData = nationalityReferenceData;
    }

    public List<OffenceReferenceData> getOffenceCodeReferenceData() {
        return offenceCodeReferenceData;
    }

    public List<ReferenceDataCountryNationality> getNationalityReferenceData() {
        return nationalityReferenceData;
    }

    public List<String> getNationalitiesIsoCodes() {
        return getNationalityReferenceData().stream()
                .map(ReferenceDataCountryNationality::getIsoCode)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public Optional<OffenceReferenceData> getReferenceDataByOffenceCode(String offenceCode) {
        return
                getOffenceCodeReferenceData()
                        .stream()
                        .filter(rd -> rd.getCjsOffenceCode().equals(offenceCode))
                        .findFirst();

    }

    // Relies on the upstream query pairing blacklisted with blacklistValidFrom/To (or leaving all three null) so the window disambiguates same-code entries fetched for different committed dates.
    public boolean isOffenceBlacklisted(final String cjsOffenceCode, final LocalDate offenceCommittedDate) {
        return getOffenceCodeReferenceData().stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(cjsOffenceCode))
                .filter(rd -> rd.getBlacklistValidFrom() == null || rd.getBlacklistValidTo() == null
                        || (!offenceCommittedDate.isBefore(LocalDate.parse(rd.getBlacklistValidFrom()))
                        && !offenceCommittedDate.isAfter(LocalDate.parse(rd.getBlacklistValidTo()))))
                .findFirst()
                .map(OffenceReferenceData::getBlacklisted)
                .orElse(Boolean.FALSE);
    }

}
