package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

@SuppressWarnings("squid:S1488")
public class ApplicationParameters {

    @Inject
    @Value(key = "missing_mandatory_fields", defaultValue = "f12a0920-5253-4fff-a5be-f64b33c5f576")
    private String pocaSuccessEmailTemplateId;

    public String getEmailTemplateId() {
        return pocaSuccessEmailTemplateId;
    }
}
