package uk.gov.moj.cpp.enterpriseid.generator;

import java.security.SecureRandom;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Generates a 12 character random ID, containing only uppercase characters A-Z and 1-9, excluding
 * vowels (A, E, I, O, U) and with the final character being a checksum character.
 *
 * The final checksum character is CHARACTER_SET[sum_of_char_indices % GENERATED_ID_LENGTH].
 */
@ApplicationScoped
public class EnterpriseIdGeneratorImpl implements EnterpriseIdGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int GENERATED_ID_LENGTH = 11;

    private static final char[] CHARACTER_SET = {'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T',
            'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    private static final String CHARACTER_SET_STRING = new String(CHARACTER_SET);
    private static final int INVALID_CHARACTER = -1;
    private static final int CHARSET_SIZE = CHARACTER_SET.length;

    @Override
    public String enterpriseId() {
        final char[] enterpriseId = new char[GENERATED_ID_LENGTH + 1];
        int checksum = 0;

        for (int i = 0; i < GENERATED_ID_LENGTH; i++) {
            final int charIndex = secureRandom.nextInt(CHARSET_SIZE);
            checksum += charIndex;
            enterpriseId[i] = CHARACTER_SET[charIndex];
        }

        enterpriseId[GENERATED_ID_LENGTH] = CHARACTER_SET[checksum % GENERATED_ID_LENGTH];

        return new String(enterpriseId);
    }

    boolean hasValidChecksumCharacter(final String enterpriseId) {

        if (enterpriseId.length() != GENERATED_ID_LENGTH + 1) {
            return false;
        }

        final char[] enterpriseIdChars = enterpriseId.toCharArray();

        int checksum = 0;
        for (int i = 0; i < GENERATED_ID_LENGTH; i++) {
            final int position = getPosition(enterpriseIdChars[i]);
            if (position == INVALID_CHARACTER) {
                return false;
            }
            checksum += position;
        }

        return getPosition(enterpriseIdChars[GENERATED_ID_LENGTH]) == checksum % GENERATED_ID_LENGTH;
    }

    private int getPosition(final char enterpriseIdChar) {
        return CHARACTER_SET_STRING.indexOf(enterpriseIdChar);
    }
}
