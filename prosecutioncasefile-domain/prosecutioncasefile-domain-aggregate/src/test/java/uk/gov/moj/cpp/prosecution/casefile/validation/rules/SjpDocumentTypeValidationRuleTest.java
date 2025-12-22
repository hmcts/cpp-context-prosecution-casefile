package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_DOCUMENT_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class SjpDocumentTypeValidationRuleTest {

    private SjpDocumentTypeValidationRule sjpDocumentTypeValidationRule = new SjpDocumentTypeValidationRule();

    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldReturnProblemWhenDocumentTypeIsNotValid() {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData("MC100");

        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(INVALID_DOCUMENT_TYPE, "documentType", caseDocumentWithReferenceData.getMaterial().getDocumentType());

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsSJPN() {
        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(getCaseDocumentWithReferenceData("SJPN"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsCITN() {
        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(getCaseDocumentWithReferenceData("CITN"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsPLEA() {
        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(getCaseDocumentWithReferenceData("PLEA"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsFINANCIALMEANS() {
        final Material material = getMaterial("FINANCIAL_MEANS");
        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(getCaseDocumentWithReferenceData("FINANCIAL_MEANS"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsDISQUALIFICATIONREPLYSLIP() {
        final Material material = getMaterial("DISQUALIFICATION_REPLY_SLIP");
        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(getCaseDocumentWithReferenceData("DISQUALIFICATION_REPLY_SLIP"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }



    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsOTHERTYPE() {
        final Material material = getMaterial("OTHER-Passport");
        final Optional<Problem> actualProblem = sjpDocumentTypeValidationRule.validate(getCaseDocumentWithReferenceData("OTHER-Passport"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    private CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final String documentType){
        return new CaseDocumentWithReferenceData(randomUUID(),false, getMaterial(documentType), null, null, documentType, false, false);
    }

    private Material getMaterial(final String documentType) {
        return new Material(documentType, UUID.randomUUID(), randomAlphanumeric(10), false);
    }
}
