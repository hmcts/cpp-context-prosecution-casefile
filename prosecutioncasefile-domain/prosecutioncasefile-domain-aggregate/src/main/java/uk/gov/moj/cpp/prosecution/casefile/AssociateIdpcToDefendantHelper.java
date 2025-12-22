package uk.gov.moj.cpp.prosecution.casefile;

import static java.time.LocalDate.parse;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AssociateIdpcToDefendantHelper {

    private UUID caseId;
    private List<Defendant> defendants;

    public AssociateIdpcToDefendantHelper(final UUID caseId, final List<Defendant> defendants) {

        this.caseId = caseId;
        this.defendants = unmodifiableList(defendants);
    }

    private boolean isSurnameMatch(final Defendant currentDefendant, final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant cmsDefendant) {
        final PersonalInformation personalInformation = currentDefendant.getIndividual().getPersonalInformation();
        return isNotBlank(personalInformation.getLastName())  && isNotBlank(cmsDefendant.getSurname())  && equalsIgnoreCase(trim(personalInformation.getLastName()), trim(cmsDefendant.getSurname()));
    }

    private boolean isFirstNameMatch(final Defendant currentDefendant, final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant cmsDefendant) {
        final PersonalInformation personalInformation = currentDefendant.getIndividual().getPersonalInformation();
        return isNotBlank(personalInformation.getFirstName()) && isNotBlank(cmsDefendant.getForenames()) && equalsIgnoreCase(trim(personalInformation.getFirstName()), trim(cmsDefendant.getForenames()));
    }

    private boolean isDateOfBirthMatch(final Defendant currentDefendant, final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant cmsDefendant) {
        final SelfDefinedInformation selfDefinedInformation = currentDefendant.getIndividual().getSelfDefinedInformation();
        return selfDefinedInformation.getDateOfBirth() != null && cmsDefendant.getDob() != null && selfDefinedInformation.getDateOfBirth().equals(parse(cmsDefendant.getDob()));
    }

    private Optional<Defendant> matchedDefendantByDOB(final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant cmsDefendant, final List<Defendant> currentDefendantList) {
        final List<Defendant> dateOfBirthMatched = currentDefendantList.stream().filter(currentDefendant -> isDateOfBirthMatch(currentDefendant , cmsDefendant)).collect(toList());
        if(!dateOfBirthMatched.isEmpty()) {
            return dateOfBirthMatched.size() == 1 ? dateOfBirthMatched.stream().findFirst() : empty();
        }
        return empty();
    }

    private Optional<Defendant> matchedDefendantByFirstNameAndDOB(final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant cmsDefendant, final List<Defendant> currentDefendantList) {
        final List<Defendant> firstNameMatched = currentDefendantList.stream().filter(currentDefendant -> isFirstNameMatch( currentDefendant, cmsDefendant)).collect(toList());
        if(!firstNameMatched.isEmpty()) {
            return firstNameMatched.size() == 1 ? firstNameMatched.stream().findFirst() : matchedDefendantByDOB(cmsDefendant, firstNameMatched);
        }
        return empty();
    }

    public Optional<Defendant> associateDefendant(final UUID cmsCaseId, final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant cmsDefendant) {
        if (this.caseId!=null && this.caseId.equals(cmsCaseId)) {

            final List<Defendant> surnameMatchedList = this.defendants.stream().filter(d -> isSurnameMatch(d, cmsDefendant)).collect(toList());

            if(!surnameMatchedList.isEmpty()) {

                final Optional<Defendant> associatedDefendant = surnameMatchedList.size() == 1 ? surnameMatchedList.stream().findFirst() : matchedDefendantByFirstNameAndDOB(cmsDefendant, surnameMatchedList);

                if (associatedDefendant.isPresent()) {
                    return associatedDefendant;
                }
            }
        }
        return empty();
    }

}
