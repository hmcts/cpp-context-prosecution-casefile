package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.justice.core.courts.BoxHearingRequest.boxHearingRequest;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.HearingDateTimeConstants.DATE_OF_HEARING_PATTERN;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.HearingDateTimeConstants.TIME_OF_HEARING_PATTERN;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.HearingDateTimeConstants.TIME_OF_HEARING_PATTERN_WITHOUT_MILLIS;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class ProsecutionToBoxHearingRequestConverter implements Converter<Prosecution, BoxHearingRequest> {

    private static final int TWO_WEEKS_IN_DAYS = 14;
    private static final String MAGISTRATES_COURT_HOUSE_TYPE = "B";

    @Inject
    private Clock clock;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private OrganisationUnitToCourtCentreConverter organisationUnitToCourtCentreConverter;

    @Override
    public BoxHearingRequest convert(final Prosecution source) {
        final CaseDetails caseDetails = source.getCaseDetails();
        final UUID caseId = caseDetails.getCaseId();
        final Defendant defendant = source.getDefendants().stream().findFirst().orElse(null);

        if (isNull(defendant)) {
            throw new ConverterException(format("Error converting from DefendantsParkedForSummonsApplicationApproval to InitiateCourtApplicationProceedings for case %s: no defendants found for case", caseId));
        }

        final String ouCode = defendant.getInitialHearing().getCourtHearingLocation();
        final List<OrganisationUnitReferenceData> organisationUnits = referenceDataQueryService.retrieveOrganisationUnits(ouCode);
        if (isEmpty(organisationUnits)) {
            throw new ConverterException(format("Error converting from DefendantsParkedForSummonsApplicationApproval to InitiateCourtApplicationProceedings for case %s: no organisation unit found in reference data for ouCode %s", caseId, ouCode));
        }

        final LocalDate applicationDueDate = calculateApplicationDueDate(defendant.getInitialHearing().getDateOfHearing(), defendant.getInitialHearing().getTimeOfHearing());
        final OrganisationUnitReferenceData organisationUnit = organisationUnits.get(0);
        final CourtCentre courtCentre = organisationUnitToCourtCentreConverter.convert(organisationUnit);
        return boxHearingRequest()
                .withApplicationDueDate(LocalDates.to(applicationDueDate))
                .withCourtCentre(courtCentre)
                .withJurisdictionType(MAGISTRATES_COURT_HOUSE_TYPE.equalsIgnoreCase(organisationUnit.getOucodeL1Code()) ? MAGISTRATES : CROWN)
                .build();

    }

    private LocalDate calculateApplicationDueDate(final String dateOfHearing, final String timeOfHearing) {
        final ZonedDateTime today = clock.now();
        if (dateOfHearing != null && timeOfHearing != null) {

            final ZonedDateTime potentialApplicationDueDate = ZonedDateTime.of(convertDateOfHearingToLocalDate(dateOfHearing), convertTimeOfHearingToLocalTime(timeOfHearing), UTC).minusDays(TWO_WEEKS_IN_DAYS);
            if (potentialApplicationDueDate.isAfter(today)) {
                return potentialApplicationDueDate.toLocalDate();
            }
        }
        return today.toLocalDate();
    }

    private LocalDate convertDateOfHearingToLocalDate(final String dateOfHearing) {
        return LocalDate.parse(dateOfHearing, DATE_OF_HEARING_PATTERN);
    }

    private LocalTime convertTimeOfHearingToLocalTime(final String timeOfHearing) {
        if (timeOfHearing.length() == 8) {
            return LocalTime.parse(timeOfHearing, TIME_OF_HEARING_PATTERN_WITHOUT_MILLIS);
        } else {
            return LocalTime.parse(timeOfHearing, TIME_OF_HEARING_PATTERN);
        }
    }

}
