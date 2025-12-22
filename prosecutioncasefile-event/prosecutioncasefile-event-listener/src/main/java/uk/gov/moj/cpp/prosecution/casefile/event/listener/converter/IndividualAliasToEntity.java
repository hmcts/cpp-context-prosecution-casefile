package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.IndividualAliasDetail;

public class IndividualAliasToEntity implements Converter<IndividualAlias, IndividualAliasDetail> {

    @Override
    public IndividualAliasDetail convert(final IndividualAlias alias) {
        return new IndividualAliasDetail(
                alias.getTitle(),
                alias.getFirstName(),
                alias.getGivenName2(),
                alias.getGivenName3(),
                alias.getLastName());
    }
}
