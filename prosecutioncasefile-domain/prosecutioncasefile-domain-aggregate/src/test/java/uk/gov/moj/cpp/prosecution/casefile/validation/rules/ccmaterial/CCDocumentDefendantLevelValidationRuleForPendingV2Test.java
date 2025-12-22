package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ID_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ON_CP;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCDocumentDefendantLevelValidationRuleForPendingV2Test {

    private static final String DEFENDANT_LEVEL = "Defendant level";
    private static final String CASE_LEVEL = "Case level";
    private static final String APPLICATIONS = "Applications";
    private static final String VALID_DOCUMENT_TYPE = "Valid Document Type";

    private static final String DEFENDANT_ID = "6e70f5da-e513-498c-9d97-8eecb0883088";
    private static final String ASN = "ASN";
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private CCDocumentDefendantLevelValidationRuleForPendingV2 ccDocumentDefendantLevelValidationRuleForPendingV2 = new CCDocumentDefendantLevelValidationRuleForPendingV2();

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsCaseLevel() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(CASE_LEVEL);

        final Optional<Problem> optionalProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsApplications() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(APPLICATIONS);

        final Optional<Problem> optionalProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDefendantIsOrganisation() {

        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);

        final Optional<Problem> optionalProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDefendantIdMissingForDefendantLevelDocument() {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(DEFENDANT_LEVEL);

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(DEFENDANT_ID_REQUIRED, "prosecutorDefendantId", "");

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    @Test
    public void shouldReturnValidWhenDefendantIsDuplicated() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("John")
                        .withDateOfBirth("2000-01-01")
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().addAll(getDuplicatedDefendants());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDefendantIsInvalidAndCpsDefendantIdIsNotNull() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().withCpsDefendantId(DEFENDANT_ID).build();
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().add(defendant().withProsecutorDefendantReference(randomUUID().toString()).build());

        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();

        assertThat(actualProblem.size(), is(1));
        assertThat(actualProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    @Test
    public void shouldReturnProblemWhenDefendantIsInvalidAndProsecutorDefendantIdIsNotNull() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().withProsecutorDefendantId(DEFENDANT_ID).build();
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().add(defendant().withId(randomUUID().toString()).build());

        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();

        assertThat(actualProblem.size(), is(1));
        assertThat(actualProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    @Test
    public void shouldReturnValidWhenDefendantIdIsNotMatchedButAsnIsMatched() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build()).withAsn(ASN).build();
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().add(defendant().withAsn(ASN).withId(randomUUID().toString()).build());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDefendantIdDoesNotNotExistButAsnIsExist() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().withAsn(ASN).build();
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().add(defendant().withAsn(ASN).withId(randomUUID().toString()).build());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void ShouldReturnProblemWhenPersonalInformationDoesNotExistAndIdIsInvalid(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("Johnn")
                        .withDateOfBirth("2000-01-01")
                        .withCpsDefendantId(DEFENDANT_ID)
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithIndividual());
        caseDocumentWithReferenceData.getValidDefendantIds().clear();
        caseDocumentWithReferenceData.setDocumentCategory(DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.setDocumentType(VALID_DOCUMENT_TYPE);

        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();

        assertThat(actualProblem.size(), is(3));
        assertThat(actualProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(actualProblem.get(1).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(actualProblem.get(2).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    @Test
    public void ShouldReturnValidWhenPersonalInformationDoesNotExistAndIdIsValidForCpsOrganisationDefendantDetails(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithIndividual());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void ShouldReturnValidWhenPersonalInformationDoesNotExistAndIdIsValidForPersonDefendantDetails(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .withCpsDefendantId(DEFENDANT_ID)
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithIndividual());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void ShouldReturnValidWhenPersonalInformationMatches(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("John")
                        .withDateOfBirth("2000-01-01")
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithIndividual());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void ShouldReturnProblemWhenPersonalInformationDoesNotMatch(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                 .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("Johnn")
                        .withDateOfBirth("2000-01-01")
                         .withCpsDefendantId(DEFENDANT_ID)
                        .build())
                .build();


        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithIndividual());
        caseDocumentWithReferenceData.setDocumentCategory(DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.setDocumentType(VALID_DOCUMENT_TYPE);

        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();

        assertThat(actualProblem.size(), is(3));
        assertThat(actualProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(actualProblem.get(1).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(actualProblem.get(2).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    @Test
    public void ShouldReturnValidWhenPersonalInformationMatchesForMultipleDefendants(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("John")
                        .withDateOfBirth("2000-01-01")
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithIndividual());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void ShouldReturnProblemWhenCaseDefendantDoesNotHaveIndividual(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("Johnn")
                        .withDateOfBirth("2000-01-01")
                        .withCpsDefendantId(DEFENDANT_ID)
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL, getDefendantsWithoutIndividual());
        caseDocumentWithReferenceData.setDocumentCategory(DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.setDocumentType(VALID_DOCUMENT_TYPE);


        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();


        assertThat(actualProblem.size(), is(2));
        assertThat(actualProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(actualProblem.get(1).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(String documentCategory) {
        return getCaseDocumentWithReferenceData(DefendantSubject.defendantSubject().build(), documentCategory);
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final DefendantSubject defendantSubject, String documentCategory) {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = new  CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, null, ProsecutionCaseSubject.prosecutionCaseSubject().withDefendantSubject(defendantSubject).build(), null, null, new HashMap<>());
        caseDocumentWithReferenceData.setDocumentCategory(documentCategory);
        return caseDocumentWithReferenceData;
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final DefendantSubject defendantSubject, String documentCategory, final List<Defendant> defendants) {
        final Map<String, UUID> validIds = new HashMap<>();
        validIds.put(DEFENDANT_ID, fromString(defendants.get(0).getId()));
        return new CaseDocumentWithReferenceData(randomUUID(), false, defendants, "documentType", false, false, null, ProsecutionCaseSubject.prosecutionCaseSubject().withDefendantSubject(defendantSubject).build(), null, null, validIds);
    }

    private List<Defendant> getDuplicatedDefendants() {
        return Arrays.asList(defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Mr")
                                        .withFirstName("John")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Mr")
                                        .withFirstName("John")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .build());
    }

    private List<Defendant> getDefendantsWithIndividual() {
        return Arrays.asList(defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Mr")
                                        .withFirstName("John")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Mis")
                                        .withFirstName("Suzan")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .build());
    }

    private List<Defendant> getDefendantsWithoutIndividual() {
        return Arrays.asList(defendant()
                        .withId(randomUUID().toString())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .build());
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
