package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.ProsecutorRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutorRefDataEnricherTest {

    private static final String ORIGINATING_ORGANISATION = "ORIGINATING_ORGANISATION";
    private static final UUID AUTHORITY_ID = randomUUID();
    @Mock
    private Metadata metadata;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private ProsecutorRefDataEnricher prosecutorRefDataEnricher;

    @Test
    public void shouldPopulateProsecutorWhenOuCodeIsNotNull() {

        when(referenceDataQueryService.retrieveProsecutors(ORIGINATING_ORGANISATION)).thenReturn(getMockProsecutionRefData());

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(true),
                getMockProsecutionWithReferenceData(true));

        prosecutorRefDataEnricher.enrich(prosecutionWithReferenceDataList);
        assertNotNull(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

        assertNotNull(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));
        verify(referenceDataQueryService, times(1)).retrieveProsecutors(ORIGINATING_ORGANISATION);
    }

    @Test
    public void testShouldPopulateNspProsecutor() {

        when(referenceDataQueryService.getProsecutorById(any(UUID.class))).thenReturn(getMockProsecutionRefData());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockNspProsecutionWithReferenceData();
        prosecutorRefDataEnricher.enrich(prosecutionWithReferenceData);
        assertNotNull(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));
        verify(referenceDataQueryService).getProsecutorById(any(UUID.class));
    }

    @Test
    public void shouldPopulateProsecutorWhenOuCodeIsNull() {
        when(referenceDataQueryService.getProsecutorById(AUTHORITY_ID)).thenReturn(getMockProsecutionRefData());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockProsecutionWithReferenceData(false);
        prosecutorRefDataEnricher.enrich(prosecutionWithReferenceData);
        assertNotNull(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));
        verify(referenceDataQueryService).getProsecutorById(AUTHORITY_ID);
    }


    private ProsecutorsReferenceData getMockProsecutionRefData() {
        return ProsecutorsReferenceData.prosecutorsReferenceData()
                .withFullName("Blake Austin")
                .withShortName("Blake")
                .withMajorCreditorCode("1L")
                .withSequenceNumber(1)
                .withId(randomUUID())
                .withContactEmailAddress("contact@cpp.co.uk")
                .build();
    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData(boolean withOuCode) {

        Prosecutor.Builder prosecutorBuilder = Prosecutor.prosecutor();
        if (withOuCode) {
            prosecutorBuilder.withProsecutingAuthority(ORIGINATING_ORGANISATION);
        } else {
            prosecutorBuilder.withProsecutionAuthorityId(AUTHORITY_ID);
        }
        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withProsecutor(prosecutorBuilder.build())
                .build();
        final Prosecution prosecution = Prosecution.prosecution().withCaseDetails(caseDetails).build();

        return new ProsecutionWithReferenceData(prosecution);
    }

    private ProsecutionWithReferenceData getMockNspProsecutionWithReferenceData() {

        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withProsecutor(Prosecutor.prosecutor().withProsecutionAuthorityId(randomUUID()).withProsecutingAuthority("NSP-ORG").build())
                .build();
        final Prosecution prosecution = Prosecution.prosecution().withCaseDetails(caseDetails).build();

        return new ProsecutionWithReferenceData(prosecution);
    }

}
