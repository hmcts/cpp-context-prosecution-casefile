package uk.gov.moj.cpp.prosecution.casefile.validation.rules;


import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_MARKER_IS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CASE_MARKERS;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseMarkersValidationAndEnricherRuleTest {

    private static final String VALID_CASE_MARKER = "AB";
    private static final String VALID_CASE_MARKER_LOWER_CASE = "ab";
    private static final String INVALID_CASE_MARKER_1 = "XY";
    private static final String INVALID_CASE_MARKER_2 = "GH";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCaseMarkersIsNull() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers()).thenReturn(null);
        final Optional<Problem> optionalProblem = new CaseMarkersValidationAndEnricherRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenCaseMarkersIsEmpty() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers()).thenReturn(Collections.emptyList());

        final Optional<Problem> optionalProblem = new CaseMarkersValidationAndEnricherRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenCaseMarkersContainsValidTypeCode() {

        ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData();
        when(referenceDataQueryService.getCaseMarkerDetails()).thenReturn(getMockCaseMarkers());
        final Optional<Problem> optionalProblem = new CaseMarkersValidationAndEnricherRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getCaseMarkers().size(), is(1));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getCaseMarkers().get(0).getMarkerTypeCode(), is(VALID_CASE_MARKER));
    }

    @Test
    public void shouldReturnEmptyListWhenCaseMarkersContainsValidLowerCaseTypeCode() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers()).thenReturn(singletonList(caseMarker().withMarkerTypeCode(VALID_CASE_MARKER_LOWER_CASE).build()));
        when(referenceDataQueryService.getCaseMarkerDetails()).thenReturn(getMockCaseMarkers());

        final Optional<Problem> optionalProblem = new CaseMarkersValidationAndEnricherRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }


    @Test
    public void shouldReturnProblemWhenCaseMarkersContainsInvalidTypeCode() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers()).thenReturn(singletonList(caseMarker().withMarkerTypeCode(INVALID_CASE_MARKER_1).build()));
        when(referenceDataQueryService.getCaseMarkerDetails()).thenReturn(getMockCaseMarkers());

        final Optional<Problem> optionalProblem = new CaseMarkersValidationAndEnricherRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();


        assertThat(optionalProblem.get().getCode(), is(CASE_MARKER_IS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getId(), is("0"));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CASE_MARKERS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_CASE_MARKER_1));
    }

    @Test
    public void shouldReturn2ProblemsInCorrectOrderWhenCaseMarkersContains2InvalidTypeCodes() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseMarkers())
                .thenReturn(asList(
                        caseMarker()
                                .withMarkerTypeCode(INVALID_CASE_MARKER_1)
                                .build(),
                        caseMarker()
                                .withMarkerTypeCode(INVALID_CASE_MARKER_2)
                                .build()));
        when(referenceDataQueryService.getCaseMarkerDetails()).thenReturn(getMockCaseMarkers());

        final Optional<Problem> optionalProblem = new CaseMarkersValidationAndEnricherRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(CASE_MARKER_IS_INVALID.name()));

        assertThat(optionalProblem.get().getValues().get(0).getId(), is("0"));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CASE_MARKERS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_CASE_MARKER_1));

        assertThat(optionalProblem.get().getValues().get(1).getId(), is("1"));
        assertThat(optionalProblem.get().getValues().get(1).getKey(), is(CASE_MARKERS.getValue()));
        assertThat(optionalProblem.get().getValues().get(1).getValue(), is(INVALID_CASE_MARKER_2));
    }

    private List<CaseMarker> getMockCaseMarkers() {
        return of(caseMarker()
                        .withMarkerTypeCode(VALID_CASE_MARKER)
                        .build(),
                caseMarker()
                        .withMarkerTypeCode("DA")
                        .build());
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData() {
        return new ProsecutionWithReferenceData(Prosecution.prosecution()
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseMarkers(of(CaseMarker.caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER)
                                .build()))
                        .build())
                .build());
    }
}