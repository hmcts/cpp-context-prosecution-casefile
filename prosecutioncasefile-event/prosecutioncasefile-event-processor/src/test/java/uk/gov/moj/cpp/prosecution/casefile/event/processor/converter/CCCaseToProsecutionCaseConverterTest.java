package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.CPS_PROSECUTOR_ID;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceDataWithContactEmail;

import uk.gov.justice.core.courts.DefendantFineAccountNumber;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.MigrationCaseStatus;
import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCCaseToProsecutionCaseConverterTest {

    private static final String XHIBIT_IDENTIFIER = "XHIBIT_IDENTIFIER";
    private static final String XHIBIT = "XHIBIT";
    private static final String EITHER_WAY = "Either Way";
    @InjectMocks
    private CCCaseToProsecutionCaseConverter ccCaseToProsecutionCaseConverter;

    @Mock
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Mock
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @Mock
    private CaseDetailsToCivilFeesConverter caseDetailsToCivilFeesConverter;

    @Test
    public void convertSjpProsecutionToCCCase() {

        final CcCaseReceived ccCaseReceived = ccCaseReceived().withProsecutionWithReferenceData(buildProsecutionWithReferenceData(EITHER_WAY)).build();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();

        final InitiateCourtProceedings convertedCourtProceedings = ccCaseToProsecutionCaseConverter.convert(ccCaseReceived);

        final CaseDetails caseDetails = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails();
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getInitiationCode().toString(), equalTo(caseDetails.getInitiationCode()));
        assertThat(convertedProsecutionCase.getSummonsCode(), equalTo(caseDetails.getSummonsCode()));
        assertThat(convertedProsecutionCase.getOriginatingOrganisation(), equalTo(caseDetails.getOriginatingOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisation(), equalTo(caseDetails.getCpsOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisationId(), equalTo(CPS_PROSECUTOR_ID));
        assertThat(convertedProsecutionCase.getStatementOfFacts(), equalTo(defendants.get(0).getOffences().get(0).getStatementOfFacts()));
        assertThat(convertedProsecutionCase.getStatementOfFactsWelsh(), equalTo(defendants.get(0).getOffences().get(0).getStatementOfFactsWelsh()));
        assertThat(convertedProsecutionCase.getClassOfCase(), equalTo(caseDetails.getClassOfCase()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getId()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getShortName()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getContact().getPrimaryEmail(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getInformantEmailAddress()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityName(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getFullName()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getMajorCreditorCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getMajorCreditorCode()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress1(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress1()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress2(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress2()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress3(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress3()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress4(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress4()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress5(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress5()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityOUCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getOucode()));
        assertNull(convertedProsecutionCase.getMigrationSourceSystem());
        assertThat(convertedProsecutionCase.getIsCivil(), equalTo(Boolean.TRUE));


    }

    @Test
    public void convertMigrationProsecutionToCCCase() {
        final CcCaseReceived ccCaseReceived = ccCaseReceived().withProsecutionWithReferenceData(
                buildProsecutionWithReferenceData(EITHER_WAY,randomUUID().toString(),false, MigrationSourceSystem.migrationSourceSystem()
                        .withMigrationSourceSystemCaseIdentifier(XHIBIT_IDENTIFIER)
                        .withMigrationSourceSystemName(XHIBIT)
                        .withMigrationCaseStatus(MigrationCaseStatus.ACTIVE)
                        .withDefendantFineAccountNumbers(singletonList(DefendantFineAccountNumber.defendantFineAccountNumber()
                                .withDefendantId(randomUUID())
                                .withFineAccountNumber("FINE13457")
                                .build()))
                        .build())).build();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();

        final InitiateCourtProceedings convertedCourtProceedings = ccCaseToProsecutionCaseConverter.convert(ccCaseReceived);

        final CaseDetails caseDetails = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails();
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getInitiationCode().toString(), equalTo(caseDetails.getInitiationCode()));
        assertThat(convertedProsecutionCase.getSummonsCode(), equalTo(caseDetails.getSummonsCode()));
        assertThat(convertedProsecutionCase.getOriginatingOrganisation(), equalTo(caseDetails.getOriginatingOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisation(), equalTo(caseDetails.getCpsOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisationId(), equalTo(CPS_PROSECUTOR_ID));
        assertThat(convertedProsecutionCase.getStatementOfFacts(), equalTo(defendants.get(0).getOffences().get(0).getStatementOfFacts()));
        assertThat(convertedProsecutionCase.getStatementOfFactsWelsh(), equalTo(defendants.get(0).getOffences().get(0).getStatementOfFactsWelsh()));
        assertThat(convertedProsecutionCase.getClassOfCase(), equalTo(caseDetails.getClassOfCase()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getId()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getShortName()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getContact().getPrimaryEmail(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getInformantEmailAddress()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityName(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getFullName()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getMajorCreditorCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getMajorCreditorCode()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress1(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress1()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress2(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress2()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress3(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress3()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress4(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress4()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress5(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress5()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityOUCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getOucode()));
        assertThat(convertedProsecutionCase.getMigrationSourceSystem().getMigrationSourceSystemName(), equalTo(XHIBIT));
        assertThat(convertedProsecutionCase.getMigrationSourceSystem().getMigrationSourceSystemCaseIdentifier(), equalTo(XHIBIT_IDENTIFIER));
        assertThat(convertedProsecutionCase.getMigrationSourceSystem().getDefendantFineAccountNumbers().get(0).getFineAccountNumber(), equalTo("FINE13457"));


    }

    @Test
    public void convertProsecutionToCCCaseWithNSP() {
        final CcCaseReceived ccCaseReceived = ccCaseReceived().withProsecutionWithReferenceData(buildProsecutionWithReferenceDataWithContactEmail("Either Way", randomUUID().toString(), false)).build();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();

        final InitiateCourtProceedings convertedCourtProceedings = ccCaseToProsecutionCaseConverter.convert(ccCaseReceived);

        final CaseDetails caseDetails = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails();
        final ProsecutionCase convertedProsecutionCase = convertedCourtProceedings.getInitiateCourtProceedings().getProsecutionCases().get(0);

        assertThat(convertedProsecutionCase.getInitiationCode().toString(), equalTo(caseDetails.getInitiationCode()));
        assertThat(convertedProsecutionCase.getSummonsCode(), equalTo(caseDetails.getSummonsCode()));
        assertThat(convertedProsecutionCase.getOriginatingOrganisation(), equalTo(caseDetails.getOriginatingOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisation(), equalTo(caseDetails.getCpsOrganisation()));
        assertThat(convertedProsecutionCase.getCpsOrganisationId(), equalTo(CPS_PROSECUTOR_ID));
        assertThat(convertedProsecutionCase.getStatementOfFacts(), equalTo(defendants.get(0).getOffences().get(0).getStatementOfFacts()));
        assertThat(convertedProsecutionCase.getStatementOfFactsWelsh(), equalTo(defendants.get(0).getOffences().get(0).getStatementOfFactsWelsh()));
        assertThat(convertedProsecutionCase.getClassOfCase(), equalTo(caseDetails.getClassOfCase()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getId()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getShortName()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getContact().getPrimaryEmail(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getContactEmailAddress()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityName(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getFullName()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getMajorCreditorCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getMajorCreditorCode()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress1(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress1()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress2(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress2()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress3(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress3()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress4(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress4()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getAddress().getAddress5(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getAddress().getAddress5()));
        assertThat(convertedProsecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityOUCode(), equalTo(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData().getOucode()));

    }

}