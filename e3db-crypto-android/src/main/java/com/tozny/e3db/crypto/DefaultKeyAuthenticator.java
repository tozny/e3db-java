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

package com.tozny.e3db.crypto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.PermissionChecker;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.tozny.e3dbandroidcrypto.R;

import java.security.GeneralSecurityException;
import java.security.UnrecoverableKeyException;

import static android.app.Activity.RESULT_OK;

/**
 * Performs fingerprint authentication to unlock protected keys.
 */
public final class DefaultKeyAuthenticator extends KeyAuthenticator {
  private final Activity activity;
  private final String title;

  /**
   * Create an instance that will display over the given activity.
   *
   * @param activity
   *         Activity that will host the dialog
   * @param title
   *         Title of the fingerprint dialog.
   */
  public DefaultKeyAuthenticator(Activity activity, String title) {
    if (activity == null)
      throw new IllegalArgumentException("activity");
    if (title == null)
      throw new IllegalArgumentException("title");

    this.activity = activity;
    this.title = title;
  }

  /**
   * A dialog which uses fingerprint APIs to authenticate the user. Not for public consumption,
   * but required to be so by fragments API.
   */
  @TargetApi(23)
  public static class FingerprintAuthDialogFragment extends DialogFragment
      implements FingerprintUiHelper.Callback {

    private Activity mActivity;
    private Callback mCallback;
    private FingerprintManagerCompat.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private String mTitle;

    public interface Callback {
      void onFingerprintAuthenticated(FingerprintManagerCompat.CryptoObject cryptoObject);

      void onFingerprintCancel();
    }

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      mActivity = getActivity();
    }

    @Override
    public void onAuthenticated(FingerprintManagerCompat.CryptoObject cryptoObject) {
      // Callback from FingerprintUiHelper. Let the activity know that authentication was
      // successful.
      if (mCallback != null) {
        mCallback.onFingerprintAuthenticated(cryptoObject);
      }
      dismiss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Do not create a new Fragment when the Activity is re-created such as orientation changes.
      setRetainInstance(true);
      setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      if (mTitle != null) {
        getDialog().setTitle(mTitle);
      } else {
        getDialog().setTitle(getString(R.string.sign_in));
      }
      View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
      Button mCancelButton = v.findViewById(R.id.cancel_button);
      mCancelButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          if (mCallback != null) {
            mCallback.onFingerprintCancel();
          }
          dismiss();
        }
      });

      mFingerprintUiHelper = new FingerprintUiHelper(FingerprintManagerCompat.from(this.getActivity()),
          (ImageView) v.findViewById(R.id.fingerprint_icon),
          (TextView) v.findViewById(R.id.fingerprint_status), this);

      return v;
    }

    @Override
    public void onError(String errString) {
    }

    @Override
    public void onPause() {
      super.onPause();
      mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onResume() {
      super.onResume();
      mFingerprintUiHelper.startListening(mCryptoObject);
    }

    public void setCallback(Callback callback) {
      mCallback = callback;
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManagerCompat.CryptoObject cryptoObject) {
      mCryptoObject = cryptoObject;
    }

    public void setTitle(String title) {
      mTitle = title;
    }

    public void setTitle(int titleId) {
      mTitle = getString(titleId);
    }
  }

  /**
   * Small helper class to manage text/icon around fingerprint authentication UI.
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private static class FingerprintUiHelper extends FingerprintManagerCompat.AuthenticationCallback {

    private static final long ERROR_TIMEOUT_MILLIS = 1600;
    private static final long SUCCESS_DELAY_MILLIS = 1300;
    private final Callback mCallback;
    private final TextView mErrorTextView;
    private final FingerprintManagerCompat mFingerprintManager;
    private final ImageView mIcon;
    private CancellationSignal mCancellationSignal;
    private Runnable mResetErrorTextRunnable = new Runnable() {
      @Override
      public void run() {
        mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.hint_color, null));
        mErrorTextView.setText(mErrorTextView.getResources().getString(R.string.fingerprint_hint));
        mIcon.setImageResource(R.drawable.ic_fp_40px);
      }
    };
    private boolean mSelfCancelled;

    /**
     * Constructor for {@link FingerprintUiHelper}.
     */
    FingerprintUiHelper(FingerprintManagerCompat fingerprintManager,
                        ImageView icon, TextView errorTextView, Callback callback) {
      mFingerprintManager = fingerprintManager;
      mIcon = icon;
      mErrorTextView = errorTextView;
      mCallback = callback;
    }

    private void showError(CharSequence error) {
      mIcon.setImageResource(R.drawable.ic_fingerprint_error);
      mErrorTextView.setText(error);
      mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.warning_color, null));
      mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
      mErrorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    public interface Callback {
      void onAuthenticated(FingerprintManagerCompat.CryptoObject cryptoObject);

      void onError(String errString);
    }

    @SuppressLint("MissingPermission")
    public boolean isFingerprintAuthAvailable() {
      return mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();
    }

    @Override
    public void onAuthenticationError(int errMsgId, final CharSequence errString) {
      if (!mSelfCancelled) {
        showError(errString);
        mIcon.postDelayed(new Runnable() {
          @Override
          public void run() {
            mCallback.onError(errString.toString());
          }
        }, ERROR_TIMEOUT_MILLIS);
      }
    }

    @Override
    public void onAuthenticationFailed() {
      showError(mIcon.getResources().getString(R.string.fingerprint_not_recognized));
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
      showError(helpString);
    }

    @Override
    public void onAuthenticationSucceeded(final FingerprintManagerCompat.AuthenticationResult result) {
      mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
      mIcon.setImageResource(R.drawable.ic_fingerprint_success);
      mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.success_color, null));
      mErrorTextView.setText(mErrorTextView.getResources().getString(R.string.fingerprint_success));
      mIcon.postDelayed(new Runnable() {
        @Override
        public void run() {
          mCallback.onAuthenticated(result.getCryptoObject());
        }
      }, SUCCESS_DELAY_MILLIS);
    }

    @SuppressLint("MissingPermission")
    public void startListening(FingerprintManagerCompat.CryptoObject cryptoObject) {
      if (!isFingerprintAuthAvailable()) {
        return;
      }
      mCancellationSignal = new CancellationSignal();
      mSelfCancelled = false;
      mFingerprintManager
          .authenticate(cryptoObject, 0 /* flags */, mCancellationSignal, this, null);
      mIcon.setImageResource(R.drawable.ic_fp_40px);
    }

    public void stopListening() {
      if (mCancellationSignal != null) {
        mSelfCancelled = true;
        mCancellationSignal.cancel();
        mCancellationSignal = null;
      }
    }
  }

  /**
   * Whether the device supports fingerprint authentication or not.
   *
   * @return
   */
  @SuppressLint("MissingPermission")
  public boolean fingerprintSupported() {
    FingerprintManagerCompat fm = FingerprintManagerCompat.from(this.activity);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return PermissionChecker.checkSelfPermission(this.activity, Manifest.permission.USE_FINGERPRINT) == PermissionChecker.PERMISSION_GRANTED &&
                 fm.isHardwareDetected() &&
                 fm.hasEnrolledFingerprints();
    } else
      return false;
  }

  /**
   * Public for technical reasons but shouldn't be.
   */
  @RequiresApi(Build.VERSION_CODES.M)
  public static final class DeviceCredentialsFragment extends Fragment {
    private final DeviceLockAuthenticatorCallbackHandler cont;
    private final String title;
    private final KeyguardManager mgr;

    public DeviceCredentialsFragment() {
      this.cont = null;
      this.title = null;
      this.mgr = null;
    }


    @SuppressLint("ValidFragment")
      // Only used internally
    DeviceCredentialsFragment(DeviceLockAuthenticatorCallbackHandler cont, String title, KeyguardManager mgr) {
      if (mgr == null)
        throw new IllegalArgumentException("mgr");
      if (cont == null)
        throw new IllegalArgumentException("cont");
      if (title == null)
        throw new IllegalArgumentException("title");

      this.title = title;
      this.cont = cont;
      this.mgr = mgr;
    }

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      if (mgr != null && cont != null && title != null) {
        Intent confirmDeviceCredentialIntent = mgr.createConfirmDeviceCredentialIntent(title, "");
        if (confirmDeviceCredentialIntent != null)
          startActivityForResult(confirmDeviceCredentialIntent, 1);
        else
          cont.handleError(new GeneralSecurityException("Device credentials not set up."));
      }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (this.cont != null) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
          // TODO: Need error handling
          this.cont.handleAuthenticated();
        } else if (requestCode == 1) {
          this.cont.handleCancel();
        }
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public void authenticateWithLockScreen(DeviceLockAuthenticatorCallbackHandler cont) {
    DeviceCredentialsFragment f = new DeviceCredentialsFragment(cont, title, (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE));
    FragmentManager fragmentManager = activity.getFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.add(f, "device_credentials_fragment");
    fragmentTransaction.commit();
  }

  final int[] wrongPasswordCount = {0};

  @Override
  public void getPassword(final PasswordAuthenticatorCallbackHandler handler) {
    this.activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Context ctx = DefaultKeyAuthenticator.this.activity;

        final EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        new AlertDialog.Builder(DefaultKeyAuthenticator.this.activity)
            .setMessage(ctx.getString(R.string.key_provider_please_enter_pin))
            .setPositiveButton(ctx.getString(R.string.key_provider_ok), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                try {
                  handler.handlePassword(input.getText().toString());

                } catch (UnrecoverableKeyException e) {
                  wrongPasswordCount[0]++;

                  if (wrongPasswordCount[0] >= 3) {
                    handler.handleError(new RuntimeException("Too many password tries."));
                  } else {
                    Toast.makeText(DefaultKeyAuthenticator.this.activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                    getPassword(handler);
                  }
                }
              }
            })
            .setNegativeButton(ctx.getString(R.string.key_provider_cancel), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                handler.handleCancel();
              }
            })
            .setView(input)
            .show();

        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
          @Override
          public void onFocusChange(View view, boolean b) {
            if (input.isEnabled() && input.isFocusable()) {
              input.post(new Runnable() {
                @Override
                public void run() {
                  final InputMethodManager imm = (InputMethodManager) DefaultKeyAuthenticator.this.activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                  imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }
              });
            }
          }
        });
      }
    });
  }

  @Override
  public void authenticateWithFingerprint(FingerprintManagerCompat.CryptoObject cryptoObject, final DeviceLockAuthenticatorCallbackHandler handler) {
    try {
      FingerprintAuthDialogFragment fragment = new FingerprintAuthDialogFragment();
      fragment.setCryptoObject(cryptoObject);
      fragment.setTitle(title);

      fragment.setCallback(new FingerprintAuthDialogFragment.Callback() {
        public void onFingerprintAuthenticated(FingerprintManagerCompat.CryptoObject cryptoObject) {
          handler.handleAuthenticated();
        }

        public void onFingerprintCancel() {
          handler.handleCancel();
        }
      });

      fragment.show(activity.getFragmentManager(), "fingerprintUI");
    } catch (Throwable e) {
      handler.handleError(e);
    }
  }
}
