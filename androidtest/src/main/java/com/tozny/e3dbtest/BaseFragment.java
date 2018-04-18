package com.tozny.e3dbtest;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.tozny.e3db.*;
import com.tozny.e3db.crypto.AndroidConfigStorageHelper;
import com.tozny.e3db.crypto.KeyAuthenticator;
import com.tozny.e3db.crypto.KeyProtection;

import java.io.IOException;
import java.util.UUID;


public class BaseFragment extends Fragment implements BaseFragmentInterface {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private FragmentInteractionListener mListener;

    protected Integer mIndex;
    protected String mName;

    protected enum State {
        UNKNOWN,
        CONFIG_LOADED,
        CONFIG_DELETED,
        ERROR_FOUND
    }

    protected State mState;

    protected Button mLoadConfigButton;
    protected Button mSaveConfigButton;
    protected Button mDeleteConfigButton;
    protected Button mNewConfigButton;

    protected TextView mStatusTextView;
    protected TextView mErrorTextView;
    protected TextView mConfigTextView;

    protected static String TOKEN       = "ce4ed7a4cf50ac5bf231938da4f4b9b5466768e602529eeb57ad1dabb2c66f90";
    protected static String CLIENT_NAME = "LilliTest-";
    protected static String HOST        = "https://api.e3db.com";

    private Config mConfig = null;

    public BaseFragment() {
        // Required empty public constructor
    }

    protected void setArgs(Integer index, String name) {
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, index);
        args.putString(ARG_PARAM2, name);
        this.setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mIndex = getArguments().getInt(ARG_PARAM1);
            mName  = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base, container, false);

        mLoadConfigButton   = view.findViewById(R.id.load_config_button);
        mSaveConfigButton   = view.findViewById(R.id.save_config_button);
        mDeleteConfigButton = view.findViewById(R.id.delete_config_button);
        mNewConfigButton    = view.findViewById(R.id.new_config_button);
        mStatusTextView     = view.findViewById(R.id.status_text_view);
        mErrorTextView      = view.findViewById(R.id.error_text_view);
        mConfigTextView     = view.findViewById(R.id.config_text_view);

        mLoadConfigButton.setOnClickListener(loadConfigButtonOnClickListener);
        mSaveConfigButton.setOnClickListener(saveConfigButtonOnClickListener);
        mDeleteConfigButton.setOnClickListener(deleteConfigButtonOnClickListener);
        mNewConfigButton.setOnClickListener(newConfigButtonOnClickListener);

        mState = State.UNKNOWN;

        clearLabels();
        updateLabels(getString(R.string.unknown_state), "", "");
        updateInterface();

        return view;
    }

    @Override
    public void setMenuVisibility(final boolean visible) { /* Hack to know if this is the visible fragment. */
        super.setMenuVisibility(visible);

        if (visible) {
            if (mListener != null) mListener.setActionBarTitle(mName);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (FragmentInteractionListener) activity;

            if (isMenuVisible() && getArguments() != null)
                mListener.setActionBarTitle(getArguments().getString(ARG_PARAM2));

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private AndroidConfigStorageHelper configStorageHelper() { // TODO: Maybe make this the interface instead of the individual methods?
        return new AndroidConfigStorageHelper(getContext(), configName(), keyProtection(), keyAuthenticationHandler());
    }

    private View.OnClickListener loadConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            updateLabels(getString(R.string.loading_config), "", "");

            Config.loadConfigSecurely(configStorageHelper(), new ConfigStorageHelper.LoadConfigHandler() {
                @Override
                public void loadConfigDidSucceed(String config) {
                    try {
                        mConfig = Config.fromJson(config);
                    } catch (IOException e) {
                        loadConfigDidFail(e);
                    }

                    mState = State.CONFIG_LOADED;

                    updateLabels(getString(R.string.config_loaded), "", mConfig.json());
                    updateInterface();
                }

                @Override
                public void loadConfigDidCancel() {
                    updateLabels(getString(R.string.config_load_canceled), "", "");
                }

                @Override
                public void loadConfigNotFound() {
                    mState = State.CONFIG_DELETED;

                    updateLabels(getString(R.string.config_not_found), "", "");
                    updateInterface();
                }

                @Override
                public void loadConfigDidFail(Throwable e) {
                    e.printStackTrace();

                    mState = State.ERROR_FOUND;

                    updateLabels(getString(R.string.load_config_failed), e.getLocalizedMessage(), "");
                    updateInterface();
                }
            });
        }
    };

    private View.OnClickListener saveConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateLabels(getString(R.string.saving_config), "", "");

            Config.saveConfigSecurely(configStorageHelper(), mConfig.json(), new ConfigStorageHelper.SaveConfigHandler() {
                @Override
                public void saveConfigDidSucceed() {
                    updateLabels(getString(R.string.config_saved), "", mConfig.json());
                }

                @Override
                public void saveConfigDidCancel() {
                    updateLabels(getString(R.string.save_config_canceled), "", "");
                }

                @Override
                public void saveConfigDidFail(Throwable e) {
                    e.printStackTrace();
                    updateLabels(getString(R.string.save_config_failed), e.getLocalizedMessage(), "");
                }
            });
        }
    };

    private View.OnClickListener deleteConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateLabels(getString(R.string.deleting_config), "", "");

            Config.removeConfigSecurely(configStorageHelper(), new ConfigStorageHelper.RemoveConfigHandler() {
                @Override
                public void removeConfigDidSucceed() {
                    mConfig = null;

                    mState = State.CONFIG_DELETED;

                    updateLabels(getString(R.string.config_deleted), "", "");
                    updateInterface();
                }

                @Override
                public void removeConfigDidFail(Throwable e) {
                    e.printStackTrace();

                    mState = State.ERROR_FOUND;

                    updateLabels(getString(R.string.delete_config_failed), e.getLocalizedMessage(), "");
                    updateInterface();
                }
            });
        }
    };

    private View.OnClickListener newConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            updateLabels(getString(R.string.registering_new_client), "", "");

            Client.register(TOKEN, CLIENT_NAME + UUID.randomUUID().toString(), HOST, new ResultHandler<Config>() {
                @Override
                public void handle(Result<Config> r) {
                    if (!r.isError()) {

                        mConfig = r.asValue();
                        Config.saveConfigSecurely(configStorageHelper(), mConfig.json(), new ConfigStorageHelper.SaveConfigHandler() {
                            @Override
                            public void saveConfigDidSucceed() {
                                mState = State.CONFIG_LOADED;

                                updateLabels(getString(R.string.new_config_created_and_saved), "", mConfig.json());
                                updateInterface();
                            }

                            @Override
                            public void saveConfigDidCancel() {
                                updateLabels(getString(R.string.config_create_canceled), "", "");

                            }

                            @Override
                            public void saveConfigDidFail(Throwable e) {
                                e.printStackTrace();

                                mState = State.ERROR_FOUND;

                                updateLabels(getString(R.string.create_config_failed), e.getLocalizedMessage(), "");
                                updateInterface();
                            }
                        });

                    } else {
                        mState = State.ERROR_FOUND;

                        updateLabels(getString(R.string.create_config_failed), (r.asError().error() == null ? r.asError().toString() : r.asError().error().getMessage()), "");
                        updateInterface();

                    }
                }
            });
        }
    };

    protected void clearLabels() {
        mStatusTextView.setText("");
        mErrorTextView.setText("");
        mConfigTextView.setText("");
    }

    protected void updateLabels(String statusText, String errorText, String configText) {
        mStatusTextView.setText(statusText);
        mErrorTextView.setText(errorText);
        mConfigTextView.setText(configText);
    }

    protected void updateInterface() {
        switch (mState) {

            case UNKNOWN:
                mLoadConfigButton.setVisibility(View.VISIBLE);
                mSaveConfigButton.setVisibility(View.GONE);
                mDeleteConfigButton.setVisibility(View.GONE);
                mNewConfigButton.setVisibility(View.GONE);

                break;

            case CONFIG_LOADED:
                mLoadConfigButton.setVisibility(View.VISIBLE);
                mSaveConfigButton.setVisibility(View.VISIBLE);
                mDeleteConfigButton.setVisibility(View.VISIBLE);
                mNewConfigButton.setVisibility(View.GONE);

                break;

            case CONFIG_DELETED:
                mLoadConfigButton.setVisibility(View.GONE);
                mSaveConfigButton.setVisibility(View.GONE);
                mDeleteConfigButton.setVisibility(View.GONE);
                mNewConfigButton.setVisibility(View.VISIBLE);

                break;

            case ERROR_FOUND:
                mDeleteConfigButton.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public String configName() {
        throw new IllegalStateException("Method should be overridden by subclass.");
    }

    @Override
    public KeyProtection keyProtection() {
        throw new IllegalStateException("Method should be overridden by subclass.");
    }

    @Override
    public KeyAuthenticator keyAuthenticationHandler() {
        throw new IllegalStateException("Method should be overridden by subclass.");
    }

    public interface FragmentInteractionListener {
        void setActionBarTitle(String title);
    }
}
