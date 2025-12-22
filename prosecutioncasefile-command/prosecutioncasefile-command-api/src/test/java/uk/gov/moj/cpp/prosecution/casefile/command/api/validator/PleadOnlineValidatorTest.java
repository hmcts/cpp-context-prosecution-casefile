package uk.gov.moj.cpp.prosecution.casefile.command.api.validator;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Plea.plea;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.FinancialMeans.financialMeans;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PersonalDetails.personalDetails;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleaType.GUILTY;
import static uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnline.pleadOnline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Benefits;
import uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnline;

import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.mockito.InjectMocks;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineValidatorTest {

    @InjectMocks
    PleadOnlineValidator pleadOnlineValidator;

    @Test
    public void shouldValidateWithPleadOnlinePayload() {
        final PleadOnline pleadOnline = pleadOnline()
                .withOffences(asList(offence()
                        .withPlea(GUILTY)
                        .build()))
                .withPersonalDetails(personalDetails()
                        .build())
                .withFinancialMeans(financialMeans()
                        .withBenefits(Benefits.benefits().build())
                        .withEmploymentStatus("Test")
                        .build())
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(pleadOnline);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("FinancialMeansRequiredWhenPleadingGuilty"), hasSize(1));
        assertThat(result.get("FinancialMeansRequiredWhenPleadingGuilty").get(0), is("Financial Means are required when you are pleading GUILTY"));
    }


    @Test
    public void shouldValidateWithProsecutionCasePayload_WhenProsecutionCaseIsNull() {
        final ProsecutionCase prosecutionCase = null;

        final Map<String, List<String>> result = pleadOnlineValidator.validate(prosecutionCase);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("CaseNotFound"), hasSize(1));
        assertThat(result.get("CaseNotFound").get(0), is("Your case could not be found - Contact the Contact Centre if you need to discuss it"));
    }


    @Test
    public void shouldValidateWithProsecutionCasePayload_WhenCaseStatusIsCompleted() {
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("COMPLETED")
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(prosecutionCase);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("CaseAlreadyReviewed"), hasSize(1));
        assertThat(result.get("CaseAlreadyReviewed").get(0), is("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    }

    @Test
    public void shouldValidateWithProsecutionCasePayload_WhenCaseAdjournedPostConviction() {
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("APPEALED")
                .withDefendants(asList(defendant()
                        .withOffences(asList(Offence.offence()
                                .withConvictionDate("2023-01-23")
                                .build()))
                        .build()))
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(prosecutionCase);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("CaseAdjournedPostConviction"), hasSize(1));
        assertThat(result.get("CaseAdjournedPostConviction").get(0), is("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    }


    @Test
    public void shouldValidateWithProsecutionCasePayload_WhenPleaAlreadySubmitted() {
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("APPEALED")
                .withDefendants(asList(defendant()
                        .withOffences(asList(Offence.offence()
                                .withPlea(plea().build())
                                .build()))
                        .build()))
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(prosecutionCase);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("PleaAlreadySubmitted"), hasSize(1));
        assertThat(result.get("PleaAlreadySubmitted").get(0), is("Plea already submitted - Contact the Contact Centre if you need to change or discuss it"));
    }

    @Test
    public void shouldValidateWithProsecutionCasePayload() {
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("APPEALED")
                .withDefendants(asList(defendant()
                        .withOffences(asList(Offence.offence()
                                .build()))
                        .build()))
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(prosecutionCase);
        assertThat(result.values(), hasSize(0));
    }


    @Test
    public void shouldValidateWithJsonPayload_WhenCaseAlreadyReviewed() {
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("status", "")
                .add("completed", false)
                .add("assigned", false)
                .add("defendant", Json
                        .createObjectBuilder()
                        .add("offences", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("pendingWithdrawal", true)
                                        .build())
                                .build()
                        )
                        .build())
                .build();
        final Map<String, List<String>> result = pleadOnlineValidator.validate(jsonObject);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("CaseAlreadyReviewed"), hasSize(1));
        assertThat(result.get("CaseAlreadyReviewed").get(0), is("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    }

    @Test
    public void shouldValidateWithJsonPayload_WhenCaseAdjournedPostConviction() {
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("status", "")
                .add("completed", false)
                .add("assigned", false)
                .add("defendant", Json
                        .createObjectBuilder()
                        .add("offences", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("pendingWithdrawal", false)
                                        .add("convictionDate", "2020-12-23")
                                        .build())
                                .build())
                        .build())
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(jsonObject);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("CaseAdjournedPostConviction"), hasSize(1));
        assertThat(result.get("CaseAdjournedPostConviction").get(0), is("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    }

    @Test
    public void shouldValidateWithJsonPayload_WhenPleaAlreadySubmitted() {
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("status", "")
                .add("completed", false)
                .add("assigned", false)
                .add("defendant", Json
                        .createObjectBuilder()
                        .add("offences", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("pendingWithdrawal", false)
                                        .add("plea", "Guilty")
                                        .build())
                                .build())
                        .build())
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(jsonObject);
        assertThat(result.values(), hasSize(1));
        assertThat(result.get("PleaAlreadySubmitted"), hasSize(1));
        assertThat(result.get("PleaAlreadySubmitted").get(0), is("Plea already submitted - Contact the Contact Centre if you need to change or discuss it"));
    }

    @Test
    public void shouldValidateWithJsonPayload() {
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("status", "")
                .add("completed", false)
                .add("assigned", false)
                .add("defendant", Json
                        .createObjectBuilder()
                        .add("offences", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("pendingWithdrawal", false)
                                        .build())
                                .build())
                        .build())
                .build();

        final Map<String, List<String>> result = pleadOnlineValidator.validate(jsonObject);
        assertThat(result.values(), hasSize(0));
    }


}