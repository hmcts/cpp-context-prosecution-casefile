package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.IndicatedPleaValue.INDICATED_GUILTY;
import static uk.gov.justice.core.courts.InitiationCode.O;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.FIXED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.WEEK_COMMENCING;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleaType.GUILTY;
import static uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OffenceLocationHelper.getOffenceLocation;

import com.google.common.collect.Lists;

import uk.gov.justice.core.courts.CivilOffence;

import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.CaseDetailsEnrichmentService;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Plea;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

@SuppressWarnings({"java:S126", "java:S1541"})
@ServiceComponent(COMMAND_API)
public class InitiateCCProsecutionApi {

    public static final String CONVICTING_COURT_CODE_IS_MANDATORY = "convicting court code is mandatory";
    public static final String PLEA_DATE_MUST_BE_TODAY_OR_IN_THE_PAST = "plea date must be today or in the past";
    public static final String VERDICT_DATE_MUST_BE_TODAY_OR_IN_THE_PAST = "verdict date must be today or in the past";
    public static final String LIST_NEW_HEARING_AND_INITIAL_HEARING_ARE_MUTUALLY_EXCLUSIVE = "The List new hearing and Initial hearing are mutually exclusive";
    public static final String WEEK_COMMENCING_MUST_BE_PROVIDED = "Week commencing must be provided";
    public static final String EARLIEST_START_DATE_MUST_BE_FUTURE_DATE = "Earliest start date must be future date";
    public static final String EARLIEST_START_DATE_MUST_BE_PROVIDED = "Earliest start date must be provided";

    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    public static final String SOW_REF_VALUE = "MoJ";

    @SuppressWarnings("java:S1541")
    @Handles("prosecutioncasefile.command.initiate-cc-prosecution")
    public void initiateCCProsecution(final Envelope<InitiateProsecution> envelope) {

        if (envelope.payload().getDefendants().stream()
                .anyMatch(def -> nonNull(def.getIndividual()) && nonNull(def.getIndividual().getPersonalInformation())
                        && isNull(def.getIndividual().getPersonalInformation().getAddress()))) {
            throw new BadRequestException("The Address of the defendant is mandatory");
        }
        boolean isCivil = nonNull(envelope.payload().getIsCivil()) && envelope.payload().getIsCivil();
        if (isCivil && envelope.payload().getDefendants().stream()
                .anyMatch(defendant -> defendant.getOffences().stream()
                        .anyMatch(offence -> nonNull(offence.getChargeDate())))) {
            throw new BadRequestException("Charge date should not be provided for any offence when the case is civil");
        }


        Channel channel = envelope.payload().getChannel();
        HearingRequest listNewHearing = envelope.payload().getListNewHearing();

        boolean isMCCWithNewListAndInitialHearing =
                Channel.MCC.equals(channel) &&
                        nonNull(listNewHearing) &&
                        envelope.payload().getDefendants().stream()
                                .filter(Objects::nonNull)
                                .map(Defendant::getInitialHearing)
                                .anyMatch(Objects::nonNull);
        if (isMCCWithNewListAndInitialHearing) {
            throw new BadRequestException(LIST_NEW_HEARING_AND_INITIAL_HEARING_ARE_MUTUALLY_EXCLUSIVE);
        }

        if (MCC.equals(channel) && O.name().equalsIgnoreCase(envelope.payload().getCaseDetails().getInitiationCode())) {
            if (nonNull(listNewHearing)) {
                validateWcdAndFixedDate(listNewHearing);
            }
            if (!isCivil) {
                validatePleaAndVerdictInOffences(envelope);
            }
        }


        final List<Defendant> defendants = enrichDefendants(envelope);

        final InitiateProsecution initiateProsecution = envelope.payload();
        final Prosecution ccProsecution = Prosecution.prosecution()
                .withCaseDetails(caseDetailsEnrichmentService.enrichCaseDetails(initiateProsecution.getCaseDetails(), initiateProsecution.getCaseDetails().getProsecutor()))
                .withChannel(initiateProsecution.getChannel())
                .withMigrationSourceSystem(initiateProsecution.getMigrationSourceSystem())
                .withListNewHearing(initiateProsecution.getListNewHearing())
                .withDefendants(defendants)
                .withExternalId(initiateProsecution.getExternalId())
                .withIsCivil(initiateProsecution.getIsCivil())
                .withIsGroupMaster(initiateProsecution.getIsGroupMaster())
                .withIsGroupMember(initiateProsecution.getIsGroupMember())
                .build();

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(ccProsecution);
        prosecutionWithReferenceData.setExternalId(initiateProsecution.getExternalId());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.initiate-cc-prosecution-with-reference-data")
                .build();
        sender.send(envelopeFrom(metadata, prosecutionWithReferenceData));
    }


    private List<Defendant> enrichDefendants(final Envelope<InitiateProsecution> envelope) {
        final String prosecutionAuthority = getProsecutionAuthorityShortName(envelope);
        final List<Defendant> defendants = envelope.payload().getDefendants();
        final String initiationCode = envelope.payload().getCaseDetails().getInitiationCode();
        final boolean isCivil = nonNull(envelope.payload().getIsCivil()) && envelope.payload().getIsCivil();

        final List<Defendant> newDefendants = new ArrayList<>();
        for (final Defendant defendant : defendants) {
            final List<Offence> offences = defendant
                    .getOffences()
                    .stream()
                    .map(offence -> this.enrichOffence(offence, prosecutionAuthority, envelope.payload().getChannel(), isCivil))
                    .toList();
            newDefendants.add(Defendant.defendant()
                    .withAddress(defendant.getAddress())
                    .withAliasForCorporate(defendant.getAliasForCorporate())
                    .withAppliedProsecutorCosts(defendant.getAppliedProsecutorCosts())
                    .withAsn(defendant.getAsn())
                    .withCroNumber(defendant.getCroNumber())
                    .withCustodyStatus(defendant.getCustodyStatus())
                    .withDocumentationLanguage(defendant.getDocumentationLanguage())
                    .withEmailAddress1(defendant.getEmailAddress1())
                    .withEmailAddress2(defendant.getEmailAddress2())
                    .withHearingLanguage(defendant.getHearingLanguage())
                    .withId(defendant.getId())
                    .withIndividual(defendant.getIndividual())
                    .withIndividualAliases(defendant.getIndividualAliases())
                    .withInitialHearing(defendant.getInitialHearing())
                    .withInitiationCode(initiationCode)
                    .withLanguageRequirement(defendant.getLanguageRequirement())
                    .withNumPreviousConvictions(defendant.getNumPreviousConvictions())
                    .withOffences(offences)
                    .withOrganisationName(defendant.getOrganisationName())
                    .withPncIdentifier(defendant.getPncIdentifier())
                    .withPostingDate(defendant.getPostingDate())
                    .withProsecutorDefendantReference(defendant.getProsecutorDefendantReference())
                    .withSpecificRequirements(defendant.getSpecificRequirements())
                    .withTelephoneNumberBusiness(defendant.getTelephoneNumberBusiness())
                    .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                    .withLibraReferenceNumber(defendant.getLibraReferenceNumber())
                    .build()
            );
        }
        return newDefendants;
    }

    private Offence enrichOffence(final Offence offence, final String prosecutionAuthority, final Channel channel, final boolean isCivil) {
        String vehicleRegistrationMark = offence.getVehicleRegistrationMark();
        if (isBlank(offence.getVehicleRegistrationMark()) &&
                nonNull(offence.getVehicleRelatedOffence()) &&
                isNotBlank(offence.getVehicleRelatedOffence().getVehicleRegistrationMark())) {
            vehicleRegistrationMark = offence.getVehicleRelatedOffence().getVehicleRegistrationMark();
        }

        VehicleRelatedOffence vehicleRelatedOffence = offence.getVehicleRelatedOffence();
        if (isNotBlank(offence.getVehicleRegistrationMark())) {
            if (isNull(offence.getVehicleRelatedOffence())) {
                vehicleRelatedOffence = new VehicleRelatedOffence(null, offence.getVehicleRegistrationMark());
            } else if (isBlank(offence.getVehicleRelatedOffence().getVehicleRegistrationMark())) {
                vehicleRelatedOffence = new VehicleRelatedOffence(offence.getVehicleRelatedOffence().getVehicleCode(), offence.getVehicleRegistrationMark());
            }
        }

        Optional<String> sowRef = isCivil ? Optional.of(SOW_REF_VALUE) : Optional.empty();
        final OffenceReferenceData offenceReferenceData = getOffenceReferenceData(offence, sowRef);

        boolean isExParte = (nonNull(offenceReferenceData) && nonNull(offenceReferenceData.getExParte())) && offenceReferenceData.getExParte();

        return Offence.offence()
                .withAlcoholRelatedOffence(offence.getAlcoholRelatedOffence())
                .withAppliedCompensation(offence.getAppliedCompensation())
                .withArrestDate(offence.getArrestDate())
                .withBackDuty(offence.getBackDuty())
                .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                .withBackDutyDateTo(offence.getBackDutyDateTo())
                .withChargeDate(offence.getChargeDate())
                .withCivilOffence(getCivilOffence(isCivil ? isExParte : null))
                .withLaidDate(offence.getLaidDate())
                .withMaxPenalty(offence.getMaxPenalty())
                .withMotReasonId(offence.getMotReasonId())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceCommittedDate(getOffenceCommittedDate(offence, isCivil))
                .withOffenceCommittedEndDate(offence.getOffenceCommittedEndDate())
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withOffenceId(offence.getOffenceId())
                .withOffenceLocation(getOffenceLocation(offence, prosecutionAuthority, channel, offenceReferenceData))
                .withOffenceSequenceNumber(offence.getOffenceSequenceNumber())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOffenceWording(offence.getOffenceWording())
                .withOffenceWordingWelsh(offence.getOffenceWordingWelsh())
                .withOtherPartyVictim(offence.getOtherPartyVictim())
                .withPlea(offence.getPlea())
                .withProsecutorOfferAOCP(offence.getProsecutorOfferAOCP())
                .withReferenceData(offenceReferenceData)
                .withStatementOfFacts(offence.getStatementOfFacts())
                .withStatementOfFactsWelsh(offence.getStatementOfFactsWelsh())
                .withVehicleMake(offence.getVehicleMake())
                .withVehicleRegistrationMark(vehicleRegistrationMark)
                .withVehicleRelatedOffence(vehicleRelatedOffence)
                .withVerdict(offence.getVerdict())
                .withConvictingCourtCode(offence.getConvictingCourtCode())
                .build();

    }

    private CivilOffence getCivilOffence(final Boolean isExParte) {
        if(isExParte == null) {
            return null;
        }
        return CivilOffence.civilOffence().withIsExParte(isExParte).build();
    }

    private static LocalDate getOffenceCommittedDate(final Offence offence, final boolean isCivil) {
        LocalDate offenceCommittedDate = offence.getOffenceCommittedDate();
        if (isCivil && isNull(offenceCommittedDate)) {
            if (nonNull(offence.getLaidDate())) {
                offenceCommittedDate = offence.getLaidDate();
            }

            if (isNull(offence.getOffenceCommittedDate()) && isNull(offence.getLaidDate())) {
                offenceCommittedDate = now();
            }
        }
        return offenceCommittedDate;
    }

    private OffenceReferenceData getOffenceReferenceData(final Offence offence, final Optional<String> sowRef) {
        final List<OffenceReferenceData> offencesRefData = referenceDataQueryService.retrieveOffenceDataList(Lists.newArrayList(offence.getOffenceCode()), sowRef);
        return (offencesRefData != null && !offencesRefData.isEmpty()) ? offencesRefData.get(0) : null;
    }

    private String getProsecutionAuthorityShortName(final Envelope<InitiateProsecution> envelope) {
        final Prosecutor prosecutor = envelope.payload().getCaseDetails().getProsecutor();
        ProsecutorsReferenceData prosecutorsReferenceData = null;
        if (nonNull(prosecutor.getProsecutingAuthority())) {
            prosecutorsReferenceData = referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), prosecutor.getProsecutingAuthority());
        } else if (nonNull(prosecutor.getProsecutionAuthorityId())) {
            prosecutorsReferenceData = referenceDataQueryService.getProsecutorById(prosecutor.getProsecutionAuthorityId());
        }
        return nonNull(prosecutorsReferenceData) ? prosecutorsReferenceData.getShortName() : null;
    }

    private void validatePleaAndVerdictInOffences(Envelope<InitiateProsecution> envelope) {
        envelope.payload().getDefendants().stream()
                .flatMap(def -> def.getOffences().stream())
                .forEach(this::validatePleaAndVerdict);
    }

    private void validatePleaAndVerdict(final Offence offence) {
        final Plea plea = offence.getPlea();
        if (nonNull(plea)) {
            validatePlea(plea, offence.getConvictingCourtCode());
        }
        if (nonNull(offence.getVerdict())) {
            validateDateNotInFuture(offence.getVerdict().getVerdictDate(), VERDICT_DATE_MUST_BE_TODAY_OR_IN_THE_PAST);
        }
    }

    private void validatePlea(final Plea plea, String convictingCourtCode) {
        final boolean hasGuiltyPlea = INDICATED_GUILTY.name().equals(plea.getPleaValue()) || GUILTY.name().equals(plea.getPleaValue());
        if (hasGuiltyPlea
                && isNull(convictingCourtCode)) {
            throw new BadRequestException(CONVICTING_COURT_CODE_IS_MANDATORY);
        }
        validateDateNotInFuture(plea.getPleaDate(), PLEA_DATE_MUST_BE_TODAY_OR_IN_THE_PAST);
    }

    private void validateDateNotInFuture(final LocalDate date, final String errorMessage) {
        if (date.isAfter(now())) {
            throw new BadRequestException(errorMessage);
        }
    }

    private void validateWcdAndFixedDate(final HearingRequest listNewHearing) {
        HearingDateTimeType hearingDateTimeType = listNewHearing.getHearingDateTimeType();
        if (WEEK_COMMENCING.equals(hearingDateTimeType)) {
            WeekCommencingDate weekCommencingDate = listNewHearing.getWeekCommencingDate();
            validateNotNull(weekCommencingDate,WEEK_COMMENCING_MUST_BE_PROVIDED);
        } else if (FIXED.equals(hearingDateTimeType)) {
            validateNotNull(listNewHearing.getEarliestStartDateTime(),EARLIEST_START_DATE_MUST_BE_PROVIDED);
            LocalDate earliestStartDateTime = listNewHearing.getEarliestStartDateTime().toLocalDate();
            validateDateInTheFuture(earliestStartDateTime, EARLIEST_START_DATE_MUST_BE_FUTURE_DATE);
        }
    }

    private void validateDateInTheFuture(final LocalDate date, final String errorMessage) {
        if (date.isBefore(now())) {
            throw new BadRequestException(errorMessage);
        }
    }

    private void validateNotNull(final Object object,final String errorMessage){
        if (isNull(object)) {
            throw new BadRequestException(errorMessage);
        }
    }

}
