package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationCase;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CaseDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class CaseReferenceValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    @Inject
    private CaseDetailsService caseDetailsService;

    public CaseReferenceValidationRule() {
        super(ValidationError.CASE_NOT_FOUND);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        final List<CourtApplicationCase> courtApplicationCases = submitApplication.getCourtApplication().getCourtApplicationCases();

        if (isNotEmpty(courtApplicationCases) && nonNull(additionalInformation)) {
            final List<String> inputCaseUrns = courtApplicationCases.stream()
                    .map(CourtApplicationCase::getCaseURN).collect(Collectors.toList());

            if (nonNull(additionalInformation.getProsecutionCases()) && additionalInformation.getProsecutionCases().size() == inputCaseUrns.size()) {
                return Optional.empty();
            }
        }
        return Optional.of(getValidationError());
    }
}
