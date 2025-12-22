package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantAlias.defendantAlias;
import static uk.gov.justice.core.courts.Ethnicity.ethnicity;
import static uk.gov.justice.core.courts.InitiationCode.valueFor;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

public class ProsecutionCaseFileDefendantToCCDefendantConverter implements ParameterisedConverter<List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant>, List<Defendant>, ParamsVO> {

    @Inject
    private ProsecutionCaseToCCPersonDefendantConverter prosecutionCaseToCCPersonDefendantConverterToCCOffenceConverter;

    @Inject
    private ProsecutionCaseFileToCCLegalEntityDefendantConverter prosecutionCaseFileToCCLegalEntityDefendantConverter;

    @Inject
    private ProsecutionCaseFileOffenceToCourtsOffenceConverter prosecutionCaseFileOffenceToCourtsOffenceConverter;

    @Override
    @SuppressWarnings("squid:S1188")
    public List<uk.gov.justice.core.courts.Defendant> convert(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants, final ParamsVO paramsVO) {

        return defendants.stream()
                .map(defendant -> {
                    paramsVO.setCustodyTimelineDefendant(defendant.getCustodyTimeLimit());
                            final Defendant.Builder builder = defendant()
                                    .withId(fromString(defendant.getId()))
                                    .withMasterDefendantId(fromString(defendant.getId()))
                                    .withInitiationCode(valueFor(defendant.getInitiationCode()).orElse(null))
                                    .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                                    .withCroNumber(defendant.getCroNumber())
                                    .withOffences(prosecutionCaseFileOffenceToCourtsOffenceConverter.convert(defendant.getOffences(), paramsVO))
                                    .withPersonDefendant(defendant.getIndividual() != null ? prosecutionCaseToCCPersonDefendantConverterToCCOffenceConverter.convert(defendant, paramsVO.getReferenceDataVO()) : null)
                                    .withPncId(defendant.getPncIdentifier())
                                    .withCroNumber(defendant.getCroNumber())
                                    .withProsecutionAuthorityReference(defendant.getProsecutorDefendantReference())
                                    .withProsecutionCaseId(paramsVO.getCaseId())
                                    .withLegalEntityDefendant(prosecutionCaseFileToCCLegalEntityDefendantConverter.convert(defendant))
                                    .withNumberOfPreviousConvictionsCited(defendant.getNumPreviousConvictions())
                                    .withAssociatedPersons(defendant.getIndividual() != null ? buildAssociatedPersons(defendant.getIndividual().getParentGuardianInformation(), paramsVO.getReferenceDataVO()) : null);

                            if (isNotEmpty(defendant.getIndividualAliases())) {
                                builder.withAliases(buildIndividualAliases(defendant.getIndividualAliases()));
                            } else if (isNotEmpty(defendant.getAliasForCorporate())) {
                                builder.withAliases(buildCorporateAliases(defendant.getAliasForCorporate()));
                            }
                            return builder.build();
                        }
                )
                .collect(toList());
    }

    @SuppressWarnings("squid:S1168")
    private List<AssociatedPerson> buildAssociatedPersons(final ParentGuardianInformation parentGuardianInformation, final ReferenceDataVO referenceDataVO) {

        if (null == parentGuardianInformation) {
            return null;
        } else if (!StringUtils.isEmpty(parentGuardianInformation.getOrganisationName())) {
            return null;
        } else {

            final AssociatedPerson associatedPerson = associatedPerson()
                    .withPerson(Person.person()
                            .withGender(getGender(parentGuardianInformation.getGender()).orElse(null))
                            .withDateOfBirth(null != parentGuardianInformation.getDateOfBirth() ? parentGuardianInformation.getDateOfBirth().toString() : null)
                            .withMiddleName(buildMiddleName(parentGuardianInformation))
                            .withLastName(null != parentGuardianInformation.getPersonalInformation() ? parentGuardianInformation.getPersonalInformation().getLastName() : null)
                            .withFirstName(parentGuardianInformation.getPersonalInformation().getFirstName())
                            .withTitle(parentGuardianInformation.getPersonalInformation().getTitle())
                            .withAddress(buildAddress(parentGuardianInformation))
                            .withContact(null != parentGuardianInformation.getPersonalInformation() ? buildContact(parentGuardianInformation.getPersonalInformation()) : null)
                            .withEthnicity(buildEthnicity(parentGuardianInformation, referenceDataVO))
                            .build())
                    .withRole("ParentGuardian")

                    .build();

            return ImmutableList.of(associatedPerson);
        }
    }

    private ContactNumber buildContact(final PersonalInformation personalInformation) {

        if (personalInformation.getContactDetails() == null) {
            return null;
        }

        return contactNumber()
                .withWork(personalInformation.getContactDetails().getWork())
                .withSecondaryEmail(personalInformation.getContactDetails().getSecondaryEmail())
                .withPrimaryEmail(personalInformation.getContactDetails().getPrimaryEmail())
                .withMobile(personalInformation.getContactDetails().getMobile())
                .withHome(personalInformation.getContactDetails().getHome())
                .build();
    }

    private Ethnicity buildEthnicity(final ParentGuardianInformation parentGuardianInformation, final ReferenceDataVO referenceDataVO) {
        final Ethnicity.Builder ethnicityBuiler = ethnicity();

        if (parentGuardianInformation.getSelfDefinedEthnicity() != null) {
            referenceDataVO.getSelfdefinedEthnicityReferenceData().stream()
                    .filter(selfDefinedEthnicityReferenceData -> selfDefinedEthnicityReferenceData.getCode().equalsIgnoreCase(parentGuardianInformation.getSelfDefinedEthnicity()))
                    .findAny().ifPresent(selfDefinedEthnicityReferenceDataPG -> {
                ethnicityBuiler.withSelfDefinedEthnicityId(selfDefinedEthnicityReferenceDataPG.getId());
                ethnicityBuiler.withSelfDefinedEthnicityCode(selfDefinedEthnicityReferenceDataPG.getCode());
                ethnicityBuiler.withSelfDefinedEthnicityDescription(selfDefinedEthnicityReferenceDataPG.getDescription());
            });
        }

        if (parentGuardianInformation.getObservedEthnicity() != null) {
            referenceDataVO.getObservedEthnicityReferenceData().stream()
                    .filter(observedEthnicityReferenceData -> observedEthnicityReferenceData.getEthnicityCode().equalsIgnoreCase(parentGuardianInformation.getObservedEthnicity()))
                    .findAny().ifPresent(observedEthnicityReferenceDataPG -> {
                ethnicityBuiler.withObservedEthnicityId(observedEthnicityReferenceDataPG.getId());
                ethnicityBuiler.withObservedEthnicityCode(observedEthnicityReferenceDataPG.getEthnicityCode());
                ethnicityBuiler.withObservedEthnicityDescription(observedEthnicityReferenceDataPG.getEthnicityDescription());
            });
        }

        return ethnicityBuiler.build();
    }

    private List<DefendantAlias> buildIndividualAliases(final List<IndividualAlias> individualAliases) {
        final List<DefendantAlias> defendantAliases = new ArrayList<>();
        individualAliases.forEach(individualAlias ->
                defendantAliases.add(defendantAlias()
                        .withTitle(individualAlias.getTitle())
                        .withFirstName(individualAlias.getFirstName())
                        .withMiddleName(buildAliasMiddleName(individualAlias))
                        .withLastName(individualAlias.getLastName())
                        .build())

        );
        return defendantAliases;
    }

    private String buildAliasMiddleName(final IndividualAlias individualAlias) {
        final String middleName = join(trim(individualAlias.getGivenName2()), " ", trim(individualAlias.getGivenName3())).trim();
        return isEmpty(middleName) ? null : middleName;
    }

    private List<DefendantAlias> buildCorporateAliases(final List<String> corporateAliases) {
        final List<DefendantAlias> defendantAliases = new ArrayList<>();
        corporateAliases.forEach(corporateAlias ->
                defendantAliases.add(defendantAlias().withLegalEntityName(corporateAlias).build())
        );
        return defendantAliases;
    }

    private String buildMiddleName(final ParentGuardianInformation parentGuardianInformation) {
        if (null == parentGuardianInformation.getPersonalInformation().getGivenName2() && null == parentGuardianInformation.getPersonalInformation().getGivenName3()) {
            return null;
        }
        return join(parentGuardianInformation.getPersonalInformation().getGivenName2(), " ", parentGuardianInformation.getPersonalInformation().getGivenName3());
    }

    private Optional<Gender> getGender(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender gender) {
        if (null != gender) {
            return Gender.valueFor(gender.name());
        } else {
            return Optional.empty();
        }
    }

    private Address buildAddress(final ParentGuardianInformation parentGuardianInformation) {
        if (null != parentGuardianInformation.getPersonalInformation() && null != parentGuardianInformation.getPersonalInformation().getAddress()) {

            final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address address = parentGuardianInformation.getPersonalInformation().getAddress();
            return Address.address()
                    .withAddress1(address.getAddress1())
                    .withAddress2(address.getAddress2())
                    .withAddress3(address.getAddress3())
                    .withAddress4(address.getAddress4())
                    .withAddress5(address.getAddress5())
                    .withPostcode(address.getPostcode())
                    .build();
        }

        return null;
    }

}