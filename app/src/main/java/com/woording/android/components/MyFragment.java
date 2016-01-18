package com.woording.android.components;

import android.os.Build;
import android.support.v4.app.Fragment;

public class MyFragment extends Fragment {

    public void finishApp() {
        if (Build.VERSION.SDK_INT >= 16) {
            getActivity().finishAffinity();
        } else {
            getActivity().finish();
            System.exit(0);
        }
    }
}
