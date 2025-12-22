package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.ProgressionService.CASE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.ProgressionService.PROGRESSION_CASE_DEFENDANT_HEARINGS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.ProgressionService.PROGRESSION_SEARCH_CASES;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.ProgressionService.PROSECUTION_GET_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.ProgressionService.QUERY_PARAM;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.ProgressionService.SEARCH_RESULT;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.DefendantHearings.defendantHearings;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.HearingDay.hearingDay;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDefendantHearings;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.DefendantHearings;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled("HERE!!!")
@ExtendWith(MockitoExtension.class)
public class ProgressionServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private ProgressionService progressionService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Spy
    @InjectMocks
    private CaseDetailConverter caseDetailConverter  = new CaseDetailConverter();

    private final String caseUrn = randomAlphanumeric(10);
    private final String caseId = randomUUID().toString();
    private final String defendantId = randomUUID().toString();
    private final String sittingDay = "2019-05-30T18:32:04.238Z";

    @Test
    public void shouldGetCaseIdByUrn() {
        final Metadata metadata = metadataBuilder().withName(PROGRESSION_SEARCH_CASES).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add(SEARCH_RESULT, createArrayBuilder()
                .add(createObjectBuilder().add(CASE_ID, caseId.toString()).build())
                .build()).build());

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);

        Optional<String> actualCaseId = progressionService.getCaseId(envelope, requester, caseUrn);

        assertThat(actualCaseId.isPresent(), is(true));
        assertThat(actualCaseId.get(), is(caseId.toString()));
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_SEARCH_CASES));
        assertThat(envelopeCaptor.getValue().payload().getString(QUERY_PARAM), is(caseUrn));
    }

    @Test
    public void shouldNotGetCaseIdByUrnWhenSearchResultsEmpty() {
        final Metadata metadata = metadataBuilder().withName(PROGRESSION_SEARCH_CASES).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add(SEARCH_RESULT, createArrayBuilder().build()).build());

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);

        Optional<String> actualCaseId = progressionService.getCaseId(envelope, requester, caseUrn);

        assertThat(actualCaseId.isPresent(), is(false));
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROGRESSION_SEARCH_CASES));
        assertThat(envelopeCaptor.getValue().payload().getString(QUERY_PARAM), is(caseUrn));
    }

    public void findCaseByUrn_shouldThrowException_whenNoResponsePayload() {
        final Metadata metadata = metadataBuilder().withName(PROGRESSION_SEARCH_CASES).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, null);

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);

        Optional<String> caseId = progressionService.getCaseId(envelope, requester, caseUrn);

        assertThat(caseId.isPresent(), is(false));
    }

    @Test
    public void shouldGetProsecutionCase() {
        final Metadata metadata = metadataBuilder().withName(PROSECUTION_GET_CASE).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId.toString()).build()).build());

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class))).thenReturn(prosecutionCase);

        final ProsecutionCase actualProsecutionCase = progressionService.getProsecutionCase(envelope, requester, caseId);

        assertThat(actualProsecutionCase, is(prosecutionCase));
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PROSECUTION_GET_CASE));
        assertThat(envelopeCaptor.getValue().payload().getString(CASE_ID), is(caseId));
    }

    @Test
    public void getProsecutionCase_shouldThrowException_whenNoResponsePayload() {
        final Metadata metadata = metadataBuilder().withName(PROSECUTION_GET_CASE).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, null);

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);

        assertThrows(ProgressionServiceException.class, () -> progressionService.getProsecutionCase(envelope, requester, caseId));
    }

    @Test
    public void shouldGetCaseDetailWithDefendantForPerson() {
        final Metadata metadata = metadataBuilder().withName(PROSECUTION_GET_CASE).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId).build()).build());

        final String postcode = randomAlphanumeric(7);
        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class))).thenReturn(getProsecutionCaseWithDefendants(postcode));

        final List<CaseDetail> actualCaseDetails = progressionService.getCaseDetailWithDefendantForPerson(envelope, requester, caseId, postcode);

        assertThat(actualCaseDetails.isEmpty(), is(false));
        assertThat(actualCaseDetails.get(0).getId(), is(caseId));
        assertThat(actualCaseDetails.get(0).getDefendant().getPersonalDetails().getAddress().getPostcode(), is(postcode));
    }

    @Test
    public void shouldGetNoCaseDetailWithDefendantForPersonWhenPostcodeDoesNotMatch() {
        final Metadata metadata = metadataBuilder().withName(PROSECUTION_GET_CASE).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId).build()).build());

        final String postcode = randomAlphanumeric(7);
        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class))).thenReturn(getProsecutionCaseWithDefendants(postcode));

        final List<CaseDetail> actualCaseDetails = progressionService.getCaseDetailWithDefendantForPerson(envelope, requester, caseId, randomAlphanumeric(7));

        assertThat(actualCaseDetails.isEmpty(), is(true));
    }

    @Test
    public void shouldGetCaseDetailWithDefendantForLegalEntity() {
        final Metadata metadata = metadataBuilder().withName(PROSECUTION_GET_CASE).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId).build()).build());

        final String postcode = randomAlphanumeric(7);
        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class))).thenReturn(getProsecutionCaseWithDefendants(postcode));
        //when(caseDetailConverter.convert(any(Envelope.class), any(Requester.class), any(ProsecutionCase.class),anyList())).thenReturn(null);

        final List<CaseDetail> actualCaseDetails = progressionService.getCaseDetailWithDefendantForLegalEntity(envelope, requester, caseId, postcode);

        assertThat(actualCaseDetails.isEmpty(), is(false));
        assertThat(actualCaseDetails.get(0).getId(), is(caseId));
        assertThat(actualCaseDetails.get(0).getDefendant().getLegalEntityDefendant().getAddress().getPostcode(), is(postcode));
    }

    @Test
    public void shouldGetNoCaseDetailWithDefendantForLegalEntityWhenPostcodeDoesNotMatch() {
        final Metadata metadata = metadataBuilder().withName(PROSECUTION_GET_CASE).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId).build()).build());

        final String postcode = randomAlphanumeric(7);
        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class))).thenReturn(getProsecutionCaseWithDefendants(postcode));

        final List<CaseDetail> actualCaseDetails = progressionService.getCaseDetailWithDefendantForLegalEntity(envelope, requester, caseId, randomAlphanumeric(7));

        assertThat(actualCaseDetails.isEmpty(), is(true));
    }

    @Test
    public void shouldGetCaseDefendantHearingsForCaseDefendant() {
        final Metadata metadata = metadataBuilder().withName(PROGRESSION_CASE_DEFENDANT_HEARINGS).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().build());

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(CaseDefendantHearings.class))).thenReturn(getCaseDefendantHearings(caseId, defendantId, sittingDay));

        CaseDefendantHearings caseDefendantHearings = progressionService.getCaseDefendantHearings(envelope, requester, caseId, defendantId);

        assertThat(caseDefendantHearings.getCaseId(), is(caseId));
        assertThat(caseDefendantHearings.getDefendantId(), is(defendantId));
        assertThat(caseDefendantHearings.getHearings().get(0).getHearingDays().get(0).getSittingDay().toString(), is(sittingDay));
    }

    private CaseDefendantHearings getCaseDefendantHearings(final String caseId, final String defendantId, final String sittingDay) {
        final List<DefendantHearings> defendantHearings = new ArrayList<>();
        defendantHearings.add(defendantHearings()
                .withHearingId(randomUUID().toString())
                .withHearingDays(singletonList(hearingDay()
                        .withSittingDay(ZonedDateTime.parse(sittingDay))
                        .build()))
                .build());
        return new CaseDefendantHearings(caseId, defendantId, defendantHearings);
    }

    private ProsecutionCase getProsecutionCaseWithDefendants(final String postcode) {
        return prosecutionCase()
                .withId(fromString(caseId))
                .withDefendants(singletonList(defendant()
                        .withId(randomUUID())
                        .withLegalEntityDefendant(legalEntityDefendant()
                                .withOrganisation(organisation()
                                        .withAddress(address().withPostcode(postcode).build())
                                        .build())
                                .build())
                        .withPersonDefendant(personDefendant()
                                .withPersonDetails(person()
                                        .withAddress(address().withPostcode(postcode).build())
                                        .build())
                                .build()).build()))
                .build();
    }

}