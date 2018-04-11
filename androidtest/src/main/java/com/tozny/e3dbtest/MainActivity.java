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
