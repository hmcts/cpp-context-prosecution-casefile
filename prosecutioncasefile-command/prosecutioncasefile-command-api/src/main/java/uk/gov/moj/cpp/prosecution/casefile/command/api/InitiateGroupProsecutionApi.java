package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OffenceLocationHelper.getOffenceLocation;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.CaseDetailsEnrichmentService;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.GroupProsecutions;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateGroupProsecution;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class InitiateGroupProsecutionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateGroupProsecutionApi.class);
    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    @Handles("prosecutioncasefile.command.initiate-group-prosecution")
    public void initiateGroupProsecution(final Envelope<InitiateGroupProsecution> envelope) {

        final InitiateGroupProsecution initiateGroupProsecution = envelope.payload();
        LOGGER.info("prosecutioncasefile.command.initiate-group-prosecution with external Id {}", initiateGroupProsecution.getExternalId());
        final Optional<GroupProsecutions> groupProsecutionSelected = initiateGroupProsecution.getGroupProsecutions().stream().findFirst();
        if (!groupProsecutionSelected.isPresent()) {
            LOGGER.info("No GroupProsecutions for submission id {} with  groupProsecutionSelected  as empty ", initiateGroupProsecution.getExternalId());
            return;
        }

        final Optional<Offence> offence = groupProsecutionSelected.get().getDefendants().stream().flatMap(s -> s.getOffences().stream()).findFirst();
        final String initiationCode = groupProsecutionSelected.get().getCaseDetails().getInitiationCode();

        if (!offence.isPresent() || isNullOrEmpty(initiationCode)) {
            LOGGER.info("No offence or initiationCode submission id {}   ", initiateGroupProsecution.getExternalId());
            return;
        }
        final String prosecutionAuthority = getProsecutionAuthorityShortName(envelope, groupProsecutionSelected.get());
        final OffenceReferenceData offenceReferenceData = getOffenceReferenceData(offence.get(), initiationCode);

        final List<GroupProsecution> groupProsecutions = initiateGroupProsecution.getGroupProsecutions()
                .stream()
                .map(groupProsecution -> {
                    final List<Defendant> defendants = enrichDefendants(envelope, groupProsecution, prosecutionAuthority, offenceReferenceData);
                    return new GroupProsecution(this.caseDetailsEnrichmentService.enrichCaseDetails(groupProsecution.getCaseDetails(), groupProsecution.getCaseDetails().getProsecutor()),
                            defendants,
                            groupProsecution.getGroupId(),
                            groupProsecution.getIsCivil(),
                            groupProsecution.getIsGroupMaster(),
                            groupProsecution.getIsGroupMember(),
                            groupProsecution.getPaymentReference()
                    );
                }).collect(Collectors.toList());

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutions.stream().map(GroupProsecutionWithReferenceData::new).collect(Collectors.toList()),
                initiateGroupProsecution.getExternalId(),
                initiateGroupProsecution.getChannel());
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.initiate-group-prosecution-with-reference-data")
                .build();
        this.sender.send(envelopeFrom(metadata, groupProsecutionList));
    }

    private List<Defendant> enrichDefendants(final Envelope<InitiateGroupProsecution> envelope, final GroupProsecutions groupProsecutions, final String prosecutionAuthority,final OffenceReferenceData offenceReferenceData ) {
        final List<Defendant> defendants = groupProsecutions.getDefendants();

        final List<Defendant> newDefendants = new ArrayList<>();
        for (final Defendant defendant : defendants) {
            final List<Offence> offences = defendant
                    .getOffences()
                    .stream()
                    .map(offence -> this.enrichOffence(offence, prosecutionAuthority, envelope.payload().getChannel(), offenceReferenceData))
                    .collect(Collectors.toList());
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
                    .withLanguageRequirement(defendant.getLanguageRequirement())
                    .withInitiationCode(defendant.getInitiationCode())
                    .withNumPreviousConvictions(defendant.getNumPreviousConvictions())
                    .withOffences(offences)
                    .withOrganisationName(defendant.getOrganisationName())
                    .withPncIdentifier(defendant.getPncIdentifier())
                    .withPostingDate(defendant.getPostingDate())
                    .withProsecutorDefendantReference(defendant.getProsecutorDefendantReference())
                    .withSpecificRequirements(defendant.getSpecificRequirements())
                    .withTelephoneNumberBusiness(defendant.getTelephoneNumberBusiness())
                    .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                    .build()
            );
        }
        return newDefendants;
    }

    private Offence enrichOffence(final Offence offence, final String prosecutionAuthority, final Channel channel, final OffenceReferenceData offenceReferenceData) {
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

        return Offence.offence()
                .withAlcoholRelatedOffence(offence.getAlcoholRelatedOffence())
                .withAppliedCompensation(offence.getAppliedCompensation())
                .withArrestDate(offence.getArrestDate())
                .withBackDuty(offence.getBackDuty())
                .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                .withBackDutyDateTo(offence.getBackDutyDateTo())
                .withChargeDate(offence.getChargeDate())
                .withCivilOffence(offence.getCivilOffence())
                .withLaidDate(offence.getLaidDate())
                .withMaxPenalty(offence.getMaxPenalty())
                .withMotReasonId(offence.getMotReasonId())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceCommittedDate(getOffenceCommittedDate(offence))
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
                .build();

    }

    private static LocalDate getOffenceCommittedDate(final Offence offence) {
        LocalDate offenceCommittedDate = offence.getOffenceCommittedDate();
        if (nonNull(offence.getLaidDate()) && isNull(offenceCommittedDate)) {
            offenceCommittedDate = offence.getLaidDate();
        }

        if (isNull(offence.getOffenceCommittedDate()) && isNull(offence.getLaidDate())) {
            offenceCommittedDate = now();
        }
        return offenceCommittedDate;
    }

    private OffenceReferenceData getOffenceReferenceData(final Offence offence, final String initiationCode) {
        final List<OffenceReferenceData> offencesRefData = this.referenceDataQueryService.retrieveOffenceData(offence, initiationCode);
        return (offencesRefData != null && !offencesRefData.isEmpty()) ? offencesRefData.get(0) : null;
    }

    private String getProsecutionAuthorityShortName(final Envelope<InitiateGroupProsecution> envelope, final GroupProsecutions groupProsecutions) {
        final Prosecutor prosecutor = groupProsecutions.getCaseDetails().getProsecutor();
        ProsecutorsReferenceData prosecutorsReferenceData = null;
        if (nonNull(prosecutor.getProsecutingAuthority())) {
            prosecutorsReferenceData = this.referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), prosecutor.getProsecutingAuthority());
        } else if (nonNull(prosecutor.getProsecutionAuthorityId())) {
            prosecutorsReferenceData = this.referenceDataQueryService.getProsecutorById(prosecutor.getProsecutionAuthorityId());
        }
        return nonNull(prosecutorsReferenceData) ? prosecutorsReferenceData.getShortName() : null;
    }
}
