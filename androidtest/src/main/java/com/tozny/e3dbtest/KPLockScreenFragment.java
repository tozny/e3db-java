/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3dbtest;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.tozny.e3db.KeyAuthenticator;
import com.tozny.e3db.KeyAuthentication;

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
        return "config-ls";
    }

    @Override
    public KeyAuthentication keyProtection() {
        return KeyAuthentication.withLockScreen();
    }

    @Override
    public KeyAuthenticator keyAuthenticationHandler() {
        return KeyAuthenticator.defaultAuthenticator(this.getActivity(), getString(R.string.title_kp_lock_screen));
    }
}
