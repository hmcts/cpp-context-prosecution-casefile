package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isWhitespace;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OffenceLocationHelper.getOffenceLocation;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.CaseDetailsEnrichmentService;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class InitiateSjpProsecutionApi {

    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private IdGenerator idGenerator;

    @Inject
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    @Handles("prosecutioncasefile.command.initiate-sjp-prosecution")
    public void initiateSjpProsecution(final Envelope<InitiateProsecution> envelope) {
        final InitiateProsecution initiateProsecution = enrichDefendants(envelope.payload());

        final Prosecutor prosecutorWithReferenceData = enrichProsecutor(
                envelope.metadata(),
                initiateProsecution.getCaseDetails().getProsecutor());

        final DefendantsWithReferenceData defendantsWithReferenceData = prosecutorWithReferenceData.getReferenceData() == null ?
                new DefendantsWithReferenceData(initiateProsecution.getDefendants()) :
                new DefendantsWithReferenceData(initiateProsecution.getDefendants(), prosecutorWithReferenceData.getReferenceData().getShortName());
        defendantRefDataEnrichers.forEach(x -> x.enrich(defendantsWithReferenceData));

        final Prosecution sjpProsecution = prosecution()
                .withChannel(initiateProsecution.getChannel())
                .withDefendants(defendantsWithReferenceData.getDefendants())
                .withCaseDetails(caseDetailsEnrichmentService.enrichCaseDetails(initiateProsecution.getCaseDetails(), prosecutorWithReferenceData))
                .withExternalId(initiateProsecution.getExternalId())
                .withIsCivil(initiateProsecution.getIsCivil())
                .withIsGroupMaster(initiateProsecution.getIsGroupMaster())
                .withIsGroupMember(initiateProsecution.getIsGroupMember())
                .build();
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(sjpProsecution);
        prosecutionWithReferenceData.setExternalId(initiateProsecution.getExternalId());

        prosecutionWithReferenceData.setReferenceDataVO(defendantsWithReferenceData.getReferenceDataVO());
        caseRefDataEnrichers.forEach(x -> x.enrich(prosecutionWithReferenceData));

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data")
                .build();
        sender.send(envelopeFrom(metadata, prosecutionWithReferenceData));
    }

    private Prosecutor enrichProsecutor(Metadata metadata, Prosecutor prosecutor) {
        return new Prosecutor(prosecutor.getInformant(),
                prosecutor.getProsecutingAuthority(),
                prosecutor.getProsecutionAuthorityId(),
                referenceDataQueryService.getProsecutorsByOuCode(metadata, prosecutor.getProsecutingAuthority()));
    }


    private InitiateProsecution enrichDefendants(final InitiateProsecution initiateSjpProsecution) {
        final List<Defendant> defendants = new ArrayList<>();
        for (final Defendant defendant : initiateSjpProsecution.getDefendants()) {

            if (nonNull(defendant.getOrganisationName())) {
                defendants.add(getDefendant(defendant, null, initiateSjpProsecution.getCaseDetails().getInitiationCode(), initiateSjpProsecution.getChannel()));
            }
            else {
                final ContactDetails contactDetails = convertBlankEmailAddressesToNull(defendant.getIndividual().getPersonalInformation().getContactDetails());
                final PersonalInformation personalInformation = getPersonalInformation(defendant.getIndividual().getPersonalInformation(), contactDetails);
                final Individual individual = getIndividual(defendant.getIndividual(), personalInformation);
                defendants.add(getDefendant(defendant, individual, initiateSjpProsecution.getCaseDetails().getInitiationCode(), initiateSjpProsecution.getChannel()));
            }
        }

        return InitiateProsecution.initiateProsecution()
                .withCaseDetails(initiateSjpProsecution.getCaseDetails())
                .withChannel(initiateSjpProsecution.getChannel())
                .withDefendants(defendants)
                .withExternalId(initiateSjpProsecution.getExternalId())
                .withIsCivil(false)
                .withIsGroupMaster(false)
                .withIsGroupMember(false)
                .build();

    }

    private ContactDetails convertBlankEmailAddressesToNull(final ContactDetails originalContactDetails) {
        if (nonNull(originalContactDetails)) {
            return new ContactDetails(originalContactDetails.getHome(),
                    originalContactDetails.getMobile(),
                    getNullIfBlank(originalContactDetails.getPrimaryEmail()),
                    getNullIfBlank(originalContactDetails.getSecondaryEmail()),
                    originalContactDetails.getWork());
        }
        return ContactDetails.contactDetails().build();
    }

    private String getNullIfBlank(final String value) {
        if (isWhitespace(value)) {
            return null;
        }
        return value;
    }

    private PersonalInformation getPersonalInformation(final PersonalInformation originalPersonalInformation, final ContactDetails contactDetails) {
        return new PersonalInformation(originalPersonalInformation.getAddress(),
                contactDetails,
                originalPersonalInformation.getFirstName(),
                originalPersonalInformation.getGivenName2(),
                originalPersonalInformation.getGivenName3(),
                originalPersonalInformation.getLastName(),
                originalPersonalInformation.getObservedEthnicity(),
                originalPersonalInformation.getOccupation(),
                originalPersonalInformation.getOccupationCode(),
                originalPersonalInformation.getTitle());
    }

    private Individual getIndividual(final Individual originalIndividual, final PersonalInformation personalInformation) {
        return new Individual(originalIndividual.getBailConditions(),
                originalIndividual.getCustodyStatus(),
                originalIndividual.getDriverLicenceCode(),
                originalIndividual.getDriverLicenceIssue(),
                originalIndividual.getDriverNumber(),
                originalIndividual.getNationalInsuranceNumber(),
                originalIndividual.getOffenderCode(),
                originalIndividual.getParentGuardianInformation(),
                originalIndividual.getPerceivedBirthYear(),
                personalInformation,
                originalIndividual.getSelfDefinedInformation());
    }

    private Defendant getDefendant(final Defendant originalDefendant, final Individual individual, final String initiationCode, final Channel channel) {
        final List<Offence> offences = originalDefendant
                .getOffences()
                .stream()
                .map(offence-> this.enrichOffence(offence, initiationCode, channel))
                .toList();

        return Defendant.defendant()
                .withAddress(nonNull(individual) ? null : originalDefendant.getAddress())
                .withAliasForCorporate(originalDefendant.getAliasForCorporate())
                .withAppliedProsecutorCosts(originalDefendant.getAppliedProsecutorCosts())
                .withAsn(originalDefendant.getAsn())
                .withCroNumber(originalDefendant.getCroNumber())
                .withCustodyStatus(originalDefendant.getCustodyStatus())
                .withDocumentationLanguage(originalDefendant.getDocumentationLanguage())
                .withEmailAddress1(nonNull(individual) && nonNull(individual.getPersonalInformation().getContactDetails())
                        ? null : originalDefendant.getEmailAddress1())
                .withEmailAddress2(nonNull(individual) && nonNull(individual.getPersonalInformation().getContactDetails())
                        ? null : originalDefendant.getEmailAddress2())
                .withHearingLanguage(originalDefendant.getHearingLanguage())
                .withId(ofNullable(originalDefendant.getId()).orElse(idGenerator.generateId().toString()))
                .withIndividual(individual)
                .withIndividualAliases(originalDefendant.getIndividualAliases())
                .withInitialHearing(originalDefendant.getInitialHearing())
                .withInitiationCode(initiationCode)
                .withLanguageRequirement(originalDefendant.getLanguageRequirement())
                .withNumPreviousConvictions(originalDefendant.getNumPreviousConvictions())
                .withOffences(offences)
                .withOrganisationName(originalDefendant.getOrganisationName())
                .withPncIdentifier(originalDefendant.getPncIdentifier())
                .withPostingDate(originalDefendant.getPostingDate())
                .withProsecutorDefendantReference(originalDefendant.getProsecutorDefendantReference())
                .withSpecificRequirements(originalDefendant.getSpecificRequirements())
                .withTelephoneNumberBusiness(originalDefendant.getTelephoneNumberBusiness())
                .build();

    }

    private Offence enrichOffence(final Offence offence, final String initiationCode, final Channel channel) {
        String vehicleRegistrationMark = offence.getVehicleRegistrationMark();
        if (isNull(vehicleRegistrationMark) &&
                nonNull(offence.getVehicleRelatedOffence()) &&
                nonNull(offence.getVehicleRelatedOffence().getVehicleRegistrationMark())) {
            vehicleRegistrationMark = offence.getVehicleRelatedOffence().getVehicleRegistrationMark();
        }

        final OffenceReferenceData offenceReferenceData = getOffenceReferenceData(offence, initiationCode);
        return Offence.offence()
                .withAlcoholRelatedOffence(offence.getAlcoholRelatedOffence())
                .withAppliedCompensation(offence.getAppliedCompensation())
                .withArrestDate(offence.getArrestDate())
                .withBackDuty(offence.getBackDuty())
                .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                .withBackDutyDateTo(offence.getBackDutyDateTo())
                .withChargeDate(offence.getChargeDate())
                .withLaidDate(offence.getLaidDate())
                .withMaxPenalty(offence.getMaxPenalty())
                .withMotReasonId(offence.getMotReasonId())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceCommittedDate(offence.getOffenceCommittedDate())
                .withOffenceCommittedEndDate(offence.getOffenceCommittedEndDate())
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withOffenceId(ofNullable(offence.getOffenceId()).orElse(idGenerator.generateId()))
                .withOffenceLocation(getOffenceLocation(offence.getOffenceLocation(), channel, offenceReferenceData))
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
                .withVehicleRelatedOffence(offence.getVehicleRelatedOffence())
                .withVerdict(offence.getVerdict())
                .build();
    }





    private OffenceReferenceData getOffenceReferenceData(final Offence offence, final String initiationCode) {
        final List<OffenceReferenceData> offencesRefData = referenceDataQueryService.retrieveOffenceData(offence, initiationCode);
        return (offencesRefData != null && !offencesRefData.isEmpty()) ? offencesRefData.get(0) : null;
    }

    // for testing - allows to control ids
    public static class IdGenerator {
        public UUID generateId() {
            return UUID.randomUUID();
        }
    }

}
