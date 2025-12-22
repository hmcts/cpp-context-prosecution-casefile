package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;

import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings({"squid:S1118"})
public final class PCFEnumMap {

    public static final Map<Language, HearingLanguage> getLanguageToDocumentationLanguageNeeds() {
        final EnumMap<Language, HearingLanguage> languageToDocumentationLanguageNeed = new EnumMap(Language.class);

        languageToDocumentationLanguageNeed.put(Language.E, HearingLanguage.ENGLISH);
        languageToDocumentationLanguageNeed.put(Language.W, HearingLanguage.WELSH);
        return languageToDocumentationLanguageNeed;
    }

    public static final Map<Language, HearingLanguage> getLanguageToHearingLanguageNeeds() {
        final EnumMap<Language, HearingLanguage> languageToHearingLanguageNeed = new EnumMap(Language.class);

        languageToHearingLanguageNeed.put(Language.E, HearingLanguage.ENGLISH);
        languageToHearingLanguageNeed.put(Language.W, HearingLanguage.WELSH);
        return languageToHearingLanguageNeed;
    }

    public static final Map<Language, HearingLanguage> getHearingPCFToProgressionMap() {
        final EnumMap<Language, HearingLanguage> hearingPCFToProgressionMap = new EnumMap(Language.class);
        hearingPCFToProgressionMap.put(Language.E, HearingLanguage.ENGLISH);
        hearingPCFToProgressionMap.put(Language.W, HearingLanguage.WELSH);
        return hearingPCFToProgressionMap;
    }

}
