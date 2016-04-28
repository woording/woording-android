package com.woording.android.util;

import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class ConvertLanguage {

    private final static ArrayList<Locale> locales = new ArrayList<>(Arrays.asList(Locale.getAvailableLocales()));
//    private static Map<String, String> languageMap = null;
    private static Map<String, String> isoMap = null;

//    public static String toISO3(String language) {
//        if (languageMap == null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                for (Iterator<Locale> iterator = locales.iterator(); iterator.hasNext(); ) {
//                    Locale locale = iterator.next();
//                    if (locale.getCountry().length() > 0 || locale.getScript().length() > 0) {
//                        iterator.remove();
//                    }
//                }
//            } else {
//                for (Iterator<Locale> iterator = locales.iterator(); iterator.hasNext(); ) {
//                    Locale locale = iterator.next();
//                    if (locale.getCountry().length() > 0) {
//                        iterator.remove();
//                    }
//                }
//            }
//            Collections.sort(locales, new LocaleComparator());
//
//            languageMap = new HashMap<>(locales.size());
//            for (Locale locale : locales) {
//                String iso3 = getISO3(locale);
//                languageMap.put(locale.getDisplayLanguage(), iso3);
//            }
//        }
//
//        return languageMap.get(language);
//    }

    public static String toLang(String iso3) {
        if (isoMap == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (Iterator<Locale> iterator = locales.iterator(); iterator.hasNext(); ) {
                    Locale locale = iterator.next();
                    if (locale.getCountry().length() > 0 || locale.getScript().length() > 0) {
                        iterator.remove();
                    }
                }
            } else {
                for (Iterator<Locale> iterator = locales.iterator(); iterator.hasNext(); ) {
                    Locale locale = iterator.next();
                    if (locale.getCountry().length() > 0) {
                        iterator.remove();
                    }
                }
            }
            Collections.sort(locales, new LocaleComparator());

            isoMap = new HashMap<>(locales.size());
            for (Locale locale : locales) {
                String iso = getISO3(locale);
                isoMap.put(iso, locale.getDisplayLanguage());
            }
        }
        return isoMap.get(iso3);
    }

    public static String getISO3(Locale locale) {
        String languageCode = locale.getISO3Language();
        switch (languageCode) {
            case "sqi":
                languageCode = "alb";
                break;
            case "hye":
                languageCode = "arm";
                break;
            case "eus":
                languageCode = "baq";
                break;
            case "mya":
                languageCode = "bur";
                break;
            case "zho":
                languageCode = "chi";
                break;
            case "ces":
                languageCode = "cze";
                break;
            case "nld":
                languageCode = "dut";
                break;
            case "fra":
                languageCode = "fre";
                break;
            case "kat":
                languageCode = "geo";
                break;
            case "deu":
                languageCode = "ger";
                break;
            case "ell":
                languageCode = "gre";
                break;
            case "isl":
                languageCode = "ice";
                break;
            case "mkd":
                languageCode = "mac";
                break;
            case "msa":
                languageCode = "may";
                break;
            case "mri":
                languageCode = "mao";
                break;
            case "fas":
                languageCode = "per";
                break;
            case "ron":
                languageCode = "rum";
                break;
            case "slk":
                languageCode = "slo";
                break;
            case "bod":
                languageCode = "tib";
                break;
            case "cym":
                languageCode = "wel";
                break;
            default:
                break;
        }
        return languageCode;
    }
}
