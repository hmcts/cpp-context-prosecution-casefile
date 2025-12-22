package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantLegacy;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantLegacyRepository extends EntityRepository<DefendantLegacy, UUID> {

    List<DefendantLegacy> findByCaseId(UUID caseId);
}
