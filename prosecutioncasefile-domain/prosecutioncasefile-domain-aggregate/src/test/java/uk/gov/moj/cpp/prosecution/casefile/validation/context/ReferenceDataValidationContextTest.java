package uk.gov.moj.cpp.prosecution.casefile.validation.context;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;

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

}