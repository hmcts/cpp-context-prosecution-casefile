package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.BACK_DUTY_AMOUNT_MISSING;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.BACK_DUTY_DATE_RANGE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OffenceBackDutyValidationRuleAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(DefendantWithReferenceData defendantWithReferenceData, ReferenceDataQueryService referenceDataQueryService) {
        if (isEmptyOffence(defendantWithReferenceData)) {
            return VALID;
        }

        final List<Offence> offenceList = defendantWithReferenceData.getDefendant().getOffences();

        final List<Problem> problemList = offenceList.stream()
                .filter(offence -> findMatchingBackDutyOffences(referenceDataQueryService, defendantWithReferenceData.getCaseDetails().getInitiationCode(), offence))
                .map(this::verifyBackDutyFields)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        return newValidationResult(problemList);
    }

    private boolean findMatchingBackDutyOffences(final ReferenceDataQueryService referenceDataQueryService, final String initiationCode, final Offence offence) {
        final List<OffenceReferenceData> offenceReferenceDataList = referenceDataQueryService.retrieveOffenceData(offence, initiationCode).stream()
                .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode()))
                .filter(this::isBackDutyOffence)
                .collect(Collectors.toList());

        return !offenceReferenceDataList.isEmpty();
    }

    private List<Problem> verifyBackDutyFields(final Offence offence) {
        final Optional<Problem> backDutyProblem = getBackDutyValueValidation(offence);
        final Optional<Problem> backDutyDateProblems = getBackDutyDateValidations(offence);

        final List<Problem> problemList = new ArrayList<>(2);
        backDutyProblem.ifPresent(problemList::add);
        backDutyDateProblems.ifPresent(problemList::add);

        return problemList.isEmpty() ? null : problemList;
    }

    private Optional<Problem> getBackDutyValueValidation(final Offence offence) {
        if (offence.getBackDuty() == null) {
           final List<ProblemValue> valuesList = new ArrayList<>(getOffenceInfo(offence));
            valuesList.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.BACKDUTY_VALUE.getValue(), String.valueOf(offence.getBackDuty())));
            return Optional.of(newProblem(BACK_DUTY_AMOUNT_MISSING, valuesList));
        }
        return Optional.empty();
    }

    private Optional<Problem> getBackDutyDateValidations(final Offence offence) {
        if (offence.getBackDutyDateFrom() != null || offence.getBackDutyDateTo() != null) {
            final List<ProblemValue> valuesList = new ArrayList<>(getOffenceInfo(offence));
            if (offence.getBackDutyDateFrom() == null) {
                valuesList.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.BACKDUTY_FROMDATE.getValue(), "backDutyDateFrom"));
                return Optional.of(newProblem(BACK_DUTY_DATE_RANGE_INVALID, valuesList));
            } else if (offence.getBackDutyDateTo() == null) {
                valuesList.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.BACKDUTY_TODATE.getValue(), "backDutyDateTo"));
                return Optional.of(newProblem(BACK_DUTY_DATE_RANGE_INVALID, valuesList));
            } else {
                if (offence.getBackDutyDateTo().isBefore(offence.getBackDutyDateFrom())) {
                    valuesList.add(new ProblemValue(offence.getOffenceId().toString(), FieldName.BACKDUTY_FROMDATE_TODATE.getValue(), "back duty from date is after back duty to date"));
                    return Optional.of(newProblem(BACK_DUTY_DATE_RANGE_INVALID, valuesList));
                }
            }
        }
        return Optional.empty();
    }

    private List<ProblemValue> getOffenceInfo(Offence offence) {
        return Arrays.asList(
                new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_CODE.getValue(),
                        offence.getOffenceCode()),
                new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_SEQUENCE_NO.getValue(),
                        offence.getOffenceSequenceNumber().toString())
        );
    }

    private boolean isEmptyOffence(DefendantWithReferenceData defendantWithReferenceData) {
        return defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null
                || defendantWithReferenceData.getDefendant().getOffences().isEmpty();
    }

    private boolean isBackDutyOffence(OffenceReferenceData offenceReferenceData) {
        return (offenceReferenceData.getBackDuty() == null ? false : offenceReferenceData.getBackDuty());
    }
}
