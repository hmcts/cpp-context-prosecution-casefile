package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.HearingDateTimeConstants.DATE_OF_HEARING_PATTERN;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import javax.inject.Inject;

public class ProsecutionCaseFileDefendantToCourtApplicationPartyConverter implements DualParameterisedConverter<List<Defendant>, List<CourtApplicationParty>, ReferenceDataVO, Channel> {

    @Inject
    private ProsecutionCaseFileToCCLegalEntityDefendantConverter prosecutionCaseFileToCCLegalEntityDefendantConverter;

    @Inject
    private ProsecutionCaseToCCPersonDefendantConverter prosecutionCaseToCCPersonDefendantConverter;

    @Override
    public List<CourtApplicationParty> convert(final List<Defendant> source, final ReferenceDataVO referenceData, final Channel channel) {
        return convert(source, referenceData, channel, null);
    }

    /**
     * Overload carrying the case-level found hearing ("find a hearing"), which is the only hearing
     * date available when a defendant has no {@code initialHearing}.
     */
    public List<CourtApplicationParty> convert(final List<Defendant> source, final ReferenceDataVO referenceData, final Channel channel, final HearingRequest listNewHearing) {
        return source.stream().map(defendant ->
                courtApplicationParty()
                        .withId(fromString(defendant.getId()))
                        .withMasterDefendant(buildMasterDefendant(defendant, referenceData, channel, listNewHearing))
                        .withSummonsRequired(true)
                        .withNotificationRequired(false)
                        .build())
                .collect(toList());
    }

    private uk.gov.justice.core.courts.MasterDefendant buildMasterDefendant(final Defendant source, final ReferenceDataVO referenceData, final Channel channel, final HearingRequest listNewHearing) {
        String prosecutionDefendantReference = null;
        if (Channel.MCC.equals(channel) && nonNull(source.getAsn())) {
            prosecutionDefendantReference = source.getAsn();
        }
        final MasterDefendant.Builder masterDefendantBuilder = masterDefendant()
                .withMasterDefendantId(fromString(source.getId()))
                .withProsecutionAuthorityReference(nonNull(source.getProsecutorDefendantReference()) ? source.getProsecutorDefendantReference() : prosecutionDefendantReference)
                .withIsYouth(isDefendantYouth(source, listNewHearing))
                .withPersonDefendant(nonNull(source.getIndividual()) ? prosecutionCaseToCCPersonDefendantConverter.convert(source, referenceData) : null)
                .withLegalEntityDefendant(prosecutionCaseFileToCCLegalEntityDefendantConverter.convert(source));

        return masterDefendantBuilder.build();
    }

    private boolean isDefendantYouth(final Defendant source, final HearingRequest listNewHearing) {
        if (isNull(source.getIndividual())) {
            return false;
        }

        // Branch on the presence of initialHearing, never on channel — see
        // ProsecutionToBoxHearingRequestConverter.
        final LocalDate hearingDate = nonNull(source.getInitialHearing())
                ? initialHearingDate(source.getInitialHearing().getDateOfHearing())
                : HearingRequestStart.startDate(listNewHearing).orElse(null);

        final LocalDate dateOfBirth = source.getIndividual().getSelfDefinedInformation().getDateOfBirth();
        if (isNull(hearingDate) || isNull(dateOfBirth)) {
            return false;
        }

        final Period p = Period.between(dateOfBirth, hearingDate);
        return p.getYears() < 18;
    }

    private LocalDate initialHearingDate(final String dateOfHearing) {
        return isNull(dateOfHearing) ? null : LocalDate.parse(dateOfHearing, DATE_OF_HEARING_PATTERN);
    }
}
