package uk.gov.moj.cpp.enterpriseid.generator;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class EnterpriseIdGeneratorImplTest {

    private static final String CHARACTER_SET_STRING = "BCDFGHJKLMNPQRSTVWXYZ123456789";

    private EnterpriseIdGeneratorImpl enterpriseIdGenerator = new EnterpriseIdGeneratorImpl();


    @Test
    public void shouldGenerateRandomString() {
        assertThat(enterpriseIdGenerator.enterpriseId(), is(not(equalTo(enterpriseIdGenerator.enterpriseId()))));
    }

    @Test
    public void shouldGenerateUniqueIds() {
        final Set<String> ids = new HashSet<>();
        final int idsToGenerate = 100000;

        for (int i = 0; i < idsToGenerate; i++) {
            final String id = enterpriseIdGenerator.enterpriseId();
            ids.add(id);
            assertThat(enterpriseIdGenerator.hasValidChecksumCharacter(id), is(true));
        }

        assertThat(format("Not all generated Ids were unique, only %d of %d were unique", ids.size(), idsToGenerate), ids.size(), equalTo(idsToGenerate));
    }

    @Test
    public void shouldGenerateRandomStringWithoutVowels() {
        final String enterpriseId = enterpriseIdGenerator.enterpriseId();

        assertThat(format("The generated string contained an A: %s", enterpriseId), enterpriseId.contains("A"), is(false));
        assertThat(format("The generated string contained an E: %s", enterpriseId), enterpriseId.contains("E"), is(false));
        assertThat(format("The generated string contained an I: %s", enterpriseId), enterpriseId.contains("I"), is(false));
        assertThat(format("The generated string contained an O: %s", enterpriseId), enterpriseId.contains("O"), is(false));
        assertThat(format("The generated string contained an U: %s", enterpriseId), enterpriseId.contains("U"), is(false));
    }

    @Test
    public void shouldHaveFinalCharacaterAsChecksum() {
        final String enterpriseId = enterpriseIdGenerator.enterpriseId();

        final char[] enterpriseIdChars = enterpriseId.toCharArray();
        final int generatedStringLength = enterpriseIdChars.length - 1;

        int checksum = 0;
        for (int i = 0; i < generatedStringLength; i++) {
            final int position = getPosition(enterpriseIdChars[i]);
            checksum += position;
        }
        assertThat(getPosition(enterpriseIdChars[generatedStringLength]), is(equalTo(checksum % generatedStringLength)));
    }

    @Test
    public void shouldValidateEnterpriseId() {
        assertThat("Valid enterprise id was not validated", enterpriseIdGenerator.hasValidChecksumCharacter("4WYCF4KV5QGM"), equalTo(true));
    }

    @Test
    public void shouldNotValidateEnterpriseIdWithInvalidLength() {
        assertThat("Enterprise Id with invalid length was validated", enterpriseIdGenerator.hasValidChecksumCharacter("B"), equalTo(false));
    }

    @Test
    public void shouldNotValidateEnterpriseIdWithInvalidChecksumCharacter() {
        assertThat("Enterprise Id with invalid check character was validated", enterpriseIdGenerator.hasValidChecksumCharacter("4WYCF4KV5QGP"), equalTo(false));
    }

    @Test
    public void shouldNotValidateEnterpriseIdWithInvalidCharacter() {
        assertThat("Enterprise Id with invalid character was validated", enterpriseIdGenerator.hasValidChecksumCharacter("EEEEEEEEEEEE"), equalTo(false));
    }

    private int getPosition(final char enterpriseIdChar) {
        return CHARACTER_SET_STRING.indexOf(enterpriseIdChar);
    }
}
