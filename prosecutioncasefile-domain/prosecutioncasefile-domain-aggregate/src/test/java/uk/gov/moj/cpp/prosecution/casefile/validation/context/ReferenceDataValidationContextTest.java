package uk.gov.moj.cpp.prosecution.casefile.validation.context;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ReferenceDataValidationContextTest {

    @Test
    public void shouldReturnReferenceData() {
        final ReferenceDataValidationContext context = testReferenceDataContext();

        assertThat(context.offenceCodeReferenceData, is(testOffenceReferenceDataList()));
        assertThat(context.nationalityReferenceData, is(testNationalityReferenceDataList()));
    }

    private ReferenceDataValidationContext testReferenceDataContext() {
        return ReferenceDataValidationContext.newInstance(testOffenceReferenceDataList(), testNationalityReferenceDataList());
    }

    private List<OffenceReferenceData> testOffenceReferenceDataList() {
        return  asList(
                offenceReferenceData().withCjsOffenceCode("CODE1").withLocationRequired("Y").build(),
                offenceReferenceData().withCjsOffenceCode("CODE2").withLocationRequired("N").build());
    }

    private List<ReferenceDataCountryNationality> testNationalityReferenceDataList() {
        return asList(
                referenceDataCountryNationality().withNationality("InDian").build(),
                referenceDataCountryNationality().withNationality("BritisH").build());
    }

    @Test
    public void shouldReturnTrueWhenOffenceIsBlacklistedOnCommittedDate() {
        final ReferenceDataValidationContext context = ReferenceDataValidationContext.newInstance(
                asList(offenceReferenceData()
                        .withCjsOffenceCode("PC02554")
                        .withBlacklisted(true)
                        .withBlacklistValidFrom("2026-01-01")
                        .withBlacklistValidTo("2026-08-12")
                        .build()),
                testNationalityReferenceDataList());

        assertThat(context.isOffenceBlacklisted("PC02554", LocalDate.of(2026, 8, 11)), is(true));
    }

    @Test
    public void shouldReturnFalseWhenOffenceIsNotBlacklistedOnCommittedDate() {
        final ReferenceDataValidationContext context = ReferenceDataValidationContext.newInstance(
                asList(offenceReferenceData()
                        .withCjsOffenceCode("PC02554")
                        .withBlacklisted(false)
                        .build()),
                testNationalityReferenceDataList());

        assertThat(context.isOffenceBlacklisted("PC02554", LocalDate.of(2026, 8, 11)), is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoBlacklistDataExistsForOffence() {
        final ReferenceDataValidationContext context = testReferenceDataContext();

        assertThat(context.isOffenceBlacklisted("PC02554", LocalDate.of(2026, 8, 11)), is(false));
    }

    @Test
    public void shouldReturnFalseWhenCommittedDateOutsideBlacklistWindow() {
        final ReferenceDataValidationContext context = ReferenceDataValidationContext.newInstance(
                asList(offenceReferenceData()
                        .withCjsOffenceCode("PC02554")
                        .withBlacklisted(true)
                        .withBlacklistValidFrom("2026-01-01")
                        .withBlacklistValidTo("2026-01-31")
                        .build()),
                testNationalityReferenceDataList());

        assertThat(context.isOffenceBlacklisted("PC02554", LocalDate.of(2026, 8, 11)), is(false));
    }

    @Test
    public void shouldDisambiguateBlacklistStatusWhenSameOffenceCodeHasEntriesForDifferentCommittedDates() {
        final ReferenceDataValidationContext context = ReferenceDataValidationContext.newInstance(
                asList(offenceReferenceData()
                                .withCjsOffenceCode("PC02554")
                                .withBlacklisted(true)
                                .withBlacklistValidFrom("2026-01-01")
                                .withBlacklistValidTo("2026-01-31")
                                .build(),
                        offenceReferenceData()
                                .withCjsOffenceCode("PC02554")
                                .withBlacklisted(false)
                                .build()),
                testNationalityReferenceDataList());

        assertThat(context.isOffenceBlacklisted("PC02554", LocalDate.of(2026, 1, 15)), is(true));
        assertThat(context.isOffenceBlacklisted("PC02554", LocalDate.of(2026, 8, 11)), is(false));
    }

}