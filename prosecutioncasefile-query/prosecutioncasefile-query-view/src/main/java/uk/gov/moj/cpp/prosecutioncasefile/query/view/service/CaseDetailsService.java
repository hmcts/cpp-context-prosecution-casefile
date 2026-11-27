package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import org.slf4j.Logger;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;

import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

public class CaseDetailsService {

    private static final Logger LOGGER = getLogger(CaseDetailsService.class);

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    public CaseDetailsView findCase(final UUID id) {
        final CaseDetails caseDetails = caseDetailsRepository.findBy(id);

        return ofNullable(caseDetails)
                .map(CaseDetailsView::new)
                .orElse(null);
    }

    public CaseDetailsView findCaseByProsecutionReferenceId(final String prosecutionCaseReference) {

        CaseDetails caseDetails = null;
        try {
            caseDetails = caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(prosecutionCaseReference);
        }catch (EntityNotFoundException entityNotFoundException){
            LOGGER.error("CaseDetailsView not found for the given prosecutionCaseReference : {}", prosecutionCaseReference, entityNotFoundException);
        }
        return ofNullable(caseDetails)
                .map(CaseDetailsView::new)
                .orElse(null);
    }

    public List<CaseDetailsView> findAllCaseByProsecutionReferenceIds(final List<String> prosecutionCaseReference) {
        final List<CaseDetails> caseDetails = caseDetailsRepository.findAllCaseDetailsByProsecutionCaseReferences(prosecutionCaseReference);

        return caseDetails.stream()
                .filter(Objects::nonNull)
                .map(CaseDetailsView::new)
                .collect(toList());
    }
}
