package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.IMPRISONABLE_OFFENCE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext.withOffenceCodeReferenceDataOnly;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ImprisonableOffenceValidationRuleTest {

    private UUID id;

    @BeforeEach
    public void setup() {
        id = UUID.randomUUID();
    }

    @Test
    public void shouldNotReturnProblemWhenNotContainsImprisonableOffences() {
        final ValidationResult validationResult = ImprisonableOffenceValidationRule.INSTANCE.validate(
                getProsecutionWithMultiOffences(asList(of("CODE1"), of("CODE2"))), getArbitraryReferenceDataContext());

        assertThat(validationResult.isValid(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenContainsImprisonableOffences() {
        final ValidationResult validationResult = ImprisonableOffenceValidationRule.INSTANCE.validate(
                getProsecutionWithMultiOffences(asList(of("CODE1"), of("CODE2"), of("CODE3"), of("CODE4"))), getArbitraryReferenceDataContext());

        assertEquals(validationResult, newValidationResult(asList(
                newProblem(IMPRISONABLE_OFFENCE,
                        new ProblemValue(id.toString(), "offenceCode", "CODE3"),
                        new ProblemValue(id.toString(), "offenceSequenceNo", "3")
                ),
                newProblem(IMPRISONABLE_OFFENCE,
                        new ProblemValue(id.toString(), "offenceCode", "CODE4"),
                        new ProblemValue(id.toString(), "offenceSequenceNo", "4")
                ))

        ));
    }

    private ReferenceDataValidationContext getArbitraryReferenceDataContext() {
        return withOffenceCodeReferenceDataOnly(asList(
                offenceReferenceData().withCjsOffenceCode("CODE1").withModeOfTrial("STRAFF").build(),
                offenceReferenceData().withCjsOffenceCode("CODE2").withModeOfTrial("SNONIMP").build(),
                offenceReferenceData().withCjsOffenceCode("CODE3").withModeOfTrial("SIMP").build(),
                offenceReferenceData().withCjsOffenceCode("CODE4").withModeOfTrial("SIMP").build()
        ));
    }

    private Defendant getProsecutionWithMultiOffences(final List<Optional<String>> offenceCodes) {
        return defendant()
                        .withOffences(buildOffences(offenceCodes))
                        .build();
    }

    private List<Offence> buildOffences(final List<Optional<String>> offenceCodes) {
        final AtomicInteger seqNumber = new AtomicInteger(0);
        return offenceCodes.stream()
                .map(a -> buildOffence(a, seqNumber))
                .collect(toList());

    }

    private Offence buildOffence(final Optional<String> offenceCode, final AtomicInteger seqNumber) {

        final Offence.Builder offenceBuilder = offence();
        offenceCode.ifPresent(offenceBuilder::withOffenceCode);
        offenceBuilder.withOffenceSequenceNumber(seqNumber.incrementAndGet());
        offenceBuilder.withOffenceCommittedDate(now());
        offenceBuilder.withOffenceId(id);
        return offenceBuilder.build();
    }
}