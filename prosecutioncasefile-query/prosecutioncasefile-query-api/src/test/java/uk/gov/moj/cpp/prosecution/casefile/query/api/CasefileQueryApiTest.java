package uk.gov.moj.cpp.prosecution.casefile.query.api;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.CasefileQueryApi.DEFENDANT_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.LEGAL_ENTITY;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.PERSON;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.POSTCODE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.URN;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.CaseForCitizenService;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.ProsecutionCasefileQueryView;

import java.time.LocalDate;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CasefileQueryApiTest {

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private ProsecutionCasefileQueryView prosecutionCasefileQueryView;

    @Mock
    private CaseForCitizenService caseForCitizenService;

    @Mock
    private Requester requester;

    @InjectMocks
    private CasefileQueryApi casefileQueryApi;
    @Mock
    private JsonObject jsonResponse;
    private final String caseUrn = randomAlphanumeric(10);
    private final String postcode = randomAlphanumeric(10);
    private static final String DOB = "dob";


    @Test
    public void shouldHandleCaseDetailsQuery() {
        when(prosecutionCasefileQueryView.getCaseDetails(query)).thenReturn(response);
        assertThat(casefileQueryApi.getCaseDetails(query), equalTo(response));

        verify(prosecutionCasefileQueryView).getCaseDetails(query);
    }

    @Test
    public void shouldHandleCaseDetailByProsecutionByReferencesQuery() {
        when(prosecutionCasefileQueryView.getCaseDetailsByProsecutionCaseReference(query)).thenReturn(response);
        assertThat(casefileQueryApi.getCaseByProsecutionCaseReference(query), equalTo(response));

        verify(prosecutionCasefileQueryView).getCaseDetailsByProsecutionCaseReference(query);
    }

    @Test
    public void shouldGetErrorDetailsForCases() {
        when(prosecutionCasefileQueryView.getErrorDetailsForCases(query)).thenReturn(response);
        assertThat(casefileQueryApi.getErrorDetailsForCases(query), equalTo(response));

        verify(prosecutionCasefileQueryView).getErrorDetailsForCases(query);
    }

    @Test
    public void shouldGetQueryCaseErrorsCountActionGroups() {
        when(prosecutionCasefileQueryView.casesErrorsCount(query)).thenReturn(response);
        assertThat(casefileQueryApi.casesErrorsCount(query), equalTo(response));
        verify(prosecutionCasefileQueryView).casesErrorsCount(query);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidDefendantTypeInQueryParams() {
        when(query.payloadAsJsonObject()).thenReturn(getRequestPayload(caseUrn, postcode, "UNKNOWN_TYPE", null));

        assertThrows(IllegalArgumentException.class, () -> casefileQueryApi.findCaseForCitizen(query));
    }

    @Test
    public void shouldThrowExceptionWhenDobFormatIsNotValid() {
        when(query.payloadAsJsonObject()).thenReturn(getRequestPayload(caseUrn, postcode, PERSON.name(), "11-11-2000"));

        assertThrows(BadRequestException.class, () -> casefileQueryApi.findCaseForCitizen(query));
    }

    @Test
    public void shouldThrowExceptionWhenDefendantTypePersonAndNoDob() {
        when(query.payloadAsJsonObject()).thenReturn(getRequestPayload(caseUrn, postcode, PERSON.name(), null));

        assertThrows(BadRequestException.class, () -> casefileQueryApi.findCaseForCitizen(query));
    }

    @Test
    public void shouldFindCaseWhenDefendantTypePerson() {
        when(caseForCitizenService.getCaseWithDefendant(any(), any(), any(), any(), any(), any())).thenReturn(jsonResponse);
        String dateOfBirth = "1990-03-03";
        when(query.payloadAsJsonObject()).thenReturn(getRequestPayload(caseUrn, postcode, PERSON.name(), dateOfBirth));

        final JsonEnvelope caseForCitizenResponse = casefileQueryApi.findCaseForCitizen(query);

        assertThat(caseForCitizenResponse.payloadAsJsonObject(), equalTo(jsonResponse));
        verify(caseForCitizenService).getCaseWithDefendant(eq(query), eq(requester), eq(PERSON), eq(caseUrn), eq(postcode), any(LocalDate.class));
    }

    @Test
    public void shouldFindCaseWhenDefendantTypeLegalEntity() {
        when(caseForCitizenService.getCaseWithDefendant(any(), any(), any(), any(), any(), any())).thenReturn(jsonResponse);
        when(query.payloadAsJsonObject()).thenReturn(getRequestPayload(caseUrn, postcode, LEGAL_ENTITY.name(), null));

        final JsonEnvelope caseForCitizenResponse = casefileQueryApi.findCaseForCitizen(query);

        assertThat(caseForCitizenResponse.payloadAsJsonObject(), equalTo(jsonResponse));
        verify(caseForCitizenService).getCaseWithDefendant(eq(query), eq(requester), eq(LEGAL_ENTITY), eq(caseUrn), eq(postcode), any());
    }

    private JsonObject getRequestPayload(final String caseUrn, final String postcode, final String defendantType, final String dateOfBirth) {
        JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(URN, caseUrn)
                .add(POSTCODE, postcode)
                .add(DEFENDANT_TYPE, defendantType);
        ofNullable(dateOfBirth).ifPresent(dob -> jsonObjectBuilder.add(DOB, dob));
        return jsonObjectBuilder.build();
    }
}
