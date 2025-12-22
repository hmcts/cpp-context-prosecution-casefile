package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.AssociatedPerson;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrganisationPersonMapper {

    public static final Function<List<AssociatedPerson>, List<uk.gov.justice.core.courts.AssociatedPerson>> convertAssociatedPerson
            = sourceAssociatedPerson -> ofNullable(sourceAssociatedPerson)
            .map(associatedPersonList -> associatedPersonList.stream()
                    .map(associatedPerson -> uk.gov.justice.core.courts.AssociatedPerson.associatedPerson()
                            .withPerson(PersonMapper.convertPerson.apply(associatedPerson.getPerson()))
                            .withRole(associatedPerson.getRole()).build())
                    .collect(Collectors.toList())
            )
            .orElse(null);

    private OrganisationPersonMapper() {
    }
}
