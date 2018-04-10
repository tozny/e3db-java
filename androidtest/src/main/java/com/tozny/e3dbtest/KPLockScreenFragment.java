package com.tozny.e3dbtest;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tozny.e3db.crypto.DefaultIBanana;
import com.tozny.e3db.crypto.IBanana;
import com.tozny.e3db.crypto.KeyProtection;

/**
 * A simple {@link Fragment} subclass.
 */
public class KPLockScreenFragment extends BaseFragment {

    public KPLockScreenFragment() {
        // Required empty public constructor
    }

    public static KPLockScreenFragment newInstance(Integer index, String name) {
        KPLockScreenFragment fragment = new KPLockScreenFragment();
        fragment.setArgs(index, name);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public String configName() {
        return "config-lock-screen";
    }

    @Override
    public KeyProtection keyProtection() {
        return KeyProtection.withLockScreen();
    }

    @Override
    public IBanana keyAuthenticationHandler() {
        return new DefaultIBanana(this.getActivity(), "Lock Screen");
    }
}
