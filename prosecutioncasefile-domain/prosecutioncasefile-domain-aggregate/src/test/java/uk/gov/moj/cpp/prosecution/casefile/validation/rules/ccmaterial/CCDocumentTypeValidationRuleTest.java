package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class CCDocumentTypeValidationRuleTest {

    private static final String VALID_DOCUMENT_TYPE = "ABC";
    private static final String VALID_DOCUMENT_TYPE_LOWER_CASE = "abc";
    private static final String INVALID_DOCUMENT_TYPE = "BCD";

    private final CCDocumentTypeValidationRule ccDocumentTypeValidationRule = new CCDocumentTypeValidationRule();

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @BeforeEach
     void setup() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDocumentMetadataReferenceDataList());
    }

    @Test
     void shouldReturnProblemWhenDocumentTypeIsNotInReferenceData() {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(INVALID_DOCUMENT_TYPE);
        final Optional<Problem> actualProblem = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(ProblemCode.INVALID_DOCUMENT_TYPE, "documentType", INVALID_DOCUMENT_TYPE);

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
     void shouldNotReturnProblemWhenDocumentTypeIsInReferenceData() {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(VALID_DOCUMENT_TYPE);
        final Optional<Problem> actualProblem = ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
     void shouldOverwriteDocumentTypeForApplication() {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentForApplicationWithReferenceData(VALID_DOCUMENT_TYPE);

        ccDocumentTypeValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(caseDocumentWithReferenceData.getDocumentCategory(), is("Applications"));
        assertThat(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory(), is("Applications"));

    }

    private CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final String documentType) {
        return new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), "Prosecutor defendant id",
                emptyList(), documentType, false, false);
    }

    private CaseDocumentWithReferenceData getCaseDocumentForApplicationWithReferenceData(final String documentType) {
        CaseDocumentWithReferenceData caseDocumentWithReferenceData = new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), "Prosecutor defendant id",
                emptyList(), documentType, false, false);
        caseDocumentWithReferenceData.setHasApplication(true);
        caseDocumentWithReferenceData.setCourtApplicationSubject(CourtApplicationSubject.courtApplicationSubject().build());
        return caseDocumentWithReferenceData;
    }

    private List<DocumentTypeAccessReferenceData> getMockDocumentMetadataReferenceDataList() {
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = new ArrayList<>();
        documentMetadataReferenceDataList.add(documentTypeAccessReferenceData()
                .withSection(VALID_DOCUMENT_TYPE_LOWER_CASE)
                .withDocumentCategory("Document category")
                .withId(randomUUID())
                .build());
        return documentMetadataReferenceDataList;
    }
}
