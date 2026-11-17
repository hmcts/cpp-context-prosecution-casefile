package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.MigrationSourceSystem.migrationSourceSystem;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.MigratedCaseDefendant.migratedCaseDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;

import uk.gov.moj.cpp.prosecution.casefile.DocumentDetails;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MigratedCaseDefendant;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseReferredToCourtRecorded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatchPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MigratedCaseRecorded;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProsecutionCaseFileMigratedCaseTest {

    private static final String CASE_LEVEL_SECTION = "CASE_LEVEL_DOC";
    private static final String DEFENDANT_LEVEL_SECTION = "DEFENDANT_LEVEL_DOC";
    private static final String PROSECUTING_AUTHORITY = "CPS";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private static final String REFERENCE = "20GD0001234";
    private static final String LIBRA = "LIBRA";
    private static final String LIBRA_CASE_ID = "LIB-100002";

    private final ProsecutionCaseFile prosecutionCaseFile = new ProsecutionCaseFile();

    private final UUID caseId = randomUUID();
    private final UUID personDefendantId = randomUUID();
    private final UUID organisationDefendantId = randomUUID();

    private final List<MigratedCaseDefendant> defendants = asList(
            migratedCaseDefendant()
                    .withId(personDefendantId)
                    .withProsecutorDefendantReference("DEF-REF-1")
                    .withAsn("ASN-0001")
                    .withFirstName("Jane")
                    .withLastName("Doe")
                    .build(),
            migratedCaseDefendant()
                    .withId(organisationDefendantId)
                    .withOrganisationName("Example Ltd")
                    .build());

    @Test
    void recordMigratedCase_freshCase_should_emitMigratedCaseRecordedAndAcceptCase() {
        final List<Object> events = prosecutionCaseFile
                .recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, defendants)
                .collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(MigratedCaseRecorded.class));
        final MigratedCaseRecorded event = (MigratedCaseRecorded) events.get(0);
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getProsecutionCaseReference(), is(REFERENCE));
        assertThat(event.getMigrationSourceSystemName(), is(LIBRA));
        assertThat(event.getMigrationSourceSystemCaseIdentifier(), is(LIBRA_CASE_ID));
        assertThat(event.getDefendants(), is(defendants));

        assertThat(prosecutionCaseFile.getCaseId(), is(caseId));
        assertThat(prosecutionCaseFile.isProsecutionReceived(), is(true));
        assertThat(prosecutionCaseFile.isProsecutionAccepted(), is(true));
        assertThat(prosecutionCaseFile.getProsecutorCaseReference(), is(REFERENCE));

        final List<Defendant> recordedDefendants = prosecutionCaseFile.getDefendants();
        assertThat(recordedDefendants, hasSize(2));

        final Defendant person = recordedDefendants.get(0);
        assertThat(person.getId(), is(personDefendantId.toString()));
        assertThat(person.getProsecutorDefendantReference(), is("DEF-REF-1"));
        assertThat(person.getAsn(), is("ASN-0001"));
        assertThat(person.getIndividual().getPersonalInformation().getFirstName(), is("Jane"));
        assertThat(person.getIndividual().getPersonalInformation().getLastName(), is("Doe"));

        final Defendant organisation = recordedDefendants.get(1);
        assertThat(organisation.getId(), is(organisationDefendantId.toString()));
        assertThat(organisation.getOrganisationName(), is("Example Ltd"));
        assertThat(organisation.getIndividual(), is(nullValue()));
    }

    @Test
    void recordMigratedCase_alreadyRecorded_should_emitNothing() {
        prosecutionCaseFile.recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, defendants).collect(toList());

        final List<Object> events = prosecutionCaseFile
                .recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, defendants)
                .collect(toList());

        assertThat(events, is(empty()));
    }

    @Test
    void recordMigratedCase_caseAlreadyReceivedByPcf_should_emitNothing() {
        prosecutionCaseFile.apply(CaseReferredToCourtRecorded.caseReferredToCourtRecorded()
                .withCaseId(caseId)
                .withReferralReasonId(randomUUID())
                .build());

        final List<Object> events = prosecutionCaseFile
                .recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, defendants)
                .collect(toList());

        assertThat(events, is(empty()));
        assertThat(prosecutionCaseFile.isProsecutionAccepted(), is(false));
    }

    @Test
    void recordMigratedCase_libraCaseAlreadyReceivedViaMcc_should_emitNothingAndLeaveCaseUnchanged() {
        final String mccDefendantId = randomUUID().toString();
        prosecutionCaseFile.apply(ccCaseReceived()
                .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(prosecution()
                        .withChannel(MCC)
                        .withCaseDetails(caseDetails()
                                .withCaseId(caseId)
                                .withInitiationCode("J")
                                .withProsecutorCaseReference(REFERENCE)
                                .build())
                        .withDefendants(asList(Defendant.defendant().withId(mccDefendantId).build()))
                        .withMigrationSourceSystem(migrationSourceSystem()
                                .withMigrationSourceSystemName(LIBRA)
                                .withMigrationSourceSystemCaseIdentifier(LIBRA_CASE_ID)
                                .build())
                        .build()))
                .build());

        final List<Object> events = prosecutionCaseFile
                .recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, defendants)
                .collect(toList());

        assertThat(events, is(empty()));
        assertThat(prosecutionCaseFile.isProsecutionReceived(), is(true));
        assertThat(prosecutionCaseFile.isProsecutionAccepted(), is(false));
        assertThat(prosecutionCaseFile.getDefendants(), hasSize(1));
        assertThat(prosecutionCaseFile.getDefendants().get(0).getId(), is(mccDefendantId));
    }

    @Test
    void addIdpcCaseMaterial_twoRecordedDefendantsWithSameNames_should_raiseMatchPendingNotFail() {
        final UUID firstJaneDoeId = randomUUID();
        final UUID secondJaneDoeId = randomUUID();
        prosecutionCaseFile.recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, asList(
                migratedCaseDefendant().withId(firstJaneDoeId).withFirstName("Jane").withLastName("Doe").build(),
                migratedCaseDefendant().withId(secondJaneDoeId).withFirstName("Jane").withLastName("Doe").build()))
                .collect(toList());

        final List<Object> events = prosecutionCaseFile.addIdpcCaseMaterial(caseId, REFERENCE, randomUUID(), "IDPC",
                        uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant.defendant()
                                .withSurname("Doe")
                                .withForenames("Jane")
                                .withDob("1990-01-01")
                                .build())
                .collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(IdpcDefendantMatchPending.class));
    }

    @Test
    void addCpsMaterial_caseLevelAfterRecord_should_addMaterialNotPend() {
        recordLibraCase();

        final List<Object> events = addCpsMaterial(CASE_LEVEL_SECTION, null);

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(MaterialAdded.class));
        assertThat(((MaterialAdded) events.get(0)).getCaseId(), is(caseId));
    }

    @Test
    void addCpsMaterial_defendantLevelMatchedByReference_should_addMaterialForThatDefendant() {
        recordLibraCase();

        final List<Object> events = addCpsMaterial(DEFENDANT_LEVEL_SECTION, "DEF-REF-1");

        assertThat(events, hasSize(1));
        final MaterialAdded materialAdded = (MaterialAdded) events.get(0);
        assertThat(materialAdded.getDefendantId(), is(personDefendantId));
        assertThat(materialAdded.getDefendantName(), is("Jane Doe"));
    }

    @Test
    void addCpsMaterial_defendantLevelMatchedByAsn_should_addMaterialForThatDefendant() {
        prosecutionCaseFile.recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, asList(
                migratedCaseDefendant()
                        .withId(personDefendantId)
                        .withAsn("ASN-0001")
                        .withFirstName("Jane")
                        .withLastName("Doe")
                        .build())).collect(toList());

        final List<Object> events = addCpsMaterial(DEFENDANT_LEVEL_SECTION, "ASN-0001");

        assertThat(events, hasSize(1));
        assertThat(((MaterialAdded) events.get(0)).getDefendantId(), is(personDefendantId));
    }

    @Test
    void addCpsMaterial_defendantLevelNoMatch_should_rejectWithDefendantIdInvalid() {
        recordLibraCase();

        final List<Object> events = addCpsMaterial(DEFENDANT_LEVEL_SECTION, "UNKNOWN-DEF");

        assertThat(events, hasSize(2));
        final MaterialRejected materialRejected = (MaterialRejected) events.get(0);
        assertThat(materialRejected.getErrors().get(0).getCode(), is("DEFENDANT_ID_INVALID"));
        assertThat(events.get(1), instanceOf(CaseDocumentReviewRequired.class));
    }

    @Test
    void addCpsMaterial_secondFileAfterRecord_should_addEachFile() {
        recordLibraCase();

        final List<Object> first = addCpsMaterial(CASE_LEVEL_SECTION, null);
        final List<Object> second = addCpsMaterial(CASE_LEVEL_SECTION, null);

        assertThat(first, hasSize(1));
        assertThat(second, hasSize(1));
        final MaterialAdded firstAdded = (MaterialAdded) first.get(0);
        final MaterialAdded secondAdded = (MaterialAdded) second.get(0);
        assertThat(firstAdded.getMaterial().getFileStoreId().equals(secondAdded.getMaterial().getFileStoreId()), is(false));
    }

    private void recordLibraCase() {
        prosecutionCaseFile.recordMigratedCase(caseId, REFERENCE, LIBRA, LIBRA_CASE_ID, defendants).collect(toList());
    }

    private List<Object> addCpsMaterial(final String section, final String prosecutorDefendantId) {
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(asList(
                documentTypeAccessReferenceData()
                        .withId(randomUUID())
                        .withSection(CASE_LEVEL_SECTION)
                        .withDocumentCategory(DocumentCategory.CASE_LEVEL.toString())
                        .build(),
                documentTypeAccessReferenceData()
                        .withId(randomUUID())
                        .withSection(DEFENDANT_LEVEL_SECTION)
                        .withDocumentCategory(DocumentCategory.DEFENDANT_LEVEL.toString())
                        .build()));

        final Material material = material()
                .withFileStoreId(randomUUID())
                .withDocumentType(section)
                .withFileType("application/pdf")
                .build();

        return prosecutionCaseFile.addCpsMaterial(caseId, PROSECUTING_AUTHORITY, prosecutorDefendantId, material,
                        referenceDataQueryService, ZonedDateTime.now(), new DocumentDetails("cms-doc-1", 1, "section-code"))
                .collect(toList());
    }
}
