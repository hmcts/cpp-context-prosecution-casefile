package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationAcceptedToCourtApplicationProceedingsConverterTest {

    private ApplicationAcceptedToCourtApplicationProceedingsConverter subject;
    private UUID applicationId;
    private UUID boxHearingRequestId;
    private SubmitApplicationAccepted sourceSubmitApplication;
    protected ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @BeforeEach
    public void init() {
        applicationId = randomUUID();
        boxHearingRequestId = randomUUID();
        JsonObject jsonObject = generateSubmitApplicationPayload();
        sourceSubmitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplicationAccepted.class);
        subject = new ApplicationAcceptedToCourtApplicationProceedingsConverter();
    }

    private JsonObject generateSubmitApplicationPayload() {
        String payloadStr = getStringFromResource()
                .replace("%APPLICATION_ID%", applicationId.toString())
                .replace("%BOX_HEARING_ID%", boxHearingRequestId.toString());
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private static String getStringFromResource() {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource("submit-application.json"), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + "submit-application.json");
        }
        return request;
    }

    @Test
    public void shouldConvertSubmitApplicationAcceptedEventToProgressionCommandPayload() {

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);

        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getId(), equalTo(applicationId));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getId(), equalTo(boxHearingRequestId));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationCourtApplicationType() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);

        final uk.gov.justice.core.courts.CourtApplication targetCourtApplication = initiateCourtApplicationProceedings.getCourtApplication();

        assertThat(targetCourtApplication.getType().getCode(), is(sourceCourtApplication.getCourtApplicationType().getCode()));
        assertThat(targetCourtApplication.getType().getCategoryCode(), is(sourceCourtApplication.getCourtApplicationType().getCategoryCode()));
        assertThat(targetCourtApplication.getType().getJurisdiction().toString(), is(sourceCourtApplication.getCourtApplicationType().getJurisdiction().toString()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicationParticulars() {
        CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);
        final uk.gov.justice.core.courts.CourtApplication targetCourtApplication = initiateCourtApplicationProceedings.getCourtApplication();

        assertThat(targetCourtApplication.getApplicationParticulars(), equalTo(sourceCourtApplication.getApplicationParticulars()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicationPayment() {
        CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);
        final uk.gov.justice.core.courts.CourtApplication targetCourtApplication = initiateCourtApplicationProceedings.getCourtApplication();

        assertThat(targetCourtApplication.getCourtApplicationPayment().getPaymentReference(), is(sourceCourtApplication.getCourtApplicationPayment().getPaymentReference()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicationDecisionSoughtByDate() {
        CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);

        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicationReceivedDate(), is(LocalDate.now().toString()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicationDecisionSoughtByDate(), is(sourceCourtApplication.getApplicationDecisionSoughtByDate().toString()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicant() {
        CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);

        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getName(), is(sourceCourtApplication.getApplicant().getOrganisation().getName()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getIncorporationNumber(), is(sourceCourtApplication.getApplicant().getOrganisation().getIncorporationNumber()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getRegisteredCharityNumber(), is(sourceCourtApplication.getApplicant().getOrganisation().getRegisteredCharityNumber()));

        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getAddress().getAddress1(), is(sourceCourtApplication.getApplicant().getOrganisation().getAddress().getAddress1()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getAddress().getAddress2(), is(sourceCourtApplication.getApplicant().getOrganisation().getAddress().getAddress2()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getAddress().getAddress3(), is(sourceCourtApplication.getApplicant().getOrganisation().getAddress().getAddress3()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getAddress().getAddress4(), is(sourceCourtApplication.getApplicant().getOrganisation().getAddress().getAddress4()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getAddress().getAddress5(), is(sourceCourtApplication.getApplicant().getOrganisation().getAddress().getAddress5()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getAddress().getPostcode(), is(sourceCourtApplication.getApplicant().getOrganisation().getAddress().getPostcode()));

        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getContact().getWork(), is(sourceCourtApplication.getApplicant().getOrganisation().getContact().getWork()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getContact().getHome(), is(sourceCourtApplication.getApplicant().getOrganisation().getContact().getHome()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getContact().getMobile(), is(sourceCourtApplication.getApplicant().getOrganisation().getContact().getMobile()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getContact().getPrimaryEmail(), is(sourceCourtApplication.getApplicant().getOrganisation().getContact().getPrimaryEmail()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getContact().getSecondaryEmail(), is(sourceCourtApplication.getApplicant().getOrganisation().getContact().getSecondaryEmail()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisation().getContact().getFax(), is(sourceCourtApplication.getApplicant().getOrganisation().getContact().getFax()));


        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().size(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().size()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getTitle(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getTitle()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getFirstName(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getFirstName()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getMiddleName(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getMiddleName()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getLastName(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getLastName()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getDateOfBirth(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getDateOfBirth().toString()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getGender().toString(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getGender().toString()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getInterpreterLanguageNeeds(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getInterpreterLanguageNeeds()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getDocumentationLanguageNeeds().toString(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getDocumentationLanguageNeeds().toString()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getNationalInsuranceNumber(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getNationalInsuranceNumber()));
        assertThat(initiateCourtApplicationProceedings.getCourtApplication().getApplicant().getOrganisationPersons().get(0).getPerson().getSpecificRequirements(), is(sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getSpecificRequirements()));
    }

    @Test
    public void shouldConvertEventPayloadToBoxHearingRequest() {
        final BoxHearingRequest boxHearingRequestSource = sourceSubmitApplication.getBoxHearingRequest();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = subject.convert(sourceSubmitApplication);

        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getId(), equalTo(boxHearingRequestId));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getApplicationDueDate(), equalTo(boxHearingRequestSource.getApplicationDueDate().toString()));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getJurisdictionType().name(), equalTo(boxHearingRequestSource.getJurisdictionType().name()));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getSendAppointmentLetter(), equalTo(boxHearingRequestSource.getSendAppointmentLetter()));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getCourtCentre().getId(), equalTo(boxHearingRequestSource.getCourtCentre().getId()));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getCourtCentre().getCode(), equalTo(boxHearingRequestSource.getCourtCentre().getCode()));
        assertThat(initiateCourtApplicationProceedings.getBoxHearing().getCourtCentre().getName(), equalTo(boxHearingRequestSource.getCourtCentre().getName()));
    }
}