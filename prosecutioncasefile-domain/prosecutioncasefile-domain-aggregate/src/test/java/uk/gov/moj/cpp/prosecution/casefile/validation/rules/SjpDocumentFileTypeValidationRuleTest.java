package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_FILE_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SjpDocumentFileTypeValidationRuleTest {

    private SjpFileTypeValidationRule fileTypeValidationRule = new SjpFileTypeValidationRule();

    private ReferenceDataQueryService referenceDataQueryService;

//    public String documentType;


    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("SJPN"),
                Arguments.of("CITN")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldReturnProblemWhenDocumentTypeIsAllowedBySjpButMimeTypeIsInvalid(final String documentType) {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(documentType, "application/json");

        final Optional<Problem> actualProblem = fileTypeValidationRule.validate(getCaseDocumentWithReferenceData(documentType, "application/json"), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(INVALID_FILE_TYPE, "fileType", caseDocumentWithReferenceData.getMaterial().getFileType());

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldReturnProblemWhenDocumentTypeIsAllowedBySjpButMimeTypeIsNotPresent(final String documentType) {
        final Optional<Problem> actualProblem = fileTypeValidationRule.validate(getCaseDocumentWithReferenceData(documentType, null), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(INVALID_FILE_TYPE, "fileType", "UNRECOGNISED");

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldNotReturnProblemWhenDocumentTypeIsAllowedBySjpAndMimeTypeIsValid(final String documentType) {
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData(documentType, "application/pdf"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData(documentType, "application/x-tika-msoffice"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData(documentType, "application/x-tika-ooxml"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData(documentType, "application/msword"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
    }

    @Test
    public void shouldNotReturnProblemWhenDocumentTypeIsNotAllowedBySjp() {
        final Optional<Problem> actualProblem = fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("PLEA", "application/pdf"), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem, is(empty()));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final String documentType, final String fileType){
        return new CaseDocumentWithReferenceData(randomUUID(),false, getMaterial(documentType, fileType), null, null, documentType, false, false);
    }

    private static Material getMaterial(final String documentType, final String fileType) {
        return new Material(documentType, randomUUID(), fileType, false);
    }
}
