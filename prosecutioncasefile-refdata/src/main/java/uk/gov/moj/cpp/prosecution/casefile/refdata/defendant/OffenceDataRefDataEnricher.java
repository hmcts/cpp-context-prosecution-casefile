package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OffenceLocationHelper.getOffenceLocation;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Lists;

public class OffenceDataRefDataEnricher implements DefendantRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    private static final String SOW_REF_VALUE = "MoJ";

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final Map<String, List<OffenceReferenceData>> offenceReferenceDataMap = new HashMap<>();


        for(final DefendantsWithReferenceData defendantsWithReferenceData: defendantsWithReferenceDataList) {

            final Optional<String> sowRef = defendantsWithReferenceData.isCivil() ? Optional.of(SOW_REF_VALUE) : Optional.empty();

            final List<Defendant> defendants = defendantsWithReferenceData.getDefendants();
            final List<Offence> offences = defendants.stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .collect(Collectors.toList());

            final List<OffenceReferenceData> offenceReferenceDataList = new ArrayList<>();

            offences.forEach(offence -> {
                final String key = offence.getOffenceCode();
                List<OffenceReferenceData> offenceReferenceData = offenceReferenceDataMap.get(key);
                if (isNull(offenceReferenceData)) {
                    offenceReferenceData = referenceDataQueryService.retrieveOffenceDataList(Lists.newArrayList(offence.getOffenceCode()), sowRef);
                    offenceReferenceDataMap.put(offence.getOffenceCode(), offenceReferenceData);
                }

                if (!isOffenceRefDataExists(offenceReferenceDataList, offence.getOffenceCode())) {
                    offenceReferenceDataList.addAll(offenceReferenceData);
                }
            });

            final List<Defendant> newDefendants = new ArrayList<>();
            for (final Defendant defendant : defendants) {
                final List<Offence> offencesFromList = defendant
                        .getOffences()
                        .stream()
                        .map(offence -> this.createOffenseWithCustomOffenceLocation(offence, defendantsWithReferenceData))
                        .collect(Collectors.toList());

                newDefendants.add(createDefendantWithOffences(defendant, offencesFromList));

            }
            defendantsWithReferenceData.setDefendants(newDefendants);
            defendantsWithReferenceData.getReferenceDataVO().setOffenceReferenceData(offenceReferenceDataList);

        }

    }

    private Defendant createDefendantWithOffences(final Defendant defendant, final List<Offence> offences){

        return Defendant.defendant()
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
                .withInitiationCode(defendant.getInitiationCode())
                .withLanguageRequirement(defendant.getLanguageRequirement())
                .withNumPreviousConvictions(defendant.getNumPreviousConvictions())
                .withOffences(offences) // Keeping the modification, but in the same sequence
                .withOrganisationName(defendant.getOrganisationName())
                .withPncIdentifier(defendant.getPncIdentifier())
                .withPostingDate(defendant.getPostingDate())
                .withProsecutorDefendantReference(defendant.getProsecutorDefendantReference())
                .withSpecificRequirements(defendant.getSpecificRequirements())
                .withTelephoneNumberBusiness(defendant.getTelephoneNumberBusiness())
                .withCustodyTimeLimit(defendant.getCustodyTimeLimit())
                .build();

    }

    private Offence createOffenseWithCustomOffenceLocation(final Offence offence, DefendantsWithReferenceData enrichedDefendants) {

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
                .withOffenceCommittedDate(offence.getOffenceCommittedDate())
                .withOffenceCommittedEndDate(offence.getOffenceCommittedEndDate())
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withOffenceId(offence.getOffenceId())
                .withOffenceLocation(getOffenceLocation(offence, enrichedDefendants.getProsecutionAuthorityShortName()))
                .withOffenceSequenceNumber(offence.getOffenceSequenceNumber())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOffenceWording(offence.getOffenceWording())
                .withOffenceWordingWelsh(offence.getOffenceWordingWelsh())
                .withOtherPartyVictim(offence.getOtherPartyVictim())
                .withPlea(offence.getPlea())
                .withProsecutorOfferAOCP(offence.getProsecutorOfferAOCP())
                .withReferenceData(offence.getReferenceData())
                .withStatementOfFacts(offence.getStatementOfFacts())
                .withStatementOfFactsWelsh(offence.getStatementOfFactsWelsh())
                .withVehicleMake(offence.getVehicleMake())
                .withVehicleRegistrationMark(offence.getVehicleRegistrationMark())
                .withVehicleRelatedOffence(offence.getVehicleRelatedOffence())
                .withVerdict(offence.getVerdict())
                .withConvictingCourtCode(offence.getConvictingCourtCode())
                .build();
    }

    private boolean isOffenceRefDataExists(final List<OffenceReferenceData> offenceReferenceDataList, final String offenceCode) {
        return offenceReferenceDataList.stream().anyMatch(offenceReferenceData -> offenceReferenceData.getCjsOffenceCode().equals(offenceCode));
    }

}