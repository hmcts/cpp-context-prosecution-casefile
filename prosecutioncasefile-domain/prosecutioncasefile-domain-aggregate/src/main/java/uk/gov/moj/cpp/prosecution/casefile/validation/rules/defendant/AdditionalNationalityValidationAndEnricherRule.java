package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;

import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ADDITIONAL_NATIONALITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_ADDITIONAL_NATIONALITY;

public class AdditionalNationalityValidationAndEnricherRule extends AbstractNationalityValidationRule {

    @Override
    protected String getNationality(SelfDefinedInformation selfDefinedInformation) {
        return selfDefinedInformation.getAdditionalNationality();
    }

    @Override
    protected ProblemCode getProblemCode() {
        return DEFENDANT_ADDITIONAL_NATIONALITY_INVALID;
    }

    @Override
    protected FieldName getProblemCodeFieldName() {
        return DEFENDANT_ADDITIONAL_NATIONALITY;
    }
}
