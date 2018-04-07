package com.tozny.e3dbtest;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

public class Main4Activity extends AppCompatActivity {

    private TextView mTextMessage;

    private ViewPager mViewPager;

    private KPNoneFragment        mNoneFragment;
    private KPPasswordFragment    mPasswordFragment;
    private KPLockScreenFragment  mLockScreenFragment;
    private KPFingerPrintFragment mFingerPrintFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        mTextMessage = findViewById(R.id.message);

        mNoneFragment        = KPNoneFragment.newInstance(0, getString(R.string.title_kp_none));
        mPasswordFragment    = KPPasswordFragment.newInstance(1, getString(R.string.title_kp_password));
        mLockScreenFragment  = KPLockScreenFragment.newInstance(2, getString(R.string.title_kp_lock_screen));
        mFingerPrintFragment = KPFingerPrintFragment.newInstance(3, getString(R.string.title_kp_fingerprint));

        switchFragment(mNoneFragment, getString(R.string.title_kp_none));

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) { // TODO: Lilli, put these in an array to simplify method
            switch (item.getItemId()) {
                case R.id.navigation_kp_none:
                    switchFragment(mNoneFragment, getString(R.string.title_kp_password));

                    return true;

                case R.id.navigation_kp_password:
                    switchFragment(mPasswordFragment, getString(R.string.title_kp_lock_screen));

                    return true;

                case R.id.navigation_kp_lock_screen:
                    switchFragment(mLockScreenFragment, getString(R.string.title_kp_lock_screen));

                    return true;

                case R.id.navigation_kp_fingerprint:
                    switchFragment(mFingerPrintFragment, getString(R.string.title_kp_fingerprint));

                    return true;

            }

            return false;
        }
    };

    private void switchFragment(BaseFragment fragment, String text) {
        if (getActionBar() != null) getActionBar().setTitle(text);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(text);  // provide compatibility to all the versions

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.content_fragment, fragment);
        transaction.commit();
    }
}
