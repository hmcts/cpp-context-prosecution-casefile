package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_DEFENDANT_POST_CODE;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PostCodeValidationRuleTest {


    public static final String INVALID_POST_CODE = "CRO 2QX";
    public static final String ADDRESS_POSTCODE_FIELD = "address_postcode";
    public static final String ADDRESS_1 = "ASHBY WALK";
    private static final String VALID_POST_CODE = "CR0 2QX";
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock()
    private DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldMatchWhenNoPostCodeAvailable() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(Defendant.defendant().build());
        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        assertThat("Empty post code invalidated", validationResult.problems(), is(empty()));
    }

    @Test
    public void shouldPassWhenPostCodeIsValid() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(Defendant.defendant()
                .withAddress(Address.address()
                        .withAddress1(ADDRESS_1)
                        .withPostcode(VALID_POST_CODE)
                        .build())
                .build());
        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        assertThat(validationResult.problems(), is(empty()));
    }

    @Test
    public void shouldFailWhenPostCodeIsInvalid() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(Defendant.defendant()
                .withAddress(Address.address()
                        .withAddress1(ADDRESS_1)
                        .withPostcode(INVALID_POST_CODE)
                        .build())
                .build());
        final ValidationResult validationResult = new PostCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        final Problem problem = validationResult.problems().get(0);
        assertThat(problem, is(notNullValue()));
        assertThat(problem.getCode(), is(INVALID_DEFENDANT_POST_CODE.name()));
        assertThat(problem.getValues().get(0).getKey(), is(ADDRESS_POSTCODE_FIELD));
        assertThat(problem.getValues().get(0).getValue(), is(INVALID_POST_CODE));
    }

}
