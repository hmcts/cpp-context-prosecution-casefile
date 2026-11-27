package uk.gov.moj.cpp.prosecution.casefile.command.api.validator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.drools.core.util.StringUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.CaseStatus.REFERRED_FOR_COURT_HEARING;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.FinancialMeans;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.LegalEntityFinancialMeans;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleaType;
import uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnline;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class PleadOnlineValidator {

    private static final Map<String, List<String>> PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS = singletonMap(
            "FinancialMeansRequiredWhenPleadingGuilty",
            singletonList("Financial Means are required when you are pleading GUILTY"));
    private static final Map<String, List<String>> CASE_HAS_BEEN_REVIEWED = singletonMap(
            "CaseAlreadyReviewed",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    private static final Map<String, List<String>> PLEA_ALREADY_SUBMITTED = singletonMap(
            "PleaAlreadySubmitted",
            singletonList("Plea already submitted - Contact the Contact Centre if you need to change or discuss it"));
    private static final Set<String> PROHIBITED_CASE_STATES = new HashSet<>(Arrays.asList(COMPLETED.name(),
            REFERRED_FOR_COURT_HEARING.name()));
    private static final Map<String, List<String>> CASE_ADJOURNED_POST_CONVICTION = singletonMap(
            "CaseAdjournedPostConviction",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    private static final Map<String, List<String>> CASE_NOT_FOUND = singletonMap(
            "CaseNotFound",
            singletonList("Your case could not be found - Contact the Contact Centre if you need to discuss it"));


    /**
     * Rules: - Financial Means are mandatory when Plea is GUILTY
     */
    @SuppressWarnings("squid:S4274")
    public Map<String, List<String>> validate(final PleadOnline pleadOnline) {
        final boolean anyGuiltyPlea = pleadOnline.getOffences().stream()
                .map(Offence::getPlea)
                .anyMatch(PleaType.GUILTY::equals);

        if ((nonNull(pleadOnline.getPersonalDetails()) && anyGuiltyPlea && hasEmptyFinancialMeans(pleadOnline.getFinancialMeans())) ||
                (nonNull(pleadOnline.getLegalEntityDefendant()) && anyGuiltyPlea && hasEmptyLegalEntityFinancialMeans(pleadOnline.getLegalEntityFinancialMeans()))) {
            return PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS;
        }
        return emptyMap();
    }

    private static boolean hasEmptyFinancialMeans(final FinancialMeans financialMeans) {
        return financialMeans == null ||
                financialMeans.getBenefits() == null ||
                financialMeans.getIncome() == null ||
                isEmpty(financialMeans.getEmploymentStatus());
    }

    private static boolean hasEmptyLegalEntityFinancialMeans(final LegalEntityFinancialMeans financialMeans) {
        return financialMeans == null ||
                financialMeans.getOutstandingFines() == null ||
                financialMeans.getGrossTurnover() == null ||
                financialMeans.getTradingMoreThan12Months() == null ||
                financialMeans.getNetTurnover() == null ||
                financialMeans.getNumberOfEmployees() == null;
    }

    public Map<String, List<String>> validate(final JsonObject caseDetail) {
        if (caseStatusProhibited(caseDetail) ||
                !checkCaseDetailField(caseDetail, "completed", FALSE) ||
                !checkCaseDetailField(caseDetail, "assigned", FALSE) ||
                offenceHasPendingWithdrawal(caseDetail)) {
            return CASE_HAS_BEEN_REVIEWED;
        }
        if (checkCaseAdjournedTo(caseDetail) ||
                offenceWithConviction(caseDetail) ||
                offenceHasConvictionDate(caseDetail)) {
            return CASE_ADJOURNED_POST_CONVICTION;
        }

        if (caseAlreadyPleaded(caseDetail)) {
            return PLEA_ALREADY_SUBMITTED;
        }
        return emptyMap();
    }

    public Map<String, List<String>> validate(final ProsecutionCase caseDetail) {
        if (isNull(caseDetail)) {
            return CASE_NOT_FOUND;
        }
        if (caseStatusProhibited(caseDetail)) {
            return CASE_HAS_BEEN_REVIEWED;
        }
        if (offenceHasConvictionDate(caseDetail)) {
            return CASE_ADJOURNED_POST_CONVICTION;
        }

        if (caseAlreadyPleaded(caseDetail)) {
            return PLEA_ALREADY_SUBMITTED;
        }
        return emptyMap();
    }

    private boolean caseStatusProhibited(final JsonObject caseDetail) {
        return Optional.ofNullable(caseDetail.getString("status", null))
                .filter(PROHIBITED_CASE_STATES::contains)
                .isPresent();
    }

    private boolean caseStatusProhibited(final ProsecutionCase caseDetail) {
        return Optional.ofNullable(caseDetail.getCaseStatus())
                .filter(PROHIBITED_CASE_STATES::contains)
                .isPresent();
    }

    private boolean checkCaseAdjournedTo(final JsonObject caseDetail) {
        return Optional.ofNullable(caseDetail.getString("adjournedTo", null))
                .isPresent();
    }

    private boolean checkCaseDetailField(final JsonObject caseDetail, final String fieldName, final Boolean fieldValue) {
        return Optional.of(caseDetail.getBoolean(fieldName, FALSE))
                .filter(fieldValue::equals)
                .isPresent();
    }

    private boolean offenceHasPendingWithdrawal(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .map(offence -> offence.getBoolean("pendingWithdrawal", FALSE))
                .anyMatch(TRUE::equals);
    }

    private boolean offenceWithConviction(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("conviction", null) != null);
    }

    private boolean offenceHasConvictionDate(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("convictionDate", null) != null);
    }

    private boolean offenceHasConvictionDate(final ProsecutionCase caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> nonNull(offence.getConvictionDate()));
    }

    private boolean caseAlreadyPleaded(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("plea", null) != null);
    }

    private boolean caseAlreadyPleaded(final ProsecutionCase caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> nonNull(offence.getPlea()));
    }

    private Stream<JsonObject> getOffences(final JsonObject caseDetail) {
        return caseDetail.getJsonObject("defendant")
                .getJsonArray("offences")
                .getValuesAs(JsonObject.class)
                .stream();
    }

    private Stream<uk.gov.justice.core.courts.Offence> getOffences(final ProsecutionCase caseDetail) {
        return caseDetail.getDefendants()
                .stream().flatMap(defendant -> defendant.getOffences().stream());
    }

}