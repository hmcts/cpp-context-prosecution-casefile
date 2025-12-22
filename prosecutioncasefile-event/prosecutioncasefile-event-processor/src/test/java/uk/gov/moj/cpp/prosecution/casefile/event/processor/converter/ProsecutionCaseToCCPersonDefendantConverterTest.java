package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseToCCPersonDefendantConverterTest {

    public static final String CUSTODY_STATUS = "C";
    public static final String STATUS_CODE = "C";
    private ProsecutionCaseToCCPersonDefendantConverter converter;

    @Mock
    private ReferenceDataVO referenceDataVO;

    @Test
    public void shouldNotConvertTheTitle() {
        final Defendant defendant = CaseReceivedHelper.buildDefendantWithTitle("Baroness");
        converter = new ProsecutionCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertThat(personDefendant.getPersonDetails().getTitle().toString(), is("Baroness"));
    }

    @Test
    public void shouldConvertDefendantToPersonDefendant() {
        final Defendant defendant = CaseReceivedHelper.buildDefendantWithTitle("MR");
        converter = new ProsecutionCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);

        assertThat(personDefendant.getArrestSummonsNumber(), is(defendant.getAsn()));
        assertThat(personDefendant.getBailConditions(), is(defendant.getIndividual().getBailConditions()));
        assertThat(personDefendant.getBailStatus(), is(defendant.getCustodyStatus()));
        assertThat(personDefendant.getDriverLicenseIssue(), is(defendant.getIndividual().getDriverLicenceIssue()));
        assertThat(personDefendant.getDriverLicenceCode(), is(defendant.getIndividual().getDriverLicenceCode()));
        assertThat(personDefendant.getDriverNumber(), is(defendant.getIndividual().getDriverNumber()));
        assertThat(personDefendant.getArrestSummonsNumber(), is(defendant.getAsn()));

        assertNotNull(personDefendant.getPersonDetails().getNationalityCode());
        assertNotNull(personDefendant.getPersonDetails().getAdditionalNationalityCode());
        assertThat(personDefendant.getPersonDetails().getNationalInsuranceNumber(),
                is(defendant.getIndividual().getNationalInsuranceNumber()));
        assertThat(personDefendant.getPersonDetails().getNationalityCode(),
                is(defendant.getIndividual().getSelfDefinedInformation().getNationality()));
        assertThat(personDefendant.getPersonDetails().getAdditionalNationalityCode(),
                is(defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality()));
        assertThat(personDefendant.getArrestSummonsNumber(), is(defendant.getAsn()));
    }

    @Test
    public void bailStatusCodeShouldBeMatchedhWhenCustodyStatusAndStatusCodeAreSame() {
        final Defendant defendant = CaseReceivedHelper.buildDefendantWithCustodyStatus("Mr", UUID.randomUUID().toString(), CUSTODY_STATUS);

        List<BailStatusReferenceData> listBailStatusReferenceData = new ArrayList<>();
        BailStatusReferenceData bailStatusReferenceData = new BailStatusReferenceData(UUID.randomUUID(),1, STATUS_CODE,"description","2008-05-05");
        listBailStatusReferenceData.add(bailStatusReferenceData);

        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(listBailStatusReferenceData);

        converter = new ProsecutionCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertThat(personDefendant.getBailStatus().getCode() , is(STATUS_CODE));
    }

    @Test
    void nullBailStatusCodeShouldBeMatchedhWhenCustodyStatusAndStatusCodeAreSame() {
        final Defendant defendant = CaseReceivedHelper.buildDefendantWithCustodyStatus("Mr", UUID.randomUUID().toString(), null);

        List<BailStatusReferenceData> listBailStatusReferenceData = new ArrayList<>();
        BailStatusReferenceData bailStatusReferenceData = new BailStatusReferenceData(UUID.randomUUID(),1, STATUS_CODE,"description","2008-05-05");
        listBailStatusReferenceData.add(bailStatusReferenceData);

        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(listBailStatusReferenceData);

        converter = new ProsecutionCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertNull(personDefendant.getBailStatus());
    }

    @Test
    public void bailStatusIsEmptyWhenCustodyStatusAndStatusCodeAreDifferent() {
        final Defendant defendant = CaseReceivedHelper.buildDefendantWithCustodyStatus("Mr", UUID.randomUUID().toString(), CUSTODY_STATUS);

        List<BailStatusReferenceData> listBailStatusReferenceData = new ArrayList<>();
        BailStatusReferenceData bailStatusReferenceData = new BailStatusReferenceData(UUID.randomUUID(),1, "D","description","2008-05-05");
        listBailStatusReferenceData.add(bailStatusReferenceData);

        when(referenceDataVO.getBailStatusReferenceData()).thenReturn(listBailStatusReferenceData);

        converter = new ProsecutionCaseToCCPersonDefendantConverter();
        PersonDefendant personDefendant = converter.convert(defendant, referenceDataVO);
        assertNull(personDefendant.getBailStatus());
    }

}
