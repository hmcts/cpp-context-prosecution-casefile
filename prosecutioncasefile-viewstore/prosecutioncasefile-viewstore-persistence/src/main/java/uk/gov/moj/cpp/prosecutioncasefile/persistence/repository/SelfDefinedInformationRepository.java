package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SelfDefinedInformationRepository extends EntityRepository<SelfDefinedInformationDetails, UUID> {
}