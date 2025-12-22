package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtCentre.courtCentre;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted.submitApplicationAccepted;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed.submitApplicationValidationFailed;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Error;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ErrorDetails;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CourtApplicationCreatedFromProgression;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.EnrichedApplicationData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


@SuppressWarnings({"squid:S1068", "squid:S1450"})
public class ApplicationAggregate implements Aggregate {

    private static final long serialVersionUID = -846244438348619250L;

    private String senderEmail;
    private UUID pocaFileId;

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(SubmitApplicationAccepted.class).apply(this::onPocaSubmitApplicationAccepted),
                otherwiseDoNothing());
    }

    private void onPocaSubmitApplicationAccepted(SubmitApplicationAccepted submitApplicationAccepted) {
        this.pocaFileId = submitApplicationAccepted.getPocaFileId();
        this.senderEmail = submitApplicationAccepted.getSenderEmail();
    }

    public Stream<Object> acceptSubmitApplication(final SubmitApplication submitApplication, final SubmitApplicationValidator submitApplicationValidator, final AdditionalInformation additionalInformation) {

        final List<Optional<ValidationError>> validationErrors = submitApplicationValidator.validate(submitApplication, additionalInformation);
        final ErrorDetails errorDetails = buildErrorDetails(validationErrors);

        if (!errorDetails.getErrorDetails().isEmpty()) {
            return apply(Stream.of(submitApplicationValidationFailed()
                    .withApplicationSubmitted(ApplicationSubmitted.applicationSubmitted()
                            .withCourtApplication(submitApplication.getCourtApplication())
                            .withBoxHearingRequest(submitApplication.getBoxHearingRequest())
                            .withProsecutionCases(submitApplication.getProsecutionCases())
                            .build())
                    .withSenderEmail(submitApplication.getSenderEmail())
                    .withEmailSubject(submitApplication.getEmailSubject())
                    .withErrorDetails(errorDetails)
                    .build()));
        } else {//when all validations pass
            return apply(Stream.of(submitApplicationAccepted()
                    .withCourtApplication(submitApplication.getCourtApplication())
                    .withBoxHearingRequest(enrichedBoxHearingRequest(submitApplication.getBoxHearingRequest(), additionalInformation.getCourtroomReferenceData()))
                    .withEnrichedApplicationData(EnrichedApplicationData.enrichedApplicationData()
                            .withCourtApplicationType(findCourtApplicationType(submitApplication.getCourtApplication().getCourtApplicationType(), additionalInformation.getApplicationTypes()))
                            .build())
                    .withPocaFileId(submitApplication.getPocaFileId())
                    .withSenderEmail(submitApplication.getSenderEmail())
                    .build()));
        }
    }


    public Stream<Object> acceptPocaApplication(final CourtApplication courtApplication) {
        if (null == this.pocaFileId) {
            return Stream.empty();
        }
        return apply(Stream.of(CourtApplicationCreatedFromProgression.courtApplicationCreatedFromProgression()
                .withCourtApplication(courtApplication)
                .withPocaFileId(this.pocaFileId)
                .withSenderEmail(this.senderEmail)
                .build()));
    }

    private ErrorDetails buildErrorDetails(final List<Optional<ValidationError>> validationErrors) {
        final List<Error> errors = new ArrayList<>();
        validationErrors.forEach(s -> {
            if (s.isPresent()) {
                final ValidationError validationError = s.get();
                errors.add(new Error(validationError.getCode(), validationError.getText()));
            }
        });
        return new ErrorDetails(errors);
    }

    private uk.gov.justice.core.courts.CourtApplicationType findCourtApplicationType(final CourtApplicationType courtApplicationType, final List<uk.gov.justice.core.courts.CourtApplicationType> applicationTypes) {

        return ofNullable(applicationTypes).orElse(emptyList()).stream()
                .filter(applicationType -> nonNull(applicationType.getCode()))
                .filter(applicationType -> applicationType.getCode().equals(courtApplicationType.getCode())).findAny().orElse(null);
    }

    private BoxHearingRequest enrichedBoxHearingRequest(BoxHearingRequest boxHearingRequest, OrganisationUnitWithCourtroomReferenceData courtroomReferenceData) {
        return BoxHearingRequest.boxHearingRequest()
                .withValuesFrom(boxHearingRequest)
                .withCourtCentre(courtCentre()
                        .withId(isNull(boxHearingRequest.getCourtCentre().getId()) || boxHearingRequest.getCourtCentre().getId().toString().equals(courtroomReferenceData.getId())
                                ? fromString(courtroomReferenceData.getId())
                                : boxHearingRequest.getCourtCentre().getId())
                        .withCode(isNull(boxHearingRequest.getCourtCentre().getCode()) || boxHearingRequest.getCourtCentre().getCode().equals(courtroomReferenceData.getOucode())
                                ? courtroomReferenceData.getOucode()
                                : boxHearingRequest.getCourtCentre().getCode())
                        .withName(boxHearingRequest.getCourtCentre().getName())
                        .build())
                .build();
    }
}
