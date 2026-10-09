package uk.gov.moj.cpp.enterpriseid.generator;

/**
 * Interface for generating a human-readable enterprie ID.
 */
public interface EnterpriseIdGenerator {

    /**
     * Generates a human-readable ID.
     *
     * @return a human readable ID string.
     */
    String enterpriseId();
}
