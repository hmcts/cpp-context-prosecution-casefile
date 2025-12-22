package uk.gov.moj.cpp.prosecutioncasefile.query.view;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.createFirstDefendantCaseDetails;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CountsCasesErrorsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.BusinessErrorDetailsService;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CaseDetailsService;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CountsCasesErrorsService;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCasefileQueryViewTest {

    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_PROSECUTION_CASE_REF = "prosecutionCaseReference";
    private static final String FIELD_REGION = "region";
    private static final String FIELD_CASE_TYPE = "caseType";
    private static final String FIELD_COURT_HOUSE = "courtLocation";

    private static final UUID VALUE_CASE_ID = CASE_ID;

    @Mock
    private CountsCasesErrorsService countsCasesErrorsService;

    @Mock
    private CaseDetailsService caseDetailsService;

    @InjectMocks
    private ProsecutionCasefileQueryView queryView;

    @Mock
    private BusinessErrorDetailsService businessErrorDetailsService;

    @Mock
    private CountsCasesErrorsView countsCasesErrorsView;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Test
    public void shouldGetCaseDetails_whenQueryCaseDetails() {
        final CaseDetailsView caseDetailsView = new CaseDetailsView(createFirstDefendantCaseDetails());
        final JsonEnvelope queryEnvelope = createQueryEnvelope("prosecutioncasefile.query.case");
        when(caseDetailsService.findCase(VALUE_CASE_ID)).thenReturn(caseDetailsView);

        queryView.getCaseDetails(queryEnvelope);
        verify(caseDetailsService).findCase(eq(VALUE_CASE_ID));
    }

    @Test
    public void shouldGetCaseDetails_whenQueryCaseDetailsByProsecutionCaseReference() {
        final CaseDetailsView caseDetailsView = new CaseDetailsView(createFirstDefendantCaseDetails());
        final JsonEnvelope queryEnvelope = createQueryEnvelope("prosecutioncasefile.query.case-by-prosecutionCaseReference");
        when(caseDetailsService.findCaseByProsecutionReferenceId(PROSECUTOR_CASE_REFERENCE)).thenReturn(caseDetailsView);

        queryView.getCaseDetailsByProsecutionCaseReference(queryEnvelope);
        verify(caseDetailsService).findCaseByProsecutionReferenceId(eq(PROSECUTOR_CASE_REFERENCE));
    }

    @Test
    public void shouldGetNoResult_whenQueryCaseDetails() {
        final JsonEnvelope queryEnvelope = createQueryEnvelope("prosecutioncasefile.query.case");
        when(caseDetailsService.findCase(VALUE_CASE_ID)).thenReturn(null);

        queryView.getCaseDetails(queryEnvelope);
        verify(caseDetailsService).findCase(eq(VALUE_CASE_ID));
    }

    @Test
    public void shouldReturnEmptyWhenNoErrors() {
        final JsonEnvelope queryEnvelope = createQueryEnvelope("prosecutioncasefile.query.cases.errors");

        queryView.getErrorDetailsForCases(queryEnvelope);
        verify(businessErrorDetailsService).findAllErrorsByCaseId(eq(VALUE_CASE_ID));
    }

    @Test
    public void shouldGetCountsCasesErrorsCorrectly() {
        final JsonEnvelope queryEnvelope = createQueryEnvelope("prosecutioncasefile.query.cases-errors-count");

        final CountsCasesErrorsView expectedCasesCount = new CountsCasesErrorsView(15, 10);

        queryView.casesErrorsCount(queryEnvelope);

        verify(countsCasesErrorsService).countsCasesErrors(eq(of("London")), eq(of("CourtLocation")), eq(of("CaseType")));
    }

    private JsonEnvelope createQueryEnvelope(final String name) {
        return envelope()
                .with(metadataBuilder()
                        .withId(randomUUID())
                        .withName(name)
                        .withUserId(randomUUID().toString())
                )
                .withPayloadOf(VALUE_CASE_ID.toString(), FIELD_CASE_ID)
                .withPayloadOf(PROSECUTOR_CASE_REFERENCE,FIELD_PROSECUTION_CASE_REF)
                .withPayloadOf("London", FIELD_REGION)
                .withPayloadOf("CourtLocation", FIELD_COURT_HOUSE)
                .withPayloadOf("CaseType", FIELD_CASE_TYPE)
                .build();
    }
}
