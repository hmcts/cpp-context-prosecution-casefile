package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;

import java.util.List;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@SuppressWarnings("squid:S2187")
public class CourtFeeDetailsValidationRuleTest {

    private SubmitApplication submitApplication;

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    private final CourtFeeDetailsValidationRule courtFeeDetailsValidationRule = new CourtFeeDetailsValidationRule();

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() throws IllegalAccessException {

        List<SubmitApplicationValidationRule> validationRules = singletonList(courtFeeDetailsValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }
}