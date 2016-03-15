package com.woording.android.util;

import java.util.Comparator;
import java.util.Locale;

public class LocaleComparator implements Comparator<Locale> {
    @Override
    public int compare(Locale locale1, Locale locale2) {
        return locale1.getDisplayLanguage().compareTo(locale2.getDisplayLanguage());
    }
}
