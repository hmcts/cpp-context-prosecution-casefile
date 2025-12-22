package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_FILE_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileTypeValidationRuleTest {

    private FileTypeValidationRule fileTypeValidationRule = new FileTypeValidationRule();

    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldNotReturnProblemWhenFileTypeIsAllowed() {
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("image/bmp"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("image/jpeg"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("image/png"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("image/tiff"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("text/plain"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/vnd.ms-excel"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/msword"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/vnd.openxmlformats-officedocument.wordprocessingml.document"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/vnd.ms-powerpoint"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/vnd.openxmlformats-officedocument.presentationml.presentation"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
        assertThat(fileTypeValidationRule.validate(getCaseDocumentWithReferenceData("application/pdf"), referenceDataQueryService).problems().stream().findFirst(), is(empty()));
    }

    @Test
    public void shouldReturnProblemWhenFileTypeIsNotAllowed() {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData("invalid_file_type");

        final Optional<Problem> actualProblem = fileTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(INVALID_FILE_TYPE, "materialContentType", caseDocumentWithReferenceData.getMaterialContentType());

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final String materialContentType) {
        return new CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, null, null, null, materialContentType, null);
    }
}
