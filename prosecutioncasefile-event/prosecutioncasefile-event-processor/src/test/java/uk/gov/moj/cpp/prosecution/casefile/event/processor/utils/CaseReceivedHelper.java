package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence.alcoholRelatedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias.individualAlias;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData.modeOfTrialReasonsReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData.observedEthnicityReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation.parentGuardianInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData.selfdefinedEthnicityReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence.vehicleRelatedOffence;

import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.SpiProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Details;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Document;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Welsh;

import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;

public class CaseReceivedHelper {

    public static final UUID MOCKED_CASE_ID = fromString("dfd9e505-66bc-42b5-901c-4447ba3bfcd4");
    public static final UUID CPS_PROSECUTOR_ID = fromString("dfd9e505-66bc-42b5-901c-4447ba3bfc54");
    public static final UUID MOCKED_OFFENCE_ID = fromString("afd9e999-66bc-42b5-901c-4447ba3bfcd4");

    public static final String GIVEN_NAME_2 = "GivenName2";
    public static final String GIVEN_NAME_3 = "GivenName3";

    public static final String SELF_DEFINED_ETHNICITY_CODE = "selfDefinedEthnicity";
    public static final String OBSERVED_ETHNICITY_CODE = "1";
    public static final String NATIONALITY_CODE = "nationality";
    public static final String ADDITIONAL_NATIONALITY_CODE = "additionalNationality";
    public static final String TRANSFER = "Transfer";

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial) {
        return buildProsecutionWithReferenceData(modeOfTrial, randomUUID().toString());
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial, final boolean isAliasesEmpty) {
        return buildProsecutionWithReferenceData(modeOfTrial, randomUUID().toString(), isAliasesEmpty, null);
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial, final String motReasonId) {
        return buildProsecutionWithReferenceData(modeOfTrial, motReasonId, false, null);
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceData(final String modeOfTrial, final String motReasonId, final boolean isAliasesEmpty, final MigrationSourceSystem migrationSourceSystemigrationSourceSystem) {

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceDataOptional =
                of(organisationUnitWithCourtroomReferenceData()
                        .withId(randomUUID().toString())
                        .withOucodeL3Name("South Western (Lavender Hill)")
                        .build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(buildProsecution(isAliasesEmpty, migrationSourceSystemigrationSourceSystem));
        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(organisationUnitWithCourtroomReferenceDataOptional);
        referenceDataVO.setHearingType(HearingType.hearingType().withId(randomUUID()).withHearingDescription("Preliminary Hearing").build());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(randomUUID())
                .withShortName("DVLA")
                .withFullName("fullName")
                .withMajorCreditorCode("CreditorCode")
                .withInformantEmailAddress("test@email.com")
                .withOucode("oucode")
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .build());
        referenceDataVO.setCaseMarkers(singletonList(CaseMarker.caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setSelfdefinedEthnicityReferenceData(singletonList(selfdefinedEthnicityReferenceData().withCode(SELF_DEFINED_ETHNICITY_CODE).withId(randomUUID()).withDescription("description").build()));
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(ADDITIONAL_NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(asList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build(),
                modeOfTrialReasonsReferenceData()
                        .withId(motReasonId)
                        .withCode("02")
                        .withDescription("Indictable-only offence)")
                        .withSeqNum("91")
                        .build()));

        return prosecutionWithReferenceData;
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceDataWithContactEmail(final String modeOfTrial, final String motReasonId, final boolean isAliasesEmpty) {

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceDataOptional =
                of(organisationUnitWithCourtroomReferenceData()
                        .withId(randomUUID().toString())
                        .withOucodeL3Name("South Western (Lavender Hill)")
                        .build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(buildProsecution(isAliasesEmpty, null));
        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(organisationUnitWithCourtroomReferenceDataOptional);
        referenceDataVO.setHearingType(HearingType.hearingType().withId(randomUUID()).withHearingDescription("Preliminary Hearing").build());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(randomUUID())
                .withShortName("DVLA")
                .withFullName("fullName")
                .withMajorCreditorCode("CreditorCode")
                .withContactEmailAddress("test@email.com")
                .withOucode("oucode")
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .build());
        referenceDataVO.setCaseMarkers(singletonList(CaseMarker.caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setSelfdefinedEthnicityReferenceData(singletonList(selfdefinedEthnicityReferenceData().withCode(SELF_DEFINED_ETHNICITY_CODE).withId(randomUUID()).withDescription("description").build()));
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(ADDITIONAL_NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(asList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build(),
                modeOfTrialReasonsReferenceData()
                        .withId(motReasonId)
                        .withCode("02")
                        .withDescription("Indictable-only offence)")
                        .withSeqNum("91")
                        .build()));

        return prosecutionWithReferenceData;
    }

    public static ReferenceDataVO buildReferenceDataWithOffenceAndModeOfTrial(final String modeOfTrial) {
        return buildReferenceDataWithOffenceAndModeOfTrial(modeOfTrial, randomUUID().toString());
    }

    public static ReferenceDataVO buildReferenceDataWithOffenceAndModeOfTrial(final String modeOfTrial, final String motReasonId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(buildOffenceReferenceData(modeOfTrial));
        referenceDataVO.setModeOfTrialReferenceData(asList(
                modeOfTrialReasonsReferenceData()
                        .withId(randomUUID().toString())
                        .withCode("01")
                        .withDescription("Summary-only offence")
                        .withSeqNum("90")
                        .build(),
                modeOfTrialReasonsReferenceData()
                        .withId(motReasonId)
                        .withCode("02")
                        .withDescription("Indictable-only offence)")
                        .withSeqNum("91")
                        .build()));

        return referenceDataVO;
    }

    public static ReferenceDataVO buildReferenceDataIncludingDvlaCode() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(getOffenceReferenceData());
        return referenceDataVO;
    }

    private static List<OffenceReferenceData> getOffenceReferenceData() {
        return singletonList(new OffenceReferenceData(true, "TVL-ABC", buildPressRestrictableReferenceData().getCppDateOfLastUpdate(), Details.details().build(),
                buildPressRestrictableReferenceData().getDrugsOrAlcoholRelated(), "dvlaCode", true, buildPressRestrictableReferenceData().getExParte(), buildPressRestrictableReferenceData().getLegislation(), buildPressRestrictableReferenceData().getLegislationWelsh(),
                buildPressRestrictableReferenceData().getLocationRequired(), "Max Penalty", buildPressRestrictableReferenceData().getModeOfTrial(),
                buildPressRestrictableReferenceData().getModeOfTrialDerived(), buildPressRestrictableReferenceData().getOffenceEndDate(), randomUUID(), buildPressRestrictableReferenceData().getOffenceStartDate(), buildPressRestrictableReferenceData().getPnldDateOfLastUpdate(), buildPressRestrictableReferenceData().getProsecutionTimeLimit(),
                buildPressRestrictableReferenceData().getReportRestrictResultCode(), buildPressRestrictableReferenceData().getTitle(), buildPressRestrictableReferenceData().getTitleWelsh(), buildPressRestrictableReferenceData().getValidFrom(), buildPressRestrictableReferenceData().getValidTo()));
    }

    private static List<OffenceReferenceData> buildOffenceReferenceData(final String modeOfTrial) {
        return ImmutableList.of(offenceReferenceData()
                .withCjsOffenceCode("TVL-ABC")
                .withLegislation("offenceLegalisation")
                .withLegislationWelsh("offenceLegalisationWelsh")
                .withOffenceId(fromString("d8c63737-3c60-496b-94bb-30faa761f00a"))
                .withTitle("Offence Tittle")
                .withTitleWelsh("Offence Tittle Welsh")
                .withModeOfTrialDerived(modeOfTrial)
                .withMaxPenalty("Max Penalty")
                .build());
    }

    public static ProsecutionWithReferenceData buildProsecutionWithReferenceDataWithChannel(Channel channel) {

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceDataOptional =
                of(organisationUnitWithCourtroomReferenceData()
                        .withId(randomUUID().toString())
                        .withOucodeL3Name("South Western (Lavender Hill)")
                        .build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(buildProsecutionWithChannel(channel));
        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(organisationUnitWithCourtroomReferenceDataOptional);
        referenceDataVO.setHearingType(HearingType.hearingType().withId(randomUUID()).withHearingDescription("Preliminary Hearing").build());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().withId(randomUUID()).withShortName("TFL").build());
        referenceDataVO.setCaseMarkers(singletonList(CaseMarker.caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID()).build()));
        referenceDataVO.setSelfdefinedEthnicityReferenceData(singletonList(selfdefinedEthnicityReferenceData().withCode(SELF_DEFINED_ETHNICITY_CODE).withId(randomUUID()).withDescription("description").build()));
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().withIsoCode(ADDITIONAL_NATIONALITY_CODE).withId(randomUUID().toString()).withNationality("description").build());
        referenceDataVO.setObservedEthnicityReferenceData(singletonList(observedEthnicityReferenceData().withEthnicityCode(OBSERVED_ETHNICITY_CODE).withId(randomUUID()).withEthnicityDescription("description").build()));
        return prosecutionWithReferenceData;
    }


    public static SpiProsecutionDefendantsAdded buildSpiProsecutionDefendantsAdded() {
        return SpiProsecutionDefendantsAdded.spiProsecutionDefendantsAdded()
                .withDefendants(singletonList(buildDefendant()))
                .withCaseId(randomUUID())
                .withReferenceDataVO(getReferenceDataVO())
                .build();
    }

    public static ProsecutionDefendantsAdded buildProsecutionDefendantsAdded() {
        return ProsecutionDefendantsAdded.prosecutionDefendantsAdded()
                .withDefendants(singletonList(buildDefendant()))
                .withCaseId(randomUUID())
                .withReferenceDataVO(getReferenceDataVO())
                .build();
    }

    public static ReferenceDataVO getReferenceDataVO() {

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceDataOptional = of(organisationUnitWithCourtroomReferenceData()
                .withId(randomUUID().toString())
                .withOucodeL3Name("South Western (Lavender Hill)")
                .withOucodeL3WelshName("Welsh Name")
                .build());

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(organisationUnitWithCourtroomReferenceDataOptional);
        referenceDataVO.setOffenceReferenceData(Collections.singletonList(offenceReferenceData().withCjsOffenceCode("TVL-ABC")
                .withOffenceId(randomUUID()).withModeOfTrialDerived("Summary").withTitle("Test").build()));
        referenceDataVO.setHearingType(HearingType.hearingType().withId(randomUUID()).withHearingDescription("Preliminary Hearing").build());
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().withId(randomUUID()).withShortName("DVLA").build());
        referenceDataVO.setCaseMarkers(singletonList(CaseMarker.caseMarker().withMarkerTypeCode("ML").withMarkerTypeDescription("Test Case Marker Description").withMarkerTypeId(randomUUID())
                .build()));
        return referenceDataVO;
    }

    public static Prosecution buildProsecution() {
        return buildProsecution(false, null);
    }

    public static Prosecution buildProsecution(final boolean isAliasesEmpty, final MigrationSourceSystem migrationSourceSystem) {
        return prosecution()
                .withCaseDetails(caseDetails().withInitiationCode("Z")
                        .withCaseId(MOCKED_CASE_ID)
                        .withProsecutor(buildProsecutionSubmissionDetails())
                        .withVehicleOperatorLicenceNumber("VehicleOperatorLicenseNumber")
                        .withOriginatingOrganisation("orignatingOrganisation")
                        .withCpsOrganisation("cpsOrganisation")
                        .withCpsOrganisationId(CPS_PROSECUTOR_ID)
                        .withCaseMarkers(singletonList(CaseMarker.caseMarker().withMarkerTypeCode("ML").build()))
                        .withClassOfCase("Class 1")
                        .withSummonsCode("M")
                        .withTrialReceiptType(TRANSFER)
                        .build())
                .withChannel(SPI)
                .withDefendants(asList(buildDefendant(isAliasesEmpty), buildCorporateDefendant(isAliasesEmpty)))
                .withMigrationSourceSystem(migrationSourceSystem)
                .withIsCivil(true)
                .build();
    }


    public static Prosecution buildProsecutionWithChannel(Channel channel) {
        return prosecution()
                .withCaseDetails(caseDetails().withInitiationCode("Z")
                        .withCaseId(MOCKED_CASE_ID)
                        .withProsecutorCaseReference("A16Xt4kCBJ")
                        .withProsecutor(buildProsecutionSubmissionDetails())
                        .build())
                .withChannel(channel)
                .withDefendants(asList(buildDefendant(), buildCorporateDefendant()))
                .build();
    }


    public static Prosecutor buildProsecutionSubmissionDetails() {
        return prosecutor()
                .withProsecutingAuthority("TVL")
                .withProsecutingAuthority("ProsecutionAuthority")
                .withReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                        .withContactEmailAddress("contact@cpp.co.uk")
                        .withAddress(Address.address()
                                .withAddress1("address1")
                                .withAddress2("address2")
                                .withAddress3("address3")
                                .withAddress4("address4")
                                .withAddress5("address5")
                                .withPostcode("postcode")
                                .build())
                        .withFullName("fullname")
                        .withId(randomUUID())
                        .withMajorCreditorCode("mojor")
                        .withOucode("Ou")
                        .build())
                .build();
    }

    private static Defendant buildCorporateDefendant() {
        return buildCorporateDefendant(false);
    }

    private static Defendant buildCorporateDefendant(final boolean isAliasesEmpty) {
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        defendantBuilder
                .withId(randomUUID().toString())
                .withDocumentationLanguage(Language.E)
                .withHearingLanguage(Language.E)
                .withOrganisationName("organisation name");
        if (isAliasesEmpty) {
            defendantBuilder.withAliasForCorporate(emptyList());
        } else {
            defendantBuilder.withAliasForCorporate(asList("alias1", "alias2", "alias3"));
        }

        defendantBuilder.withOffences(buildOffences())
                .withPostingDate(LocalDate.of(2017, 10, 20))
                .withLanguageRequirement("No")
                .withNumPreviousConvictions(99);
        return defendantBuilder.build();
    }


    public static Defendant buildDefendantWithTitle(String title) {
        return buildDefendantWithTitle(title, false);
    }

    public static Defendant buildDefendantWithTitle(String title, final boolean isIndividualAliasesEmpty) {
        return defendant()
                .withId(randomUUID().toString())
                .withDocumentationLanguage(Language.E)
                .withHearingLanguage(Language.E)
                .withInitiationCode("S")
                .withIndividualAliases(buildIndividualAliases(isIndividualAliasesEmpty))
                .withIndividual(Individual.individual()
                        .withPersonalInformation(personalInformation()
                                .withAddress(Address.address()
                                        .withAddress1("66 Exeter Street")
                                        .withAddress2("address line 2")
                                        .withAddress3("address line 3")
                                        .withAddress4("address line 4")
                                        .withAddress4("address line 5")
                                        .withPostcode("M60 1NW")
                                        .build())
                                .withContactDetails(contactDetails()
                                        .withPrimaryEmail("primaryemail")
                                        .withSecondaryEmail("secondaryemail")
                                        .withHome("homePhone")
                                        .withWork("workPhone")
                                        .withMobile("mobile")
                                        .build())
                                .withContactDetails(contactDetails()
                                        .withPrimaryEmail("primaryemail")
                                        .withSecondaryEmail("secondaryemail")
                                        .withHome("homePhone")
                                        .withWork("workPhone")
                                        .withMobile("mobile")
                                        .build())
                                .withFirstName("Eugene")
                                .withLastName("Tooms")
                                .withTitle(title)
                                .withObservedEthnicity(Integer.parseInt(OBSERVED_ETHNICITY_CODE))
                                .build())
                        .withPerceivedBirthYear("1970")
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withEthnicity("Unknown")
                                .withDateOfBirth(LocalDate.of(1989, 4, 18))
                                .withGender(Gender.MALE)
                                .withNationality(NATIONALITY_CODE)
                                .withAdditionalNationality(ADDITIONAL_NATIONALITY_CODE)
                                .withEthnicity(SELF_DEFINED_ETHNICITY_CODE)
                                .build())
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation()
                                        .withAddress(Address.address()
                                                .withAddress1("66 Exeter Street")
                                                .withAddress2("address line 2")
                                                .withAddress3("address line 3")
                                                .withAddress4("address line 4")
                                                .withAddress4("address line 5")
                                                .withPostcode("M60 1NW")
                                                .build())
                                        .withContactDetails(contactDetails()
                                                .withPrimaryEmail("primaryemail")
                                                .withSecondaryEmail("secondaryemail")
                                                .withHome("homePhone")
                                                .withWork("workPhone")
                                                .withMobile("mobile")
                                                .build())
                                        .withFirstName("Eugene")
                                        .withLastName("Tooms")
                                        .withTitle(title)
                                        .build())
                                .withSelfDefinedEthnicity(SELF_DEFINED_ETHNICITY_CODE)
                                .withObservedEthnicity(OBSERVED_ETHNICITY_CODE)
                                .build())
                        .withNationalInsuranceNumber("1922492")
                        .withDriverNumber("2362435")
                        .build())
                .withOffences(buildOffences())
                .withPostingDate(LocalDate.of(2017, 10, 20))
                .withLanguageRequirement("No")
                .withNumPreviousConvictions(99)
                .withInitialHearing(InitialHearing.initialHearing()
                        .withEndDate("2050-02-04T10:05:01.001Z")
                        .withCourtScheduleId("test-courtScheduleId")
                        .withDateOfHearing("2050-02-20")
                        .withTimeOfHearing("11:30:00.001")
                        .build())
                .build();
    }

    public static Defendant buildDefendantWithCustodyStatus(final String title, final String defendantId, final String custodyStatus) {
        return Defendant.defendant()
                .withId(defendantId).withCustodyStatus(custodyStatus)
                .withIndividual(Individual.individual()
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withAddress(Address.address()
                                        .withAddress1("66 Exeter Street")
                                        .withAddress2("address line 2")
                                        .withAddress3("address line 3")
                                        .withAddress4("address line 4")
                                        .withAddress4("address line 5")
                                        .withPostcode("M60 1NW")
                                        .build())
                                .withFirstName("Eugene")
                                .withLastName("Tooms")
                                .withTitle(title)
                                .build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withEthnicity("Unknown")
                                .withDateOfBirth(LocalDate.of(1989, 4, 18))
                                .withGender(Gender.MALE)
                                .build())
                        .withNationalInsuranceNumber("1922492")
                        .withDriverNumber("2362435")
                        .build())
                .withHearingLanguage(Language.E)
                .withOffences(buildOffences())
                .build();
    }

    private static List<IndividualAlias> buildIndividualAliases(final boolean returnIndividualAliasesEmpty) {
        if (returnIndividualAliasesEmpty) {
            return emptyList();
        }

        return asList(
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withGivenName2("   " + GIVEN_NAME_2 + " ")
                        .withGivenName3(" " + GIVEN_NAME_3 + "   ")
                        .withLastName("LastName")
                        .build(),
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withGivenName2(GIVEN_NAME_2)
                        .withLastName("LastName")
                        .build(),
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withGivenName3(GIVEN_NAME_3)
                        .withLastName("LastName")
                        .build(),
                individualAlias()
                        .withTitle("mr")
                        .withFirstName("FirstName")
                        .withLastName("LastName")
                        .build());
    }

    public static Defendant buildDefendant() {
        return buildDefendantWithTitle("MR");
    }

    public static Defendant buildDefendant(final boolean isIndividualAliasesEmpty) {
        return buildDefendantWithTitle("MR", isIndividualAliasesEmpty);
    }

    public static List<Offence> buildOffencesWithNullResultCode() {
        return singletonList(Offence.offence()
                .withOffenceId(MOCKED_OFFENCE_ID)
                .withVehicleRelatedOffence(vehicleRelatedOffence()
                        .withVehicleCode("vehicleCode")
                        .withVehicleRegistrationMark("vehicleRegistrationMark")
                        .build())
                .withAlcoholRelatedOffence(AlcoholRelatedOffence.alcoholRelatedOffence()
                        .withAlcoholLevelAmount(500)
                        .withAlcoholLevelMethod("A").build())
                .withBackDuty(BigDecimal.ONE)
                .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                .withChargeDate(LocalDate.of(2017, 8, 15))
                .withAppliedCompensation(BigDecimal.TEN)
                .withOffenceCode("TVL-ABC")
                .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                .withOffenceDateCode(4)
                .withOffenceLocation("London")
                .withOffenceSequenceNumber(6)
                .withOffenceWording("TV LICENSE NOT PAID")
                .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                .withStatementOfFacts("facts")
                .withStatementOfFactsWelsh("welsh-facts")
                .withLaidDate(LocalDate.of(2019, 1, 1))
                .withReferenceData(buildNonPressRestrictableReferenceDataWithNull())
                .build());
    }

    public static List<Offence> buildOffences() {
        return singletonList(offence()
                .withOffenceId(MOCKED_OFFENCE_ID)
                .withVehicleRelatedOffence(vehicleRelatedOffence()
                        .withVehicleCode("vehicleCode")
                        .withVehicleRegistrationMark("vehicleRegistrationMark")
                        .build())
                .withAlcoholRelatedOffence(alcoholRelatedOffence()
                        .withAlcoholLevelAmount(500)
                        .withAlcoholLevelMethod("A").build())
                .withBackDuty(BigDecimal.ONE)
                .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                .withChargeDate(LocalDate.of(2017, 8, 15))
                .withAppliedCompensation(BigDecimal.TEN)
                .withOffenceCode("TVL-ABC")
                .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                .withOffenceDateCode(4)
                .withOffenceLocation("London")
                .withOffenceSequenceNumber(6)
                .withOffenceWording("TV LICENSE NOT PAID")
                .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                .withStatementOfFacts("facts")
                .withStatementOfFactsWelsh("welsh-facts")
                .withLaidDate(LocalDate.of(2019, 1, 1))
                .withReferenceData(buildNonPressRestrictableReferenceData())
                .withProsecutorOfferAOCP(true)
                .build());
    }

    public static List<Offence> buildPressRestrictableOffences() {
        return singletonList(offence()
                .withOffenceId(MOCKED_OFFENCE_ID)
                .withVehicleRelatedOffence(vehicleRelatedOffence()
                        .withVehicleCode("vehicleCode")
                        .withVehicleRegistrationMark("vehicleRegistrationMark")
                        .build())
                .withBackDuty(BigDecimal.ONE)
                .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                .withChargeDate(LocalDate.of(2017, 8, 15))
                .withAppliedCompensation(BigDecimal.TEN)
                .withOffenceCode("ED96001")
                .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                .withOffenceDateCode(4)
                .withOffenceLocation("London")
                .withOffenceSequenceNumber(6)
                .withOffenceWording("TV LICENSE NOT PAID")
                .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                .withStatementOfFacts("facts")
                .withStatementOfFactsWelsh("welsh-facts")
                .withLaidDate(LocalDate.of(2019, 1, 1))
                .withReferenceData(buildPressRestrictableReferenceData())
                .build());
    }

    public static List<Offence> buildOffenceTitleReferenceDataOffences() {
        return singletonList(offence()
                .withOffenceId(MOCKED_OFFENCE_ID)
                .withVehicleRelatedOffence(vehicleRelatedOffence()
                        .withVehicleCode("vehicleCode")
                        .withVehicleRegistrationMark("vehicleRegistrationMark")
                        .build())
                .withBackDuty(BigDecimal.ONE)
                .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                .withChargeDate(LocalDate.of(2017, 8, 15))
                .withAppliedCompensation(BigDecimal.TEN)
                .withOffenceCode("ED96001")
                .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                .withOffenceDateCode(4)
                .withOffenceLocation("London")
                .withOffenceSequenceNumber(6)
                .withOffenceWording("TV LICENSE NOT PAID")
                .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                .withStatementOfFacts("facts")
                .withStatementOfFactsWelsh("welsh-facts")
                .withLaidDate(LocalDate.of(2019, 1, 1))
                .withReferenceData(buildOffenceTitleReferenceData("Offence Title", "Offence Title Welsh"))
                .build());
    }

    public static List<Offence> buildOffenceTitleReferenceDataOffencesWithNullWelsh() {
        return singletonList(Offence.offence()
                .withOffenceId(MOCKED_OFFENCE_ID)
                .withVehicleRelatedOffence(vehicleRelatedOffence()
                        .withVehicleCode("vehicleCode")
                        .withVehicleRegistrationMark("vehicleRegistrationMark")
                        .build())
                .withBackDuty(BigDecimal.ONE)
                .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                .withChargeDate(LocalDate.of(2017, 8, 15))
                .withAppliedCompensation(BigDecimal.TEN)
                .withOffenceCode("ED96001")
                .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                .withOffenceDateCode(4)
                .withOffenceLocation("London")
                .withOffenceSequenceNumber(6)
                .withOffenceWording("TV LICENSE NOT PAID")
                .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                .withStatementOfFacts("facts")
                .withStatementOfFactsWelsh("welsh-facts")
                .withLaidDate(LocalDate.of(2019, 1, 1))
                .withReferenceData(buildOffenceTitleReferenceData("Offence Title", null))
                .build());
    }


    private static OffenceReferenceData buildPressRestrictableReferenceData() {
        return offenceReferenceData().withReportRestrictResultCode("D45").build();
    }

    public static OffenceReferenceData buildNonPressRestrictableReferenceData() {
        return offenceReferenceData().withReportRestrictResultCode("").build();
    }

    public static OffenceReferenceData buildNonPressRestrictableReferenceDataWithNull() {
        return OffenceReferenceData.offenceReferenceData().withReportRestrictResultCode(null).build();
    }

    private static OffenceReferenceData buildOffenceTitleReferenceData(String offenceTitle, String welshOffenceTitle) {

        return offenceReferenceData()
                .withTitle(offenceTitle)
                .withDetails(Details.details().withDocument(Document.document().withWelsh(Welsh.welsh()
                                .withWelshoffencetitle(welshOffenceTitle).build())
                        .build()).build())
                .build();
    }

    public static String readResourcesFile(final String path) {
        String request = null;
        try {
            final InputStream inputStream = CaseReceivedHelper.class.getClassLoader().getResourceAsStream(path);
            assertThat(inputStream, notNullValue());
            request = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject getPayloadAsJsonObject(final String path) {
        return createReader(new StringReader(readResourcesFile(path))).readObject();
    }
}
