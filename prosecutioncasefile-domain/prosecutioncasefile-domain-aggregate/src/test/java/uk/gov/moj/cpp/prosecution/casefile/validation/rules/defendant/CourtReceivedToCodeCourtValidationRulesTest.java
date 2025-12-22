package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtReceivedToCodeCourtValidationRulesTest {

    private final String COURT_RECEIVED_TO_CODE = "B01BH00";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void validate() {

        when(defendantWithReferenceData.getCaseDetails().getCourtReceivedToCode())
                .thenReturn(COURT_RECEIVED_TO_CODE);

        List<OrganisationUnitReferenceData> organisationUnitReferenceData = new ArrayList<>();
        OrganisationUnitReferenceData organisationUnitReference = OrganisationUnitReferenceData.organisationUnitReferenceData().build();
        organisationUnitReferenceData.add(organisationUnitReference);
        when(referenceDataQueryService.retrieveOrganisationUnits(isA(String.class)))
                .thenReturn(organisationUnitReferenceData);

        final Optional<Problem> optionalProblem = new CourtReceivedToCodeCourtValidationRules()
                .validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void validateWithErrors() {
        when(defendantWithReferenceData.getCaseDetails().getCourtReceivedToCode())
                .thenReturn(COURT_RECEIVED_TO_CODE);

        when(referenceDataQueryService.retrieveOrganisationUnits(isA(String.class)))
                .thenReturn(Collections.EMPTY_LIST);

        final Optional<Problem> optionalProblem = new CourtReceivedToCodeCourtValidationRules()
                .validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is("courtReceivedToCode"));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(COURT_RECEIVED_TO_CODE));
        assertThat(optionalProblem.get().getCode(), is("COURT_LOCATION_OUCODE_INVALID"));
    }
}
