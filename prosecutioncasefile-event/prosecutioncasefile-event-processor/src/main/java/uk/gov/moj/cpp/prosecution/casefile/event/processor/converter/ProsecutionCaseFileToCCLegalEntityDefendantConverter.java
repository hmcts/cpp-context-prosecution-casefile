package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

public class ProsecutionCaseFileToCCLegalEntityDefendantConverter implements Converter<Defendant, LegalEntityDefendant> {
    @Override
    public LegalEntityDefendant convert(final Defendant defendant) {
        if (null == defendant.getOrganisationName()) {
            return null;
        }
        return LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(buildOrganisation(defendant))
                .build();
    }

    private Organisation buildOrganisation(final Defendant defendant) {
        return Organisation.organisation()
                .withName(defendant.getOrganisationName())
                .withIncorporationNumber(null)
                .withContact(buildContactNumber(defendant))
                .withAddress(null != defendant.getAddress() ? buildAddress(defendant) : null)
                .withRegisteredCharityNumber(null)
                .build();
    }

    private Address buildAddress(final Defendant defendant) {
        return Address.address().withAddress1(defendant.getAddress().getAddress1())
                .withAddress2(defendant.getAddress().getAddress2())
                .withAddress3(defendant.getAddress().getAddress3())
                .withAddress4(defendant.getAddress().getAddress4())
                .withAddress5(defendant.getAddress().getAddress5())
                .withPostcode(defendant.getAddress().getPostcode())
                .build();
    }

    private ContactNumber buildContactNumber(final Defendant defendant) {

        if (isEmpty(defendant.getEmailAddress1()) && isEmpty(defendant.getEmailAddress2()) && isEmpty(defendant.getTelephoneNumberBusiness())) {
            return null;
        }

        return ContactNumber.contactNumber()
                .withPrimaryEmail(defendant.getEmailAddress1())
                .withSecondaryEmail(defendant.getEmailAddress2())
                .withWork(defendant.getTelephoneNumberBusiness())
                .build();
    }
}
