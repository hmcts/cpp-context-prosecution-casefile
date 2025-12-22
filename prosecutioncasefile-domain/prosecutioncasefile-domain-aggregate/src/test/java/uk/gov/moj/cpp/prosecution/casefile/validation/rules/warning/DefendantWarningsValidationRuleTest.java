package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ON_CP;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.DefendantWarningsValidationRule.INSTANCE;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantWarningsValidationRuleTest {

    private static final String DOCUMENT_CATEGORY_DEFENDANT_LEVEL = "Defendant level";

    private static final String ASN = "ASN";
    private static final String DEFENDANT_ID = "6e70f5da-e513-498c-9d97-8eecb0883088";

    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldReturnErrorWhenAllFieldsExist() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("John")
                        .withDateOfBirth("2000-01-01")
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject);
        caseDocumentWithReferenceData.setDocumentCategory(DOCUMENT_CATEGORY_DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().addAll(getDuplicatedDefendants());


        final List<Problem> optionalProblem = INSTANCE.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().collect(Collectors.toList());

        assertThat(optionalProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(optionalProblem.get(1).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    @Test
    public void shouldReturnErrorWhenDOBDoesNotExist() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                        .withForename("John")
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject);
        caseDocumentWithReferenceData.setDocumentCategory(DOCUMENT_CATEGORY_DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().addAll(getDuplicatedDefendants());


        final List<Problem> optionalProblem = INSTANCE.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().collect(Collectors.toList());

        assertThat(optionalProblem.size(), is(2));
        assertThat(optionalProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
        assertThat(optionalProblem.get(1).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final DefendantSubject defendantSubject) {
        return new CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, null, ProsecutionCaseSubject.prosecutionCaseSubject().withDefendantSubject(defendantSubject).build(), null, null, new HashMap<>());
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
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Lord")
                                        .withFirstName("Johnnn")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build());
    }
}
