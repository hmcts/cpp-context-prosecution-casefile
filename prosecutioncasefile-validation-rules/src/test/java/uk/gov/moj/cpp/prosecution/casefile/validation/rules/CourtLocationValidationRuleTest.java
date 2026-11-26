package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest.boxHearingRequest;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication.submitApplication;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtCentre;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtLocationValidationRuleTest {

    public static final String VALID_COURT_NAME = "Bexley Magistrates' Court";
    public static final String VALID_CENTER_CODE = "centerCode";
    public static final String INVALID_CENTER_CODE = "invCenterCode";

    private SubmitApplication submitApplication;

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @InjectMocks
    private final CourtLocationValidationRule courtLocationValidationRule = new CourtLocationValidationRule();


    @BeforeEach
    public void setup() throws IllegalAccessException {
        List<SubmitApplicationValidationRule> validationRules = singletonList(courtLocationValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotReturnErrorWhenCourtCenterNameAndCodeIsValid() {

        submitApplication = submitApplication()
                .withBoxHearingRequest(boxHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withName(VALID_COURT_NAME)
                                .withCode(VALID_CENTER_CODE)
                                .build()).build()).build();

        OrganisationUnitWithCourtroomReferenceData unitWithCourtroomReferenceData = organisationUnitWithCourtroomReferenceData().withOucode(VALID_CENTER_CODE).withOucodeL3Name(VALID_COURT_NAME).build();
        AdditionalInformation additionalInformation = new AdditionalInformation(null,null,unitWithCourtroomReferenceData);

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnErrorWhenCourtCenterNameIsValidAndCodeIsNull() {

        submitApplication = submitApplication()
                .withBoxHearingRequest(boxHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withName(VALID_COURT_NAME)
                                .build()).build()).build();

        OrganisationUnitWithCourtroomReferenceData unitWithCourtroomReferenceData = organisationUnitWithCourtroomReferenceData().withOucode(VALID_CENTER_CODE).withOucodeL3Name(VALID_COURT_NAME).build();
        AdditionalInformation additionalInformation = new AdditionalInformation(null,null,unitWithCourtroomReferenceData);

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldReturnErrorWhenCourtCenterNameNotFound() {

        submitApplication = submitApplication()
                .withBoxHearingRequest(boxHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withName(VALID_COURT_NAME).build()).build()).build();

        OrganisationUnitWithCourtroomReferenceData unitWithCourtroomReferenceData = organisationUnitWithCourtroomReferenceData().withOucodeL3Name(null).build();
        AdditionalInformation additionalInformation = new AdditionalInformation(null,null,unitWithCourtroomReferenceData);
        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.COURT_LOCATION_REQUIRED));
    }

    @Test
    public void shouldReturnErrorWhenCourtCenterNameValidButCodeIsInValid() {

        submitApplication = submitApplication()
                .withBoxHearingRequest(boxHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withName(VALID_COURT_NAME)
                                .withCode(INVALID_CENTER_CODE)
                                .build()).build()).build();

        OrganisationUnitWithCourtroomReferenceData unitWithCourtroomReferenceData = organisationUnitWithCourtroomReferenceData()
                .withOucode(VALID_CENTER_CODE)
                .withOucodeL3Name(null).build();
        AdditionalInformation additionalInformation = new AdditionalInformation(null,null,unitWithCourtroomReferenceData);
        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.COURT_LOCATION_REQUIRED));
    }

}