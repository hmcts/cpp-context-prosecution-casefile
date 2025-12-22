package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpValidationFailed;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

@ServiceComponent(EVENT_PROCESSOR)
public class ValidationErrorEventProcessor {

    private static final String PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED = "public.prosecutioncasefile.events.sjp-validation-failed";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED = "public.prosecutioncasefile.events.case-validation-failed";
    private static final String PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED = "public.prosecutioncasefile.events.defendant-validation-failed";

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.sjp-validation-failed")
    public void sjpValidationFailedPrivateEvent(final Envelope<SjpValidationFailed> sjpValidationFailedEvent) {
        final SjpValidationFailed sjpValidationFailed = sjpValidationFailedEvent.payload();

        sender.send(envelop(sjpValidationFailed)
                .withName(PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED)
                .withMetadataFrom(sjpValidationFailedEvent));

    }

    @Handles("prosecutioncasefile.events.case-validation-failed")
    public void caseValidationFailedPrivateEvent(final Envelope<CaseValidationFailed> caseValidationFailedEvent) {
        final CaseValidationFailed caseValidationFailed = caseValidationFailedEvent.payload();

        sender.send(envelop(caseValidationFailed)
                .withName(PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED)
                .withMetadataFrom(caseValidationFailedEvent));

    }

    @Handles("prosecutioncasefile.events.defendant-validation-failed")
    public void defendantsValidationFailedPrivateEvent(final Envelope<DefendantValidationFailed> defendantValidationFailedEvent) {
        final DefendantValidationFailed defendantValidationFailed = defendantValidationFailedEvent.payload();

        sender.send(envelop(defendantValidationFailed)
                .withName(PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED)
                .withMetadataFrom(defendantValidationFailedEvent));

    }
}
