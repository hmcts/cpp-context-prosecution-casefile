package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SelfDefinedInformationRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public SelfDefinedInformationDetails findBy(final UUID id) {
        return entityManager.find(SelfDefinedInformationDetails.class, id.toString());
    }

    public SelfDefinedInformationDetails save(final SelfDefinedInformationDetails entity) {
        return entityManager.merge(entity);
    }

    public void remove(final SelfDefinedInformationDetails entity) {
        final SelfDefinedInformationDetails managed =
                entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
