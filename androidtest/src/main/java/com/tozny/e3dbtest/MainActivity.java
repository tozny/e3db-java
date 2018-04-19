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
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements BaseFragment.FragmentInteractionListener {
    private ViewPager mPager;
    private BottomNavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPager = findViewById(R.id.pager);
        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        mPager.addOnPageChangeListener(mOnPageChangeListener);
        mPager.setOffscreenPageLimit(3);

        mNavigationView = findViewById(R.id.navigation);
        mNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mPager.setCurrentItem(navIdToPageIndex(0));
    }

    private int navIdToPageIndex(int navId) {
        switch (navId) {
            case R.id.navigation_kp_none:        return 0;
            case R.id.navigation_kp_password:    return 1;
            case R.id.navigation_kp_lock_screen: return 2;
            case R.id.navigation_kp_fingerprint: return 3;
        }

        return -1;
    }

    private int pageIndexToNavId(int pageIndex) {
        switch (pageIndex) {
            case 0: return R.id.navigation_kp_none;
            case 1: return R.id.navigation_kp_password;
            case 2: return R.id.navigation_kp_lock_screen;
            case 3: return R.id.navigation_kp_fingerprint;
        }

        return -1;
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mNavigationView.setSelectedItemId(pageIndexToNavId(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            mPager.setCurrentItem(navIdToPageIndex(item.getItemId()));
            return true;
        }
    };

    @Override
    public void setActionBarTitle(String title) {
        if (getActionBar() != null) getActionBar().setTitle(title);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Fragment> fragments;

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);

            KPNoneFragment        mNoneFragment        = KPNoneFragment.newInstance(0, getString(R.string.title_kp_none));
            KPPasswordFragment    mPasswordFragment    = KPPasswordFragment.newInstance(1, getString(R.string.title_kp_password));
            KPLockScreenFragment  mLockScreenFragment  = KPLockScreenFragment.newInstance(2, getString(R.string.title_kp_lock_screen));
            KPFingerPrintFragment mFingerPrintFragment = KPFingerPrintFragment.newInstance(3, getString(R.string.title_kp_fingerprint));

            fragments = new ArrayList<Fragment>(
                Arrays.asList(mNoneFragment, mPasswordFragment, mLockScreenFragment, mFingerPrintFragment));
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}
