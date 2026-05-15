package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail;
import uk.gov.moj.cpp.prosecution.casefile.query.api.utils.OffenceHelper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDetailConverterTest {

    @InjectMocks
    private CaseDetailConverter caseDetailConverter;

    @Mock
    private ReferenceOffencesDataService referenceOffencesDataService;

    @Mock
    private OffenceHelper offenceHelper;

    @Mock
    private Requester requester;

    final String offenceCode = "OF611";
    final String offenceStartDate = "2010-01-01";

    final JsonEnvelope request = envelope()
            .with(metadataWithRandomUUID("prosecutioncasefile.query.case-for-citizen")).build();

    final JsonObject offenceReferenceData = buildOffenceReferenceData();


    private JsonObject buildOffenceReferenceData() {
        return JsonObjects.createObjectBuilder()
                .add("modeOfTrial", "SNONIMP1")
                .build();
    }

    @Test
    public void shouldReturnEmptyCaseDetailListWhenNoMatchingDefendants() {
        List<CaseDetail> caseDetailList = caseDetailConverter.convert(null, requester, getStubProsecutionCase(randomUUID()), emptyList());
        assertThat(caseDetailList.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnCaseDetailListWhenMatchingDefendants() {

        when(referenceOffencesDataService.getOffenceReferenceData(request, requester, offenceCode, offenceStartDate)).thenReturn(offenceReferenceData);
        when(offenceHelper.isOffenceSummaryType(offenceReferenceData)).thenReturn(false);

        UUID caseId = randomUUID();
        List<CaseDetail> caseDetailList = caseDetailConverter.convert(request, requester, getStubProsecutionCase(caseId),
                singletonList(Defendant.defendant().withId(randomUUID())
                        .withOffences(Arrays.asList(Offence.offence()
                                        .withId(randomUUID())
                                        .withOffenceTitle("offenceTitle1")
                                        .withOffenceCode(offenceCode)
                                        .withStartDate(offenceStartDate)
                                        .build(),
                                Offence.offence()
                                        .withId(randomUUID())
                                        .withOffenceTitle("offenceTitle2")
                                        .withOffenceCode(offenceCode)
                                        .withStartDate(offenceStartDate)
                                        .build()))
                        .build()));
        assertThat(caseDetailList.size(), is(1));
        assertThat(caseDetailList.get(0).getId(), is(caseId.toString()));
        assertThat(caseDetailList.get(0).getDefendant().getOffences().get(0).getImprisonable(), is(false));
        assertThat(caseDetailList.get(0).getDefendant().getOffences().get(1).getImprisonable(), is(false));
    }

    private ProsecutionCase getStubProsecutionCase(UUID caseId) {
        return ProsecutionCase.prosecutionCase().withId(caseId).build();
    }

}