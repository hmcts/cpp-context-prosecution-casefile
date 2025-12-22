package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext.withOffenceCodeReferenceDataOnly;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OffenceNotSummaryValidationRuleTest {


    @ParameterizedTest
    @ValueSource(strings = {"Either Way", "Indictable"})
    void shouldBeNotValidWhenNonSummaryCase(final String modeOfTrialsDerived) {
        final String offenceCode = "CODE1";
        final int offenceSequenceNumber = 1;
        final Offence offence = offence()
                .withOffenceCode(offenceCode)
                .withOffenceSequenceNumber(offenceSequenceNumber)
                .withOffenceId(UUID.randomUUID())
                .build();

        final Defendant defendant = Defendant.defendant().withOffences(singletonList(offence)).build();

        final ValidationResult result = OffenceNotSummaryValidationRule.INSTANCE.validate(
                defendant, withOffenceCodeReferenceDataOnly(singletonList(
                        offenceReferenceData()
                                .withModeOfTrialDerived(modeOfTrialsDerived).withCjsOffenceCode(offenceCode).build()
                )));

        assertFalse(result.isValid());
        assertThat(result.problems().size(), is(1));
        assertThat(result.problems().get(0).getCode(), is("OFFENCE_NOT_SUMMARY"));
        assertThat(result.problems().get(0).getValues().size(), is(2));
        assertThat(result.problems().get(0).getValues().get(0).getKey(), is("offenceCode"));
        assertThat(result.problems().get(0).getValues().get(0).getValue(), is(offenceCode));
        assertThat(result.problems().get(0).getValues().get(1).getKey(), is("offenceSequenceNo"));
        assertThat(result.problems().get(0).getValues().get(1).getValue(), is(String.valueOf(offenceSequenceNumber)));
    }

    @Test
    void shouldBeValidWhenSummaryCase() {
        final String offenceCode = "CODE1";
        final Offence offence = offence()
                .withOffenceSequenceNumber(1)
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .build();

        final Defendant defendant = Defendant.defendant().withOffences(singletonList(offence)).build();

        final ValidationResult result = OffenceNotSummaryValidationRule.INSTANCE.validate(
                defendant, withOffenceCodeReferenceDataOnly(singletonList(
                        offenceReferenceData()
                                .withModeOfTrialDerived("Summary")
                                .withCjsOffenceCode(offenceCode).build()
                )));

        assertTrue(result.isValid());
    }

}
