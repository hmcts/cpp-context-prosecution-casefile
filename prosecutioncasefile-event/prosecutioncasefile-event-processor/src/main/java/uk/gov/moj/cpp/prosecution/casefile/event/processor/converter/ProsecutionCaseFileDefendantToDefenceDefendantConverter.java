package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceDefendant.defenceDefendant;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Organisation;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

public class ProsecutionCaseFileDefendantToDefenceDefendantConverter implements Converter<List<Defendant>, List<DefenceDefendant>> {
    @Inject
    private ProsecutionCaseFileOffenceToDefenceOffenceConverter prosecutionCaseFileOffenceToDefenceOffenceConverter;

    @Override
    @SuppressWarnings("squid:S1188")
    public List<DefenceDefendant> convert(final List<Defendant> defendants) {

        return defendants.stream()
                .map(defendant ->
                        defenceDefendant()
                                .withId(defendant.getId())
                                .withAsn(defendant.getAsn())
                                .withProsecutorDefendantReference(defendant.getProsecutorDefendantReference())
                                .withFirstName(getFirstName(defendant))
                                .withLastName(getLastName(defendant))
                                .withOrganisation(defendant.getOrganisationName() != null ? Organisation.organisation()
                                        .withOrganisationName(defendant.getOrganisationName())
                                        .withAliasOrganisationNames(defendant.getAliasForCorporate())
                                        .withCompanyTelephoneNumber(defendant.getTelephoneNumberBusiness())
                                        .build() : null)
                                .withOffences(prosecutionCaseFileOffenceToDefenceOffenceConverter.convert(defendant.getOffences()))
                                .withDateOfBirth(getDateOfBirth(defendant))
                                .build()
                )
                .collect(toList());
    }

    private String getFirstName(final Defendant defendant) {
        return (null != defendant.getIndividual() && null != defendant.getIndividual().getPersonalInformation()) ? defendant.getIndividual().getPersonalInformation().getFirstName() : null;
    }

    private String getLastName(final Defendant defendant) {
        return (null != defendant.getIndividual() && null != defendant.getIndividual().getPersonalInformation()) ? defendant.getIndividual().getPersonalInformation().getLastName() : null;
    }

    private LocalDate getDateOfBirth(final Defendant defendant) {
        return (null != defendant.getIndividual() && null != defendant.getIndividual().getSelfDefinedInformation()) ? defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth() : null;
    }
}