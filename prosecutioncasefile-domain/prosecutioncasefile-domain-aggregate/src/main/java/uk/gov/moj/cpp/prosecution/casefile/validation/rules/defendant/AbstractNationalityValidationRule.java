package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Optional;

abstract class AbstractNationalityValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    protected abstract String getNationality(SelfDefinedInformation selfDefinedInformation);

    protected abstract ProblemCode getProblemCode();

    protected abstract FieldName getProblemCodeFieldName();

    private boolean hasNoNationalityInformation(Defendant defendantWithReferenceData) {
        return (defendantWithReferenceData.getIndividual() == null || defendantWithReferenceData.getIndividual().getSelfDefinedInformation() == null ||
                isEmpty(getNationality(defendantWithReferenceData.getIndividual().getSelfDefinedInformation())));
    }

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (hasNoNationalityInformation(defendantWithReferenceData.getDefendant())) {
            return VALID;
        }

        final String nationality = getNationality(defendantWithReferenceData.getDefendant().getIndividual().getSelfDefinedInformation());
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        Optional<ReferenceDataCountryNationality> referenceDataCountryNationalityOptional = referenceDataVO.getCountryNationalityReferenceData().stream()
                .filter(referenceDataCountryNationality -> isNationalityMatch(nationality, referenceDataCountryNationality))
                .findAny();

        if (referenceDataCountryNationalityOptional.isPresent()) {
            return VALID;
        }

        if (referenceDataQueryService == null) {
            return newValidationResult(of(newProblem(getProblemCode(), new ProblemValue(null, getProblemCodeFieldName().getValue(), nationality))));
        }

        referenceDataCountryNationalityOptional = referenceDataQueryService.retrieveCountryNationality().stream()
                .filter(country -> isNationalityMatch(nationality, country)).findAny();
        if (referenceDataCountryNationalityOptional.isPresent()) {
            referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationalityOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(getProblemCode(), new ProblemValue(null, getProblemCodeFieldName().getValue(), nationality))));
        }
    }

    public boolean isNationalityMatch(final String nationality, final ReferenceDataCountryNationality referenceDataCountryNationality) {
        if (nationality.equals(referenceDataCountryNationality.getIsoCode())) {
            return true;
        }

        return referenceDataCountryNationality.getCjsCode() != null && nationality.equals(referenceDataCountryNationality.getCjsCode());
    }

}
