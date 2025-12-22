package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person;

import java.time.LocalDate;
import java.util.function.Function;

public class PersonMapper {

    public static final Function<Person, uk.gov.justice.core.courts.Person> convertPerson
            = sourcePerson -> ofNullable(sourcePerson)
            .map(person -> uk.gov.justice.core.courts.Person.person()
                    .withTitle(person.getTitle())
                    .withFirstName(person.getFirstName())
                    .withMiddleName(person.getMiddleName())
                    .withLastName(person.getLastName())
                    .withDateOfBirth(localDateToString(person.getDateOfBirth()))
                    .withGender(Gender.valueFor(person.getGender().toString()).orElse(null))
                    .withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds())
                    .withDocumentationLanguageNeeds(getHearingLanguageNeeds(person.getDocumentationLanguageNeeds()))
                    .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                    .withSpecificRequirements(person.getSpecificRequirements())
                    .withHearingLanguageNeeds(getHearingLanguageNeeds(person.getHearingLanguageNeeds()))
                    .withAddress(AddressMapper.convertAddress.apply(person.getAddress()))
                    .withContact(ContactMapper.convertContact.apply(person.getContact()))
                    .build()).orElse(null);

    private PersonMapper() {
    }

    private static HearingLanguage getHearingLanguageNeeds(final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.HearingLanguage hearingLanguage) {
        return nonNull(hearingLanguage) ? HearingLanguage.valueOf(hearingLanguage.toString()) : null;
    }

    private static String localDateToString(final LocalDate localDate) {
        return nonNull(localDate) ? localDate.toString() : null;
    }

}
