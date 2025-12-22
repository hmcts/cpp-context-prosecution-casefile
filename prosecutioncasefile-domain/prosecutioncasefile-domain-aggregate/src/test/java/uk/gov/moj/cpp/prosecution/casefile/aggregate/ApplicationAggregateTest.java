package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest.boxHearingRequest;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtCentre.courtCentre;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.APPLICATION_TYPE_NOT_FOUND;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.valueOf;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationAggregateTest {

    private ApplicationAggregate applicationAggregate;

    @Mock
    private SubmitApplicationValidator submitApplicationValidator;

    @Mock
    private AdditionalInformation additionalInformation;

    @BeforeEach
    public void setup() {
        applicationAggregate = new ApplicationAggregate();
    }

    @Test
    public void shouldRaiseInitiateApplicationAcceptedEventWhenThereIsNoValidationError() {
        final UUID courtCentreId = randomUUID();
        final String ouCode = "B13CC00";

        final SubmitApplication submitApplication = SubmitApplication.submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .build())
                .withBoxHearingRequest(boxHearingRequest()
                        .withId(randomUUID())
                        .withCourtCentre(courtCentre()
                                .withId(courtCentreId)
                                .withName("LAVENDER HILL MAGISTRATES' COURT")
                                .withCode(ouCode)
                                .build())
                        .build())
                .withPocaFileId(randomUUID())
                .withSenderEmail("test@test.com")
                .build();

        final OrganisationUnitWithCourtroomReferenceData courtroomReferenceData = organisationUnitWithCourtroomReferenceData()
                .withId(randomUUID().toString())
                .withOucode("B13CC00")
                .withCourtRoom(CourtRoom.courtRoom().withCourtroomName("LAVENDER HILL MAGISTRATES' COURT").build())
                .build();

        when(submitApplicationValidator.validate(any(), any())).thenReturn(emptyList());
        when(additionalInformation.getCourtroomReferenceData()).thenReturn(courtroomReferenceData);
        final Stream<Object> eventStream = applicationAggregate.acceptSubmitApplication(submitApplication, submitApplicationValidator, additionalInformation);

        final SubmitApplicationAccepted actualSubmitApplicationAccepted = (SubmitApplicationAccepted) eventStream.findFirst().get();

        assertThat(actualSubmitApplicationAccepted.getCourtApplication().getId(), is(submitApplication.getCourtApplication().getId()));
        assertThat(actualSubmitApplicationAccepted.getBoxHearingRequest().getId(), is(submitApplication.getBoxHearingRequest().getId()));
        assertThat(actualSubmitApplicationAccepted.getPocaFileId(), is(submitApplication.getPocaFileId()));
        assertThat(actualSubmitApplicationAccepted.getSenderEmail(), is(submitApplication.getSenderEmail()));
    }

    @Test
    public void shouldRaiseSubmitApplicationValidationFailedEventWhenThereIsValidationError() {
        final List<Optional<ValidationError>> validationError = singletonList(of(valueOf("APPLICATION_TYPE_NOT_FOUND")));

        final SubmitApplication submitApplication = SubmitApplication.submitApplication()
                .withCourtApplication(courtApplication().withId(randomUUID()).build())
                .withBoxHearingRequest(boxHearingRequest().withId(randomUUID()).build())
                .build();

        when(submitApplicationValidator.validate(any(), any())).thenReturn(validationError);
        final Stream<Object> eventStream = applicationAggregate.acceptSubmitApplication(submitApplication, submitApplicationValidator, additionalInformation);

        final SubmitApplicationValidationFailed submitApplicationValidationFailed = (SubmitApplicationValidationFailed) eventStream.findFirst().get();

        assertThat(submitApplicationValidationFailed.getErrorDetails().getErrorDetails().size(), is(1));
        assertThat(submitApplicationValidationFailed.getErrorDetails().getErrorDetails().get(0).getErrorCode(), is(APPLICATION_TYPE_NOT_FOUND.getCode()));
        assertThat(submitApplicationValidationFailed.getErrorDetails().getErrorDetails().get(0).getErrorDescription(), is(APPLICATION_TYPE_NOT_FOUND.getText()));
        assertThat(submitApplicationValidationFailed.getApplicationSubmitted().getCourtApplication(), is(submitApplication.getCourtApplication()));
        assertThat(submitApplicationValidationFailed.getApplicationSubmitted().getBoxHearingRequest(), is(submitApplication.getBoxHearingRequest()));
    }
}