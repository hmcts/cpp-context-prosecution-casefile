package uk.gov.moj.cpp.prosecution.casefile.stub;

import static uk.gov.moj.cpp.prosecution.casefile.it.BaseIT.CONTEXT_NAME;

import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;

import com.google.common.collect.ImmutableMap;

/**
 * Switches an Azure feature toggle on or off for this context. Stubs the feature manager in WireMock
 * and clears the framework's feature cache so the new value is read straight away.
 */
public class FeatureToggleStub {

    private FeatureToggleStub() {
    }

    public static void setFeatureToggle(final String featureName, final boolean enabled) {
        FeatureStubber.clearCache(CONTEXT_NAME);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, ImmutableMap.of(featureName, enabled));
    }
}
