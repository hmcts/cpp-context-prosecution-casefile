package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static java.util.Optional.ofNullable;
import static java.util.Objects.nonNull;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.IndividualAliasDetail;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OrganisationInformationDetails;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

@SuppressWarnings({"pmd:NullAssignment"})
public class DefendantToDefendantDetails implements Converter<Defendant, DefendantDetails> {

    @Inject
    private PersonalInformationToPersonalInformationDetails personalInformationToPersonalInformationDetails;

    @Inject
    private SelfDefinedInformationToSelfDefinedInformationDetails selfDefinedInformationToSelfDefinedInformationDetails;

    @Inject
    private OffenceToOffenceDetails offenceToOffenceDetails;

    @Inject
    private IndividualAliasToEntity individualAliasToEntity;

    @Inject
    private AddressToAddressDetails addressToAddressDetails;

    @Override
    @SuppressWarnings("squid:S1135")
    public DefendantDetails convert(final Defendant defendant) {
        if (defendant.getIndividual() != null) {
            return getIndividualDefendantDetails(defendant);
        } else {
            return getCorporateDefendantDetails(defendant);
        }
    }

    private DefendantDetails getIndividualDefendantDetails(final Defendant defendant) {
        return new DefendantDetails(
                defendant.getId(),
                defendant.getAsn(),
                ofNullable(defendant.getDocumentationLanguage()).orElse(null),
                ofNullable(defendant.getHearingLanguage()).orElse(null),
                defendant.getLanguageRequirement(),
                defendant.getSpecificRequirements(),
                defendant.getNumPreviousConvictions(),
                defendant.getPostingDate(),
                defendant.getIndividual().getDriverNumber(),
                defendant.getIndividual().getNationalInsuranceNumber(),
                defendant.getProsecutorDefendantReference(),
                defendant.getAppliedProsecutorCosts(),
                personalInformationToPersonalInformationDetails.convert(defendant.getIndividual().getPersonalInformation()),
                selfDefinedInformationToSelfDefinedInformationDetails.convert(defendant.getIndividual().getSelfDefinedInformation()),
                defendant.getOffences().stream().map(offence -> offenceToOffenceDetails.convert(offence)).collect(Collectors.toSet()),
                convertAliases(defendant.getIndividualAliases()),
                null,
                defendant.getInitiationCode()
        );
    }

    private List<IndividualAliasDetail> convertAliases(final List<IndividualAlias> individualAliases) {
        return ofNullable(individualAliases).map(aliases ->
                aliases.stream()
                        .map(individualAliasToEntity::convert)
                        .collect(Collectors.toList()))
                .orElse(null);
    }

    /**
     * NOTE : This does not correctly populate corporate values. This will be taken care as a part
     * of Error handling/UI story. This needs to be refactored.
     */
    private DefendantDetails getCorporateDefendantDetails(final Defendant defendant) {
        return new DefendantDetails(
                defendant.getId(),
                defendant.getAsn(),
                ofNullable(defendant.getDocumentationLanguage()).orElse(null),
                ofNullable(defendant.getHearingLanguage()).orElse(null),
                defendant.getLanguageRequirement(),
                defendant.getSpecificRequirements(),
                defendant.getNumPreviousConvictions(),
                defendant.getPostingDate(),
                null,
                null,
                defendant.getProsecutorDefendantReference(),
                defendant.getAppliedProsecutorCosts(),
                null,
                null,
                defendant.getOffences().stream().map(offence -> offenceToOffenceDetails.convert(offence)).collect(Collectors.toSet()),
                null,
                new OrganisationInformationDetails(defendant.getOrganisationName(),
                        nonNull(defendant.getAddress()) ? addressToAddressDetails.convert(defendant.getAddress()) : null),
                defendant.getInitiationCode());
    }
}
