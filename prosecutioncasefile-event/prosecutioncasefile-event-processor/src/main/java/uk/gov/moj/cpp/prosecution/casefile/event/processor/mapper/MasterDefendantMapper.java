package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationMapper.convertApplicantOrganisation;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationPersonMapper.convertAssociatedPerson;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.PersonMapper.convertPerson;

import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;

import java.util.UUID;
import java.util.function.Function;

public class MasterDefendantMapper {


    public static final Function<Respondent, uk.gov.justice.core.courts.MasterDefendant> convertMasterDefendant
            = respondent -> ofNullable(respondent)
            .filter(resp -> toBoolean(resp.getIsDefendantMatched()))
            .map(person -> uk.gov.justice.core.courts.MasterDefendant.masterDefendant()
                    .withPersonDefendant(getPersonDefendant(respondent))
                    .withAssociatedPersons(convertAssociatedPerson.apply(respondent.getOrganisationPersons()))
                    .withLegalEntityDefendant(getLegalEntityDefendant(respondent))
                    .withMasterDefendantId(respondent.getDefendantId())
                    .withCpsDefendantId(ofNullable(respondent.getCpsDefendantId()).map(UUID::fromString).orElse(null))
                    .withDefendantCase(singletonList(DefendantCase.defendantCase()
                            .withDefendantId(respondent.getDefendantId())
                            .withCaseId(respondent.getCaseId())
                            .build()))
                    .build()).orElse(null);

    private MasterDefendantMapper() {
    }

    private static PersonDefendant getPersonDefendant(final Respondent respondent) {
        if (isNull(respondent.getPersonDetails())) {
            return null;
        }
        return PersonDefendant.personDefendant()
                .withArrestSummonsNumber(respondent.getAsn())
                .withPersonDetails(convertPerson.apply(respondent.getPersonDetails()))
                .build();
    }

    private static LegalEntityDefendant getLegalEntityDefendant(final Respondent respondent) {
        if (isNull(respondent.getOrganisation())) {
            return null;
        }
        return LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(convertApplicantOrganisation.apply(respondent.getOrganisation()))
                .build();
    }

}
