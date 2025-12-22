package uk.gov.moj.cpp.prosecution.casefile.helper;

import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_BCM_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_COTR_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_PET_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_UPDATE_COTR_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED;

public class CpsServeMaterialHelper extends AbstractTestHelper {

    public CpsServeMaterialHelper() {
        createPrivateConsumerForMultipleSelectors(
                EVENT_SELECTOR_CPS_SERVE_PET_RECEIVED,
                EVENT_SELECTOR_CPS_SERVE_BCM_RECEIVED,
                EVENT_SELECTOR_CPS_SERVE_COTR_RECEIVED,
                EVENT_SELECTOR_CPS_UPDATE_COTR_RECEIVED);

        createPublicConsumerForMultipleSelectors(
                PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED,
                PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED,
                PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED,
                PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED,
                PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED);
    }

}
