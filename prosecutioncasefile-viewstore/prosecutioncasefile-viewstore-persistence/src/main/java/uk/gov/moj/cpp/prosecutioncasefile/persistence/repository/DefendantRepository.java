package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;

import java.util.List;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantRepository extends EntityRepository<DefendantDetails, String> {

    List<DefendantDetails> findByDefendantId(final String defendantId);

}
