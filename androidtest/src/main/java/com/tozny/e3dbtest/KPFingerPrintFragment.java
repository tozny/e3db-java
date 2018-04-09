package com.tozny.e3dbtest;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tozny.e3db.crypto.IBanana;
import com.tozny.e3db.crypto.KeyProtection;

/**
 * A simple {@link Fragment} subclass.
 */
public class KPFingerPrintFragment extends BaseFragment {

    public KPFingerPrintFragment() {
        // Required empty public constructor
    }

    public static KPFingerPrintFragment newInstance(Integer index, String name) {
        KPFingerPrintFragment fragment = new KPFingerPrintFragment();
        fragment.setArgs(index, name);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public String configName() {
        return "config-fingerprint";
    }

    @Override
    public KeyProtection keyProtection() {
        return KeyProtection.withFingerprint();
    }

    @Override
    public IBanana keyAuthenticationHandler() {
        return null;
    }
}
