package com.tozny.e3dbtest;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tozny.e3db.crypto.KeyAuthenticator;
import com.tozny.e3db.crypto.KeyProtection;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link KPNoneFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class KPNoneFragment extends BaseFragment {

    public static KPNoneFragment newInstance(Integer index, String name) {
        KPNoneFragment fragment = new KPNoneFragment();
        fragment.setArgs(index, name);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public String configName() {
        return "config";
    }

    @Override
    public KeyProtection keyProtection() {
        return KeyProtection.withNone();
    }

    @Override
    public KeyAuthenticator keyAuthenticationHandler() {
        return null;
    }
}
