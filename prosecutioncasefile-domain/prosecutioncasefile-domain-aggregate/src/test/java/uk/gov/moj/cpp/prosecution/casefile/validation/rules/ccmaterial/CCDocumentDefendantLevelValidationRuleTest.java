package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCDocumentDefendantLevelValidationRuleTest {

    private static final String VALID_DOCUMENT_TYPE = "Valid document type";
    private static final String DEFENDANT_LEVEL = "Defendant level";
    private static final String CASE_LEVEL = "Case level";
    private static final String VALID_PROSECUTOR_DEFENDANT_ID = "ABC";
    private static final String VALID_PROSECUTOR_DEFENDANT_ID_LOWER_CASE = "abc";
    private static final String INVALID_PROSECUTOR_DEFENDANT_ID = "BCD";

    private CCDocumentDefendantLevelValidationRule ccDocumentDefendantLevelValidationRule = new CCDocumentDefendantLevelValidationRule();

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldReturnProblemWhenProsecutorDefendantIdMissingForDefendantLevelDocument() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDefendantLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), null, getDefendants(), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(ProblemCode.DEFENDANT_ID_REQUIRED, "prosecutorDefendantId", "");

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldReturnProblemWhenProsecutorDefendantIdNotValidForDefendantLevelDocument() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDefendantLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), INVALID_PROSECUTOR_DEFENDANT_ID, getDefendants(), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(ProblemCode.DEFENDANT_ID_INVALID, "prosecutorDefendantId", "BCD");

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldReturnNoProblemWhenProsecutorDefendantIdValid() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockDefendantLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), VALID_PROSECUTOR_DEFENDANT_ID_LOWER_CASE, getDefendants(), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoProblemWhenCaseLevelDocumentAndMissingProsecutorDefendantID() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockCaseLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), null, getDefendants(), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoProblemWhenCaseLevelDocumentAndInvalidProsecutorDefendantID() {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(getMockCaseLevelDocumentMetadataReferenceDataList());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRule
                .validate(new CaseDocumentWithReferenceData(randomUUID(), false, material().build(), INVALID_PROSECUTOR_DEFENDANT_ID, getDefendants(), VALID_DOCUMENT_TYPE, false, false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    private List<Defendant> getDefendants() {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant()
                .withProsecutorDefendantReference(VALID_PROSECUTOR_DEFENDANT_ID)
                .withId(randomUUID().toString())
                .build());
        return defendants;
    }

    private List<DocumentTypeAccessReferenceData> getMockCaseLevelDocumentMetadataReferenceDataList() {
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = new ArrayList<>();
        documentMetadataReferenceDataList.add(documentTypeAccessReferenceData()
                .withSection(VALID_DOCUMENT_TYPE)
                .withDocumentCategory(CASE_LEVEL)
                .withId(randomUUID())
                .build());
        return documentMetadataReferenceDataList;
    }

    private List<DocumentTypeAccessReferenceData> getMockDefendantLevelDocumentMetadataReferenceDataList() {
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = new ArrayList<>();
        documentMetadataReferenceDataList.add(documentTypeAccessReferenceData()
                .withSection(VALID_DOCUMENT_TYPE)
                .withDocumentCategory(DEFENDANT_LEVEL)
                .withId(randomUUID())
                .build());
        return documentMetadataReferenceDataList;
    }
}
