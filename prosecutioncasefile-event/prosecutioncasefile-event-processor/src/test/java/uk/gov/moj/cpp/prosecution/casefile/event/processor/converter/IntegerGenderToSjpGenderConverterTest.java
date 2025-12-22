package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IntegerGenderToSjpGenderConverterTest {

    @InjectMocks
    private IntegerGenderToSjpGenderConverter integerGenderToSjpGenderConverter;

    @Test
    public void shouldConvertIntegerGenderToSjpGender() {

        final uk.gov.justice.json.schemas.domains.sjp.Gender sjpGenderFemale =  integerGenderToSjpGenderConverter.convert(Gender.FEMALE);
        final uk.gov.justice.json.schemas.domains.sjp.Gender sjpGenderNotKnown = integerGenderToSjpGenderConverter.convert(Gender.NOT_KNOWN);
        final uk.gov.justice.json.schemas.domains.sjp.Gender sjpGenderUnSpecified = integerGenderToSjpGenderConverter.convert(Gender.NOT_SPECIFIED);

        assertThat(sjpGenderFemale, instanceOf(uk.gov.justice.json.schemas.domains.sjp.Gender.class));
        assertTrue(sjpGenderFemale.toString().equalsIgnoreCase(Gender.FEMALE.toString()));
        assertThat(sjpGenderNotKnown, instanceOf(uk.gov.justice.json.schemas.domains.sjp.Gender.class));
        assertThat(sjpGenderUnSpecified, instanceOf(uk.gov.justice.json.schemas.domains.sjp.Gender.class));

    }

}