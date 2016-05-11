package com.woording.android.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.woording.android.R;

public class ViewPagerAdapter extends PagerAdapter {

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        int id = 0;
        switch (position) {
            case 0:
                id = R.id.login_page;
                break;
            case 1:
                id = R.id.register_page;
                break;
        }
        return collection.findViewById(id);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
