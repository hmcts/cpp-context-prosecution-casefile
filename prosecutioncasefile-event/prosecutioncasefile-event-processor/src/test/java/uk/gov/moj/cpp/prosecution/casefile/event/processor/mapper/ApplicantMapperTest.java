package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Applicant;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ApplicantMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicantOrganisationPersons() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final Applicant sourceApplicant = sourceCourtApplication.getApplicant();

        final List<AssociatedPerson> sourceAssociatedPersonList = OrganisationPersonMapper.convertAssociatedPerson.apply(sourceApplicant.getOrganisationPersons());
        final Organisation sourceOrganisation = OrganisationMapper.convertApplicantOrganisation.apply(sourceApplicant.getOrganisation());

        final CourtApplicationParty targetCourtApplicationParties = ApplicantMapper.convertApplicant.apply(sourceApplicant);
        assertThat(targetCourtApplicationParties.getOrganisation(), is(sourceOrganisation));
        assertThat(targetCourtApplicationParties.getOrganisationPersons(), is(sourceAssociatedPersonList));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationWhenApplicantIsNull() {
        final CourtApplicationParty targetCourtApplicationParties = ApplicantMapper.convertApplicant.apply(null);
        assertTrue(isNull(targetCourtApplicationParties));
    }
}