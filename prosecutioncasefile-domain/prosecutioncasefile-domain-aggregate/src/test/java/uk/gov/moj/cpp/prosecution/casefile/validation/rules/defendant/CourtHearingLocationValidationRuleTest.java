package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.COURT_HEARING_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_COURT_HEARING_LOCATION;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.Optional;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtHearingLocationValidationRuleTest {

    public static final String COURT_HEARING_LOCATION = "B0ABC67";
    public static final String INVALID_COURT_HEARING_LOCATION = "abcd";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCourtHearingLocationIsValid() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getCourtHearingLocation()).thenReturn(COURT_HEARING_LOCATION);
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(ofNullable(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build()));
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

        //should use cached value when invoked second time
        optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        MatcherAssert.assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenMigartedCaseIsInActive() {
        when(defendantWithReferenceData.isInactiveMigratedCase()).thenReturn(true);

        Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

        //should use cached value when invoked second time
        optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenCourtHearingLocationIsInValid() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getCourtHearingLocation()).thenReturn(COURT_HEARING_LOCATION);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(anyString())).thenReturn(Optional.empty());
        final Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_COURT_HEARING_LOCATION.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(COURT_HEARING_LOCATION));
    }

    @Test
    public void shouldReturnCourtHearingLocationAsValidWhenNoEnrichFillData() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getCourtHearingLocation()).thenReturn(COURT_HEARING_LOCATION);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(anyString())).thenReturn(ofNullable(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build()));
        final Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenCourtHearingLocationDoesNotExist() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getCourtHearingLocation()).thenReturn(null);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        final Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_COURT_HEARING_LOCATION.getValue()));
        assertNull(optionalProblem.get().getValues().get(0).getValue());
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    public void shouldReturnProblemWhenCourtHearingLocationInValidLength() {
        when(defendantWithReferenceData.getDefendant().getInitialHearing().getCourtHearingLocation()).thenReturn(INVALID_COURT_HEARING_LOCATION);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        final Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.get().getCode(), is(COURT_HEARING_LOCATION_OUCODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_COURT_HEARING_LOCATION.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_COURT_HEARING_LOCATION));
        verifyNoInteractions(referenceDataQueryService);
    }

    @Test
    void shouldReturnValidWhenMCCWithListNewHearing() {
        when(defendantWithReferenceData.isMCCWithListNewHearing()).thenReturn(true);
        final Optional<Problem> optionalProblem = new CourtHearingLocationValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertTrue(optionalProblem.isEmpty());

    }

    private List<OrganisationUnitReferenceData> buildOrganisationUnits() {
        return singletonList(organisationUnitReferenceData().build());
    }
}