package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OrganisationUnitWithCourtroomRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrganisationUnitWithCourtroomRefDataEnricherTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private OrganisationUnitWithCourtroomRefDataEnricher organisationUnitWithCourtroomRefDataEnricher;

    @Test
    public void testShouldPopulateOrganisationUnitWithCourtroomsWhenOuCodeFound() {
        String ouCode = "ouCode1";
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(ouCode)).thenReturn(getMockOrganisationUnitsWithCourtroom(ouCode));

        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = asList(getMockDefendantsWithReferenceData(ouCode), getMockDefendantsWithReferenceData(ouCode));

        organisationUnitWithCourtroomRefDataEnricher.enrich(defendantsWithReferenceDataList);
        assertNotNull(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData());
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent(), is(true));
        assertThat(defendantsWithReferenceDataList.get(0).getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().get(), isA(OrganisationUnitWithCourtroomReferenceData.class));

        assertNotNull(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData());
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent(), is(true));
        assertThat(defendantsWithReferenceDataList.get(1).getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().get(), isA(OrganisationUnitWithCourtroomReferenceData.class));
        verify(referenceDataQueryService, times(1)).retrieveOrganisationUnitWithCourtroom(ouCode);
    }

    @Test
    public void testShouldNotPopulateOrganisationUnitWithCourtroomsWhenOuCodeNotExistInTheRequest() {
        String ouCode = null;

        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(ouCode);
        organisationUnitWithCourtroomRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent(), is(false));
        verifyNoInteractions(referenceDataQueryService);

    }


    @Test
    public void testShouldNotPopulateOrganisationUnitWithCourtroomsWhenOuCodeLengthIsNotValid() {
        String ouCode = "ouCode";
        final DefendantsWithReferenceData defendantsWithReferenceData = getMockDefendantsWithReferenceData(ouCode);
        organisationUnitWithCourtroomRefDataEnricher.enrich(defendantsWithReferenceData);
        assertNotNull(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData());
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent(), is(false));
        verifyNoInteractions(referenceDataQueryService);

    }

    private DefendantsWithReferenceData getMockDefendantsWithReferenceData(final String ouCode) {

        Defendant defendant = defendant()
                .withInitialHearing(initialHearing()
                        .withCourtHearingLocation(ouCode)
                        .build())
                .build();

        return new DefendantsWithReferenceData(singletonList(defendant));
    }

    private Optional<OrganisationUnitWithCourtroomReferenceData> getMockOrganisationUnitsWithCourtroom(String ouCode) {
        return of(organisationUnitWithCourtroomReferenceData()
                .withOucode(ouCode)
                .withId(randomUUID().toString())
                .build());
    }

}
