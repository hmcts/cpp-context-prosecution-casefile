package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class GroupCasesProsecutorReferenceDataEnricherTest {

    private static final String ORIGINATING_ORGANISATION = "ORIGINATING_ORGANISATION";
    private static final UUID AUTHORITY_ID = randomUUID();

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private GroupCasesProsecutorReferenceDataEnricher groupCasesProsecutorReferenceDataEnricher;

    @Test
    public void shouldPopulateProsecutorWhenOuCodeIsNotNull() {

        when(referenceDataQueryService.retrieveProsecutors(ORIGINATING_ORGANISATION)).thenReturn(getMockProsecutionRefData());

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(true), getMockProsecutionWithReferenceData(true));
        groupCasesProsecutorReferenceDataEnricher.enrich(prosecutionWithReferenceDataList);

        verify(referenceDataQueryService, times(1)).retrieveProsecutors(ORIGINATING_ORGANISATION);
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));
    }

    @Test
    public void shouldPopulateCivilProsecutor() {

        when(referenceDataQueryService.getProsecutorById(any(UUID.class))).thenReturn(getMockProsecutionRefData());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithCivilAuthority();
        groupCasesProsecutorReferenceDataEnricher.enrich(prosecutionWithReferenceData);

        verify(referenceDataQueryService, times(1)).getProsecutorById(any(UUID.class));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData() , notNullValue());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

    }

    @Test
    public void shouldPopulateProsecutorWhenOuCodeIsAvailable() {

        when(referenceDataQueryService.getProsecutorById(AUTHORITY_ID)).thenReturn(getMockProsecutionRefData());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockProsecutionWithReferenceData(false);
        groupCasesProsecutorReferenceDataEnricher.enrich(prosecutionWithReferenceData);

        verify(referenceDataQueryService, times(1)).getProsecutorById(AUTHORITY_ID);
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), notNullValue());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData(), isA(ProsecutorsReferenceData.class));

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

        final Prosecutor.Builder prosecutorBuilder = Prosecutor.prosecutor();
        if (withOuCode) {
            prosecutorBuilder.withProsecutingAuthority(ORIGINATING_ORGANISATION);
        } else {
            prosecutorBuilder.withProsecutionAuthorityId(AUTHORITY_ID);
        }

        return new ProsecutionWithReferenceData(
                Prosecution.prosecution()
                        .withCaseDetails(
                                CaseDetails.caseDetails()
                                        .withProsecutor(prosecutorBuilder.build())
                                .build())
                .build());
    }

    private ProsecutionWithReferenceData getProsecutionWithCivilAuthority() {

        return new ProsecutionWithReferenceData(Prosecution.prosecution()
                .withCaseDetails(
                        CaseDetails.caseDetails()
                                .withProsecutor(
                                        Prosecutor.prosecutor()
                                                .withProsecutionAuthorityId(randomUUID())
                                                .withProsecutingAuthority("THREE_RIVER")
                                                .build()
                                )
                                .build()
                )
                .build());
    }

}
