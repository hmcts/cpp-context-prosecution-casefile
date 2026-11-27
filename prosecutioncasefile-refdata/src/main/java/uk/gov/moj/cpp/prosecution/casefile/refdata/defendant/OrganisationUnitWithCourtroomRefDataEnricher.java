package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.Objects.nonNull;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrganisationUnitWithCourtroomRefDataEnricher implements DefendantRefDataEnricher {

    private static final int COURT_HEARING_OU_CODE_LENGTH = 7;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final Map<String, Optional<OrganisationUnitWithCourtroomReferenceData>> organisationUnitWithCourtroomMap = new HashMap<>();

        defendantsWithReferenceDataList.forEach(defendantsWithReferenceData ->
            defendantsWithReferenceData.getDefendants().forEach(defendant -> {
                final InitialHearing initialHearing = defendant.getInitialHearing();
                final String ouCode = initialHearing != null ? initialHearing.getCourtHearingLocation() : null;

                if (isValidOuCode(ouCode) && !defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent()) {
                    if (!organisationUnitWithCourtroomMap.containsKey(ouCode)){
                        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData = referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(ouCode);
                        organisationUnitWithCourtroomMap.put(ouCode, optionalOrganisationUnitWithCourtroomReferenceData);
                    }

                    final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData = organisationUnitWithCourtroomMap.get(ouCode);
                    if (optionalOrganisationUnitWithCourtroomReferenceData.isPresent()) {
                        defendantsWithReferenceData.getReferenceDataVO().setOrganisationUnitWithCourtroomReferenceData(optionalOrganisationUnitWithCourtroomReferenceData);
                    }
                }
            })
        );
    }

    private boolean isValidOuCode(final String ouCode) {
        return nonNull(ouCode) && COURT_HEARING_OU_CODE_LENGTH == ouCode.length();
    }
}