package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language.E;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;

import java.util.UUID;

import javax.inject.Inject;

public class ProsecutionCaseFileDefendantToSjpDefendantConverter implements Converter<Defendant, uk.gov.justice.json.schemas.domains.sjp.commands.Defendant> {
    @Inject
    private IntegerGenderToSjpGenderConverter integerGenderToSjpGenderConverter;

    @Inject
    private ProsecutionCaseFileAddressToSjpAddressConverter prosecutionAddressToSjpAddressConverter;

    @Inject
    private ProsecutionCaseFileOffenceToSjpOffenceConverter prosecutionCaseFileOffenceToSjpOffenceConverter;

    @Inject
    private ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter prosecutionCaseFileContactDetailsToSjpContactDetailsConverter;

    @Inject
    private ProsecutionCaseFileLanguageToSjpLanguageConverter prosecutionCaseFileLanguageToSjpLanguageConverter;

    @Override
    public uk.gov.justice.json.schemas.domains.sjp.commands.Defendant convert(final Defendant source) {
        if (nonNull(source.getIndividual())) {
            final PersonalInformation personalInformation = source.getIndividual().getPersonalInformation();
            return uk.gov.justice.json.schemas.domains.sjp.commands.Defendant.defendant()
                    .withId(UUID.fromString(source.getId()))
                    .withTitle(personalInformation.getTitle())
                    .withNumPreviousConvictions(source.getNumPreviousConvictions())
                    .withLastName(personalInformation.getLastName())
                    .withFirstName(personalInformation.getFirstName())
                    .withLegalEntityName(source.getOrganisationName())
                    .withGender(integerGenderToSjpGenderConverter.convert(source.getIndividual().getSelfDefinedInformation().getGender()))
                    .withDateOfBirth(ofNullable(source.getIndividual().getSelfDefinedInformation().getDateOfBirth()).map(LocalDates::to).orElse(null))
                    .withAddress(prosecutionAddressToSjpAddressConverter.convert(personalInformation.getAddress()))
                    .withOffences(prosecutionCaseFileOffenceToSjpOffenceConverter.convert(source.getOffences()))
                    .withNationalInsuranceNumber(source.getIndividual().getNationalInsuranceNumber())
                    .withDriverNumber(source.getIndividual().getDriverNumber())
                    .withAsn(source.getAsn())
                    .withPncIdentifier(source.getPncIdentifier())
                    .withHearingLanguage(prosecutionCaseFileLanguageToSjpLanguageConverter.convert(source.getHearingLanguage()))
                    .withContactDetails(prosecutionCaseFileContactDetailsToSjpContactDetailsConverter.convert(personalInformation.getContactDetails()))
                    .build();
        } else {
            return uk.gov.justice.json.schemas.domains.sjp.commands.Defendant.defendant()
                    .withId(UUID.fromString(source.getId()))
                    .withNumPreviousConvictions(source.getNumPreviousConvictions())
                    .withLegalEntityName(source.getOrganisationName())
                    .withAddress(prosecutionAddressToSjpAddressConverter.convert(source.getAddress()))
                    .withOffences(prosecutionCaseFileOffenceToSjpOffenceConverter.convert(source.getOffences()))
                    .withAsn(source.getAsn())
                    .withHearingLanguage(prosecutionCaseFileLanguageToSjpLanguageConverter.convert(E))
                    .withContactDetails(prosecutionCaseFileContactDetailsToSjpContactDetailsConverter.convert(uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails.contactDetails()
                            .withMobile(source.getTelephoneNumberBusiness())
                            .withPrimaryEmail(source.getEmailAddress1())
                            .withSecondaryEmail(source.getEmailAddress2())
                            .withWork(source.getTelephoneNumberBusiness())
                            .build()))
                    .build();
        }
    }
}