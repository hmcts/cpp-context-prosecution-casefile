package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

public class SelfDefinedInformationToSelfDefinedInformationDetails implements Converter<SelfDefinedInformation, SelfDefinedInformationDetails> {

    @Override
    public SelfDefinedInformationDetails convert(final SelfDefinedInformation selfDefinedInformation) {
        return new SelfDefinedInformationDetails(selfDefinedInformation.getAdditionalNationality(),
                selfDefinedInformation.getDateOfBirth(),
                selfDefinedInformation.getEthnicity(),
                selfDefinedInformation.getGender(),
                selfDefinedInformation.getNationality());
    }

}
