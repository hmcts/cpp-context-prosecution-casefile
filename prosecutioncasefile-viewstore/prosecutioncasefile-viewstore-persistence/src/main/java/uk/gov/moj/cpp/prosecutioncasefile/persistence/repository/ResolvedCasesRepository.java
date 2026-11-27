package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases_;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.apache.deltaspike.data.api.FullEntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ResolvedCasesRepository extends FullEntityRepository<ResolvedCases, UUID> {

    default Long countOfCasesFixedByDate(final LocalDate resolutionDate,
                                 final Optional<String> region,
                                 final Optional<String> courtLocation,
                                 final Optional<String> caseType) {
        return criteria().select(Long.class, countDistinct(ResolvedCases_.caseId))
                .eq(ResolvedCases_.resolutionDate, resolutionDate)
                .eqIgnoreCase(ResolvedCases_.region, region.orElse(null))
                .eqIgnoreCase(ResolvedCases_.courtLocation, courtLocation.orElse(null))
                .eqIgnoreCase(ResolvedCases_.caseType, caseType.orElse(null))
                .getSingleResult();
    }
}