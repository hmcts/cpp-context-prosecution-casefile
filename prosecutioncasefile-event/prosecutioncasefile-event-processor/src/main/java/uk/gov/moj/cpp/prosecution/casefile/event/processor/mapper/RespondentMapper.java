package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.MasterDefendantMapper.convertMasterDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationMapper.convertApplicantOrganisation;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationPersonMapper.convertAssociatedPerson;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.PersonMapper.convertPerson;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RespondentMapper {

    public static final Function<List<Respondent>, List<CourtApplicationParty>> convertRespondent
            = sourceRespondents -> ofNullable(sourceRespondents)
            .map(respondents -> respondents.stream()
                    .map(sourceRespondent -> CourtApplicationParty.courtApplicationParty()
                            .withMasterDefendant(convertMasterDefendant.apply(sourceRespondent))
                            .withId(sourceRespondent.getId())
                            .withPersonDetails(getPersonDetails(sourceRespondent))
                            .withOrganisation(getOrganisation(sourceRespondent))
                            .withOrganisationPersons(convertAssociatedPerson.apply(sourceRespondent.getOrganisationPersons()))
                            .withRepresentationOrganisation(convertApplicantOrganisation.apply(sourceRespondent.getRepresentedOrganisation()))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .collect(Collectors.toList())
            )
            .orElse(null);

    private RespondentMapper() {
    }

    private static Organisation getOrganisation(final Respondent sourceRespondent) {
        if(toBoolean(sourceRespondent.getIsDefendantMatched())){//if it is matched with a defendant, only masterDefendant should be there
            return null;
        }
        return convertApplicantOrganisation.apply(sourceRespondent.getOrganisation());
    }

    private static Person getPersonDetails(final Respondent sourceRespondent) {
        if(toBoolean(sourceRespondent.getIsDefendantMatched())){//if it is matched with a defendant, only masterDefendant should be there
            return null;
        }
        return convertPerson.apply(sourceRespondent.getPersonDetails());
    }

}
