package com.woording.android.adapter;

import android.content.Context;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.woording.android.util.LocaleComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

public class LanguageAdapter extends ArrayAdapter<String> {

    private final LayoutInflater mInflater;
    private final int mResource;
    private int mDropDownResource;

    private final ArrayList<Locale> mLocales = new ArrayList<>(Arrays.asList(Locale.getAvailableLocales()));

    public LanguageAdapter(Context context, @LayoutRes int resource) {
        super(context, resource, 0, new ArrayList<String>());

        mInflater = LayoutInflater.from(context);
        mResource = mDropDownResource = resource;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Iterator<Locale> iterator = mLocales.iterator(); iterator.hasNext(); ) {
                Locale locale = iterator.next();
                if (locale.getCountry().length() > 0 || locale.getScript().length() > 0) {
                    iterator.remove();
                }
            }
        } else {
            for (Iterator<Locale> iterator = mLocales.iterator(); iterator.hasNext(); ) {
                Locale locale = iterator.next();
                if (locale.getCountry().length() > 0) {
                    iterator.remove();
                }
            }
        }
        Collections.sort(mLocales, new LocaleComparator());
    }

    public ArrayList<Locale> getLocales() {
        return mLocales;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, mResource);
    }

    private View createViewFromResource(LayoutInflater inflater, int position, View convertView,
                                        ViewGroup parent, int resource) {
        View view;
        TextView text;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        text = (TextView) view;

        String item = mLocales.get(position).getDisplayLanguage();
        text.setText(item);

        return view;
    }

    @Override
    public void setDropDownViewResource(@LayoutRes int resource) {
        this.mDropDownResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        final LayoutInflater inflater = mInflater;
        return createViewFromResource(inflater, position, convertView, parent, mDropDownResource);
    }

    @Override
    public String getItem(int position) {
        String languageCode = mLocales.get(position).getISO3Language();
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

    @Override
    public int getCount() {
        return mLocales.size();
    }

    @Override
    public int getPosition(String item) {
        ArrayList<String> items = new ArrayList<>();
        for (Locale locale : mLocales) {
            items.add(locale.getDisplayLanguage());
        }
        return items.indexOf(item);
    }

}
