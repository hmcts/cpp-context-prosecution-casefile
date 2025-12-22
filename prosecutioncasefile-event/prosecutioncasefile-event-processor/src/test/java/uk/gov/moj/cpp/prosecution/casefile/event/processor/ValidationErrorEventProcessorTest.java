package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantProblemsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;

import java.util.UUID;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;

@ExtendWith(MockitoExtension.class)
public class ValidationErrorEventProcessorTest {

    private static final String PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED = "public.prosecutioncasefile.events.sjp-validation-failed";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED = "public.prosecutioncasefile.events.case-validation-failed";
    private static final String PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED = "public.prosecutioncasefile.events.defendant-validation-failed";

    private static final String PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED = "prosecutioncasefile.events.sjp-validation-failed";
    private static final String PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED = "prosecutioncasefile.events.case-validation-failed";
    private static final String PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED = "prosecutioncasefile.events.defendant-validation-failed";

    private final UUID caseId = randomUUID();

    private final UUID externalId = randomUUID();

    @Mock
    private ReferenceDataVO referenceDataVO;

    @Captor
    private ArgumentCaptor<Envelope> captor;

    @Mock
    private Sender sender;

    @InjectMocks
    private ValidationErrorEventProcessor validationErrorEventProcessor;

    @Test
    public void shouldPersistBusinessValidationErrorDetailsOnDefendantValidationFailed() {
        final DefendantValidationFailed defendantValidationFailed = DefendantValidationFailed.defendantValidationFailed()
                .withCaseId(randomUUID())
                .build();
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED)
                .withId(randomUUID())
                .build();

        final Envelope<DefendantValidationFailed> envelope = envelopeFrom(metadata, defendantValidationFailed);

        validationErrorEventProcessor.defendantsValidationFailedPrivateEvent(envelope);

        verify(sender).send(captor.capture());

        final Envelope outputEnvelope = captor.getValue();

        assertThat(outputEnvelope.metadata().name(), Matchers.is(PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED));
      }

    @Test
    public void shouldPersistBusinessValidationErrorDetailsOnCaseValidationFailed() {
        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.
                        caseDetails()
                        .withCaseId(caseId)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference("prosecutorCaseReference")
                        .build())
                .withChannel(SPI)
                .withDefendants(emptyList())
                .build();

        final Problem problem = new Problem("Code", singletonList(new ProblemValue("id", "key", "value")));
        final CaseValidationFailed caseValidationFailed = new CaseValidationFailed(
                prosecution,
                singletonList(problem),
                externalId, getInitialHearing());

        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED)
                .withId(randomUUID())
                .build();

        final Envelope<CaseValidationFailed> caseValidationFailedEnvelope = envelopeFrom(metadata, caseValidationFailed);

        validationErrorEventProcessor.caseValidationFailedPrivateEvent(caseValidationFailedEnvelope);

        verify(sender).send(captor.capture());

        final Envelope outputEnvelope = captor.getValue();

        assertThat(outputEnvelope.metadata().name(), Matchers.is(PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED));
    }

    @Test
    public void shouldPersistBusinessValidationErrorDetailsOnSJPValidationFailed() {
        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.
                        caseDetails()
                        .withCaseId(caseId)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference("prosecutorCaseReference")
                        .build())
                .withChannel(SPI)
                .withDefendants(emptyList())
                .build();

        final Defendant defendant = defendant()
                .withId(randomUUID().toString())
                .withCustodyStatus("custodyStatus")
                .withOrganisationName("organisationName")
                .withInitialHearing(
                        initialHearing()
                                .withDateOfHearing(now().toString())
                                .withCourtHearingLocation("courtHearingLocation")
                                .build())
                .withIndividual(
                        individual()
                                .withPersonalInformation(
                                        personalInformation()
                                                .withFirstName("FirstName")
                                                .withLastName("LastName")
                                                .build())
                                .build())
                .withOffences(singletonList(offence()
                        .withChargeDate(now())
                        .build()))
                .build();

        final Problem problem = new Problem("Code", singletonList(new ProblemValue("id", "key", "value")));
        final DefendantProblemsVO defendantProblemsVO = new DefendantProblemsVO(defendant, singletonList(problem));

        final SjpValidationFailed sjpValidationFailed = new SjpValidationFailed(
                prosecution,
                singletonList(problem),
                singletonList(defendantProblemsVO),
                referenceDataVO,
                getInitialHearing()
                );

        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED)
                .withId(randomUUID())
                .build();

        final Envelope<SjpValidationFailed> sjpValidationFailedEnvelope = envelopeFrom(metadata, sjpValidationFailed);

        validationErrorEventProcessor.sjpValidationFailedPrivateEvent(sjpValidationFailedEnvelope);

        verify(sender).send(captor.capture());

        final Envelope outputEnvelope = captor.getValue();

        assertThat(outputEnvelope.metadata().name(), Matchers.is(PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED));
    }

    private InitialHearing getInitialHearing() {
        InitialHearing initialHearing = InitialHearing.initialHearing()
                .withDateOfHearing("2050-02-04")
                .build();
        return initialHearing;
    }
}
