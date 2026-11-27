package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseDetailsRepository extends EntityRepository<CaseDetails, UUID> {

    @Query(value = "FROM CaseDetails cd WHERE cd.prosecutionCaseReference = :prosecutionCaseReference")
    CaseDetails findCaseDetailsByProsecutionCaseReference(@QueryParam("prosecutionCaseReference") final String prosecutionCaseReference);

    @Query(value = "FROM CaseDetails cd WHERE cd.prosecutionCaseReference in (:prosecutionCaseReferences) ")
    List<CaseDetails> findAllCaseDetailsByProsecutionCaseReferences(@QueryParam("prosecutionCaseReferences") final Collection<String> caseIds);
}
