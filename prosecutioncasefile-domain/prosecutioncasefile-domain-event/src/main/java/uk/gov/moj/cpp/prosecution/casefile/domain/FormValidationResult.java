package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

public class FormValidationResult {

    private JsonObject petFormData;

    private JsonObject formData;

    private JsonObject formDefendants;

    private JsonObject petDefendants;

    private List<Problem> errorList;

    private SubmissionStatus submissionStatus;


    public JsonObject getPetFormData() {
        return petFormData;
    }

    public JsonObject getPetDefendants() {
        return petDefendants;
    }

    public List<Problem> getErrorList() {
        if (CollectionUtils.isEmpty(errorList)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(errorList);
    }

    public SubmissionStatus getSubmissionStatus() {
        return submissionStatus;
    }

    public static Builder formValidationResult() {
        return new Builder();
    }

    public JsonObject getFormData() {
        return formData;
    }

    public JsonObject getFormDefendants() {
        return formDefendants;
    }

    public static class Builder {
        JsonObject petFormData;
        JsonObject petDefendants;
        List<Problem> errorList;
        SubmissionStatus submissionStatus;

        JsonObject formData;

        JsonObject formDefendants;

        private Builder() {
        }


        public Builder withPetFormData(JsonObject petFormData) {
            this.petFormData = petFormData;
            return this;
        }

        public Builder withPetDefendants(JsonObject petDefendants) {
            this.petDefendants = petDefendants;
            return this;
        }

        public Builder withErrorList(List<Problem> errorList) {
            this.errorList = errorList;
            return this;
        }

        public Builder withSubmissionStatus(SubmissionStatus submissionStatus) {
            this.submissionStatus = submissionStatus;
            return this;
        }

        public Builder withFormDefendants(JsonObject formDefendants) {
            this.formDefendants = formDefendants;
            return this;
        }

        public Builder withFormData(JsonObject bcmFormData) {
            this.formData = bcmFormData;
            return this;
        }

        public FormValidationResult build() {
            final FormValidationResult formValidationResult = new FormValidationResult();
            formValidationResult.petDefendants = this.petDefendants;
            formValidationResult.errorList = this.errorList;
            formValidationResult.petFormData = this.petFormData;
            formValidationResult.submissionStatus = this.submissionStatus;
            formValidationResult.formData = this.formData;
            formValidationResult.formDefendants = this.formDefendants;
            return formValidationResult;
        }
    }
}
