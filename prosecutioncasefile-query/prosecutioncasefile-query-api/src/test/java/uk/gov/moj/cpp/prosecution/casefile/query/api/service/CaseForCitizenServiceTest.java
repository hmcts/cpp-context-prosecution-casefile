package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.CaseForCitizenService.DOB_DATE_PATTERN;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.CaseForCitizenService.SJP_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.LEGAL_ENTITY;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.DefendantType.PERSON;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.INITIATION_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.ALREADY_PLEADED;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.NO_MATCH_FOUND;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.OUT_OF_TIME;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetailWithReason.TOO_MANY_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.PersonalDetails.personalDetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Address;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDefendantHearings;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.CaseDetail;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.LegalEntityDefendant;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.Offence;
import uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo.PersonalDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class CaseForCitizenServiceTest {

    private static final String DOB_VALUE_STR = "1990-03-03";

    private static final String urn = randomAlphanumeric(10);
    private static final String id = randomAlphanumeric(10);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Mock
    private SjpService sjpService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope query;

    @Mock
    private Requester requester;

    @Mock
    private JsonObject sjpResponse;

    @Mock
    private CaseDefendantHearings caseDefendantHearings;

    @InjectMocks
    private CaseForCitizenService caseForCitizenService;

    private final String caseUrn = randomAlphanumeric(10);
    private final String postcode = randomAlphanumeric(10);
    private final LocalDate dob = LocalDate.parse(DOB_VALUE_STR, ofPattern(DOB_DATE_PATTERN));
    private final String caseId = randomUUID().toString();

    @BeforeEach
    public void setup() {
        sjpResponse = setNewSJPResponse();
    }

    private JsonObject setNewSJPResponse() {
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add("urn", caseUrn);
        builder.add("id", caseId);
        builder.add("costs", 22.4);
        builder.add("aocpVictimSurcharge", 345);
        builder.add("aocpTotalCost", 123);
        builder.add("aocpEligible", true);
        builder.add("defendant", buildDefendant());
        return builder.build();

    }

    private JsonObject buildDefendant() {
        Address address = new Address("address1", "address2", "address3", "address4", "address5", "postcode");
        ContactDetails contactDetails = new ContactDetails("test@email.com", "home", "work", "mobile");
        PersonalDetails personDetails = new PersonalDetails("firstName", "lastName", DOB_VALUE_STR, address, contactDetails,
                "nationalInsuranceNumber", "driverNumber", "driverLicenceDetails");
        LegalEntityDefendant legalEntityDefendant = new LegalEntityDefendant("name", address, contactDetails, "incorporationNumber");
        Offence offence = new Offence("id", "plea", "title", "titleWelsh", "legislation",
                "legislationWelsh", "wording", "wordingWelsh", true, false,
                true, true, null, null);
        List<Offence> offences = new ArrayList<>();
        offences.add(offence);
        final JsonArrayBuilder offencesArrayBuilder = createArrayBuilder();
        offencesArrayBuilder.add(objectToJsonObjectConverter.convert(offence));
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add("id", id);
        builder.add("personDetails", objectToJsonObjectConverter.convert(personDetails));
        builder.add("legalEntityDefendant", objectToJsonObjectConverter.convert(legalEntityDefendant));
        builder.add("offences", offencesArrayBuilder);
        return builder.build();
    }

    @Test
    public void shouldGetCaseDetailWithDefendantForPersonWhenDefendantMatchFromSjpAndNoCaseIdInProgression() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(sjpResponse);
        when(jsonObjectToObjectConverter.convert(sjpResponse, CaseDetail.class)).thenReturn(getCaseDetail(DOB_VALUE_STR, false, SJP_TYPE));
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(empty());

        JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getString("type"), is(SJP_TYPE));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is(INITIATION_CODE));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(true));
        assertThat(caseWithDefendantJson.getBoolean("aocpEligible"), is(true));
        verify(sjpService).findCase(query, requester, caseUrn, postcode);
    }

    @Test
    public void shouldGetCaseDetailWithDefendantForPersonFromProgressionWhenDefendantNotMatchInSjp() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(sjpResponse);
        when(jsonObjectToObjectConverter.convert(sjpResponse, CaseDetail.class)).thenReturn(getCaseDetail("1980-01-01", false, SJP_TYPE));

        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));

        final CaseDetail caseDetailFromProgression = getCaseDetail(DOB_VALUE_STR, false, null);
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();
        casesDetailMatched.add(caseDetailFromProgression);
        when(progressionService.getCaseDefendantHearings(query, requester, caseId, caseDetailFromProgression.getDefendant().getId())).thenReturn(caseDefendantHearings);
        when(caseDefendantHearings.isEarliestHearingDayInFuture()).thenReturn(true);
        when(progressionService.getCaseDetailWithDefendantForPerson(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.get("type"), is(nullValue()));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is("P"));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(true));
        verify(sjpService).findCase(query, requester, caseUrn, postcode);
    }

    @Test
    public void shouldGetMatchAgainstDOBWhenCaseDetailWithSingleDefendant() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(null);
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));

        final CaseDetail caseDetailFromProgression = getCaseDetail(DOB_VALUE_STR, false, null);
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();
        casesDetailMatched.add(caseDetailFromProgression);
        when(progressionService.getCaseDefendantHearings(query, requester, caseId, caseDetailFromProgression.getDefendant().getId())).thenReturn(caseDefendantHearings);
        when(caseDefendantHearings.isEarliestHearingDayInFuture()).thenReturn(true);
        when(progressionService.getCaseDetailWithDefendantForPerson(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob.plusDays(10));

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is("P"));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(true));
        verify(sjpService).findCase(query, requester, caseUrn, postcode);
    }

    @Test
    public void shouldNotGetCaseDetailDefendantForPersonFromProgressionWhenDefendantHearingDayInPast() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(sjpResponse);
        when(jsonObjectToObjectConverter.convert(sjpResponse, CaseDetail.class)).thenReturn(getCaseDetail("1980-01-01", false, SJP_TYPE));

        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));

        final CaseDetail caseDetailFromProgression = getCaseDetail(DOB_VALUE_STR, false, null);
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();
        casesDetailMatched.add(caseDetailFromProgression);
        when(progressionService.getCaseDefendantHearings(query, requester, caseId, caseDetailFromProgression.getDefendant().getId())).thenReturn(caseDefendantHearings);
        when(caseDefendantHearings.isEarliestHearingDayInThePast()).thenReturn(true);
        when(progressionService.getCaseDetailWithDefendantForPerson(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is("P"));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(OUT_OF_TIME));
        verify(sjpService).findCase(query, requester, caseUrn, postcode);
    }

    @Test
    public void shouldNoMatchDefendantForPersonFromProgressionOrSjp() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(null);
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));
        when(progressionService.getCaseDetailWithDefendantForPerson(query, requester, caseId, postcode)).thenReturn(emptyList());

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(NO_MATCH_FOUND));
    }

    @Test
    public void shouldTooManyDefendantsForPersonMatchFoundInProgressionAndSjp() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(sjpResponse);
        when(jsonObjectToObjectConverter.convert(sjpResponse, CaseDetail.class)).thenReturn(getCaseDetail(DOB_VALUE_STR, false, SJP_TYPE));

        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseUrn));
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();
        casesDetailMatched.add(getCaseDetail(DOB_VALUE_STR, false, null));
        when(progressionService.getCaseDetailWithDefendantForPerson(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(ALREADY_PLEADED));
    }

    @Test
    public void shouldGetCaseDetailWithDefendantAlreadyPleadedForPerson() {
        when(sjpService.findCase(query, requester, caseUrn, postcode)).thenReturn(sjpResponse);
        when(jsonObjectToObjectConverter.convert(sjpResponse, CaseDetail.class)).thenReturn(getCaseDetail("1980-01-01", false, SJP_TYPE));

        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();

        casesDetailMatched.add(getCaseDetail(DOB_VALUE_STR, true, null));
        when(progressionService.getCaseDetailWithDefendantForPerson(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, PERSON, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is("P"));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(ALREADY_PLEADED));
    }

    @Test
    public void shouldGetCaseDetailWithDefendantAlreadyPleadedForLegalEntity() {
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();

        casesDetailMatched.add(getCaseDetail(DOB_VALUE_STR, true, null));
        when(progressionService.getCaseDetailWithDefendantForLegalEntity(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, LEGAL_ENTITY, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is("P"));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(ALREADY_PLEADED));
    }

    @Test
    public void shouldGetCaseDetailWithDefendantForLegalEntityFromProgression() {
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));
        final CaseDetail caseDetailMatch = getCaseDetail(DOB_VALUE_STR, false, null);
        when(progressionService.getCaseDetailWithDefendantForLegalEntity(query, requester, caseId, postcode)).thenReturn(singletonList(caseDetailMatch));
        when(progressionService.getCaseDefendantHearings(query, requester, caseId, caseDetailMatch.getDefendant().getId())).thenReturn(caseDefendantHearings);
        when(caseDefendantHearings.isEarliestHearingDayInFuture()).thenReturn(true);

        JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, LEGAL_ENTITY, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(true));
    }

    @Test
    public void shouldGetNoMatchingCaseDetailForLegalEntityFromProgression() {
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));
        when(progressionService.getCaseDetailWithDefendantForLegalEntity(query, requester, caseId, postcode)).thenReturn(emptyList());

        JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, LEGAL_ENTITY, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(NO_MATCH_FOUND));
    }

    @Test
    public void shouldGetTooManyDefendantsForLegalEntityFromProgression() {
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));
        when(progressionService.getCaseDetailWithDefendantForLegalEntity(query, requester, caseId, postcode)).thenReturn(Arrays.asList(getCaseDetail(DOB_VALUE_STR, false, null), getCaseDetail(DOB_VALUE_STR, false, null)));

        JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, LEGAL_ENTITY, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(TOO_MANY_DEFENDANTS));
    }

    @Test
    public void shouldNotGetCaseDetailDefendantForLegalEntityFromProgressionWhenDefendantHearingDayInPast() {
        when(progressionService.getCaseId(query, requester, caseUrn)).thenReturn(Optional.of(caseId));

        final CaseDetail caseDetailFromProgression = getCaseDetail(DOB_VALUE_STR, false, null);
        final List<CaseDetail> casesDetailMatched = new ArrayList<>();
        casesDetailMatched.add(caseDetailFromProgression);
        when(progressionService.getCaseDefendantHearings(query, requester, caseId, caseDetailFromProgression.getDefendant().getId())).thenReturn(caseDefendantHearings);
        when(caseDefendantHearings.isEarliestHearingDayInThePast()).thenReturn(true);
        when(progressionService.getCaseDetailWithDefendantForLegalEntity(query, requester, caseId, postcode)).thenReturn(casesDetailMatched);

        final JsonObject caseWithDefendantJson = caseForCitizenService.getCaseWithDefendant(query, requester, LEGAL_ENTITY, caseUrn, postcode, dob);

        assertThat(caseWithDefendantJson.getString("urn"), is(caseUrn));
        assertThat(caseWithDefendantJson.getString("id"), is(caseId));
        assertThat(caseWithDefendantJson.getString("initiationCode"), is("P"));
        assertThat(caseWithDefendantJson.getBoolean("canContinue"), is(false));
        assertThat(caseWithDefendantJson.getString("cantContinueReason"), is(OUT_OF_TIME));
    }

    private CaseDetail getCaseDetail(final String dob, final boolean onlinePlea, final String type) {
        return CaseDetail.caseDetail().withUrn(caseUrn)
                .withId(caseId).withInitiationCode("P")
                .withType(type)
                .withDefendant(defendant().withId(randomUUID().toString()).withPersonalDetails(personalDetails().withDateOfBirth(dob).build()).withOffences(singletonList(Offence.offence().withOnlinePleaReceived(onlinePlea).build())).build())
                .build();
    }
}