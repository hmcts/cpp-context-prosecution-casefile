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
        return source.stream().map(defendant ->
                courtApplicationParty()
                        .withId(fromString(defendant.getId()))
                        .withMasterDefendant(buildMasterDefendant(defendant, referenceData, channel))
                        .withSummonsRequired(true)
                        .withNotificationRequired(false)
                        .build())
                .collect(toList());
    }

    private uk.gov.justice.core.courts.MasterDefendant buildMasterDefendant(final Defendant source, final ReferenceDataVO referenceData, final Channel channel) {
        String prosecutionDefendantReference = null;
        if (Channel.MCC.equals(channel) && nonNull(source.getAsn())) {
            prosecutionDefendantReference = source.getAsn();
        }
        final MasterDefendant.Builder masterDefendantBuilder = masterDefendant()
                .withMasterDefendantId(fromString(source.getId()))
                .withProsecutionAuthorityReference(nonNull(source.getProsecutorDefendantReference()) ? source.getProsecutorDefendantReference() : prosecutionDefendantReference)
                .withIsYouth(isDefendantYouth(source))
                .withPersonDefendant(nonNull(source.getIndividual()) ? prosecutionCaseToCCPersonDefendantConverter.convert(source, referenceData) : null)
                .withLegalEntityDefendant(prosecutionCaseFileToCCLegalEntityDefendantConverter.convert(source));

        return masterDefendantBuilder.build();
    }

    private boolean isDefendantYouth(final Defendant source) {
        if (isNull(source.getIndividual()) || isNull(source.getInitialHearing())) {
            return false;
        }

        final String dateOfHearing = source.getInitialHearing().getDateOfHearing();
        final LocalDate dateOfBirth = source.getIndividual().getSelfDefinedInformation().getDateOfBirth();
        if (isNull(dateOfHearing) || isNull(dateOfBirth)) {
            return false;
        }

        final Period p = Period.between(dateOfBirth, LocalDate.parse(dateOfHearing, DATE_OF_HEARING_PATTERN));
        return p.getYears() < 18;
    }
}
