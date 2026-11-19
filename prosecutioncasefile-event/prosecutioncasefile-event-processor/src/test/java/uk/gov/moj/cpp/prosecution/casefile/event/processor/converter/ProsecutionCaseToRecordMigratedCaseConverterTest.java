package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MigratedCaseDefendant;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordMigratedCase;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

class ProsecutionCaseToRecordMigratedCaseConverterTest {

    private static final String REFERENCE = "DEF-REF-1";
    private static final String ASN = "ASN-0001";

    private final ProsecutionCaseToRecordMigratedCaseConverter converter = new ProsecutionCaseToRecordMigratedCaseConverter();

    private final UUID defendantId = randomUUID();

    @Test
    void convert_personDefendantWithReferenceAndAsn_should_mapBothAndName() {
        final MigratedCaseDefendant defendant = convertSingle(createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionAuthorityReference", REFERENCE)
                .add("personDefendant", personDefendant(ASN)));

        assertThat(defendant.getId(), is(defendantId));
        assertThat(defendant.getProsecutorDefendantReference(), is(REFERENCE));
        assertThat(defendant.getAsn(), is(ASN));
        assertThat(defendant.getFirstName(), is("Jane"));
        assertThat(defendant.getLastName(), is("Doe"));
        assertThat(defendant.getOrganisationName(), is(nullValue()));
    }

    @Test
    void convert_personDefendantWithAsnOnly_should_mapAsnWithoutReference() {
        final MigratedCaseDefendant defendant = convertSingle(createObjectBuilder()
                .add("id", defendantId.toString())
                .add("personDefendant", personDefendant(ASN)));

        assertThat(defendant.getAsn(), is(ASN));
        assertThat(defendant.getProsecutorDefendantReference(), is(nullValue()));
    }

    @Test
    void convert_personDefendantWithReferenceOnly_should_mapReferenceWithoutAsn() {
        final MigratedCaseDefendant defendant = convertSingle(createObjectBuilder()
                .add("id", defendantId.toString())
                .add("prosecutionAuthorityReference", REFERENCE)
                .add("personDefendant", personDefendant(null)));

        assertThat(defendant.getProsecutorDefendantReference(), is(REFERENCE));
        assertThat(defendant.getAsn(), is(nullValue()));
    }

    @Test
    void convert_organisationDefendant_should_mapOrganisationName() {
        final MigratedCaseDefendant defendant = convertSingle(createObjectBuilder()
                .add("id", defendantId.toString())
                .add("legalEntityDefendant", createObjectBuilder()
                        .add("organisation", createObjectBuilder()
                                .add("name", "Example Ltd"))));

        assertThat(defendant.getOrganisationName(), is("Example Ltd"));
        assertThat(defendant.getFirstName(), is(nullValue()));
        assertThat(defendant.getLastName(), is(nullValue()));
        assertThat(defendant.getAsn(), is(nullValue()));
    }

    @Test
    void convert_migrationSourceSystemWithoutCaseIdentifier_should_buildCommandWithoutIdentifier() {
        final UUID caseId = randomUUID();
        final JsonObject prosecutionCase = createObjectBuilder()
                .add("id", caseId.toString())
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("prosecutionAuthorityId", randomUUID().toString())
                        .add("prosecutionAuthorityCode", "CPS")
                        .add("caseURN", "20GD0001234"))
                .add("defendants", createArrayBuilder().add(createObjectBuilder()
                        .add("id", defendantId.toString())))
                .add("migrationSourceSystem", createObjectBuilder()
                        .add("migrationSourceSystemName", "LIBRA"))
                .build();

        final RecordMigratedCase recordMigratedCase = converter.convert(prosecutionCase);

        assertThat(recordMigratedCase.getCaseId(), is(caseId));
        assertThat(recordMigratedCase.getMigrationSourceSystemName(), is("LIBRA"));
        assertThat(recordMigratedCase.getMigrationSourceSystemCaseIdentifier(), is(nullValue()));
        assertThat(recordMigratedCase.getDefendants().get(0).getId(), is(defendantId));
    }

    private MigratedCaseDefendant convertSingle(final JsonObjectBuilder defendant) {
        final JsonObject prosecutionCase = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("prosecutionAuthorityId", randomUUID().toString())
                        .add("prosecutionAuthorityCode", "CPS")
                        .add("caseURN", "20GD0001234"))
                .add("defendants", createArrayBuilder().add(defendant))
                .add("migrationSourceSystem", createObjectBuilder()
                        .add("migrationSourceSystemName", "LIBRA")
                        .add("migrationSourceSystemCaseIdentifier", "LIB-100002"))
                .build();

        return converter.convert(prosecutionCase).getDefendants().get(0);
    }

    private JsonObjectBuilder personDefendant(final String asn) {
        final JsonObjectBuilder personDefendant = createObjectBuilder()
                .add("personDetails", createObjectBuilder()
                        .add("firstName", "Jane")
                        .add("lastName", "Doe"));
        if (asn != null) {
            personDefendant.add("arrestSummonsNumber", asn);
        }
        return personDefendant;
    }
}
