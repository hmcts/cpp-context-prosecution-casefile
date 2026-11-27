package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorCaseDetails;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface BusinessValidationErrorCaseDetailsRepository extends
        EntityRepository<BusinessValidationErrorCaseDetails, UUID> {

    List<BusinessValidationErrorCaseDetails> findByCaseId(final UUID caseId);

    void deleteByCaseId(final UUID caseId);


}
