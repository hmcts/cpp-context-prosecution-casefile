package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.MigratedCaseDefendant.migratedCaseDefendant;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordMigratedCase.recordMigratedCase;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MigratedCaseDefendant;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordMigratedCase;

import java.util.List;

import javax.json.JsonObject;

public class ProsecutionCaseToRecordMigratedCaseConverter implements Converter<JsonObject, RecordMigratedCase> {

    private static final String FIELD_ID = "id";

    @Override
    public RecordMigratedCase convert(final JsonObject prosecutionCase) {
        final JsonObject migrationSourceSystem = prosecutionCase.getJsonObject("migrationSourceSystem");

        return recordMigratedCase()
                .withCaseId(fromString(prosecutionCase.getString(FIELD_ID)))
                .withProsecutionCaseReference(prosecutionCaseReference(prosecutionCase.getJsonObject("prosecutionCaseIdentifier")))
                .withMigrationSourceSystemName(migrationSourceSystem.getString("migrationSourceSystemName"))
                .withMigrationSourceSystemCaseIdentifier(migrationSourceSystem.getString("migrationSourceSystemCaseIdentifier", null))
                .withDefendants(defendants(prosecutionCase))
                .build();
    }

    // prosecutionCaseIdentifier is oneOf caseURN / prosecutionAuthorityReference in core-domain
    private String prosecutionCaseReference(final JsonObject prosecutionCaseIdentifier) {
        return prosecutionCaseIdentifier.containsKey("caseURN")
                ? prosecutionCaseIdentifier.getString("caseURN")
                : prosecutionCaseIdentifier.getString("prosecutionAuthorityReference");
    }

    private List<MigratedCaseDefendant> defendants(final JsonObject prosecutionCase) {
        return prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                .map(this::defendant)
                .collect(toList());
    }

    private MigratedCaseDefendant defendant(final JsonObject defendant) {
        final MigratedCaseDefendant.Builder builder = migratedCaseDefendant()
                .withId(fromString(defendant.getString(FIELD_ID)))
                .withProsecutorDefendantReference(defendant.getString("prosecutionAuthorityReference", null));

        final JsonObject personDefendant = defendant.getJsonObject("personDefendant");
        if (nonNull(personDefendant)) {
            builder.withAsn(personDefendant.getString("arrestSummonsNumber", null));
            final JsonObject personDetails = personDefendant.getJsonObject("personDetails");
            if (nonNull(personDetails)) {
                builder.withFirstName(personDetails.getString("firstName", null))
                        .withLastName(personDetails.getString("lastName", null));
            }
        }

        final JsonObject legalEntityDefendant = defendant.getJsonObject("legalEntityDefendant");
        if (nonNull(legalEntityDefendant) && nonNull(legalEntityDefendant.getJsonObject("organisation"))) {
            builder.withOrganisationName(legalEntityDefendant.getJsonObject("organisation").getString("name", null));
        }

        return builder.build();
    }
}
