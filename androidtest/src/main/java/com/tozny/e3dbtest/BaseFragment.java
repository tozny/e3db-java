package com.tozny.e3dbtest;


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


/**
 * A simple {@link Fragment} subclass.
 */
public class BaseFragment extends Fragment implements BaseFragmentInterface {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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

    protected TextView mErrorTextView;
    protected TextView mStatusTextView;
    protected TextView mConfigTextView;

    protected static String TOKEN       = "ce4ed7a4cf50ac5bf231938da4f4b9b5466768e602529eeb57ad1dabb2c66f90";
    protected static String CLIENT_NAME = "LilliTest-";
    protected static String HOST        = "https://api.e3db.com";

    private Config mConfig = null;

    //protected static Client client = null;

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

        TextView nameLabel = view.findViewById(R.id.name_label);
        nameLabel.setText(mName);

        mLoadConfigButton   = view.findViewById(R.id.load_config_button);
        mSaveConfigButton   = view.findViewById(R.id.save_config_button);
        mDeleteConfigButton = view.findViewById(R.id.delete_config_button);
        mNewConfigButton    = view.findViewById(R.id.new_config_button);
        mErrorTextView      = view.findViewById(R.id.error_text_view);
        mStatusTextView     = view.findViewById(R.id.status_text_view);
        mConfigTextView     = view.findViewById(R.id.config_text_view);

        mLoadConfigButton.setOnClickListener(loadConfigButtonOnClickListener);
        mSaveConfigButton.setOnClickListener(saveConfigButtonOnClickListener);
        mDeleteConfigButton.setOnClickListener(deleteConfigButtonOnClickListener);
        mNewConfigButton.setOnClickListener(newConfigButtonOnClickListener);

        mState = State.UNKNOWN;

        clearLabels();
        updateLabels("", "Unknown State", "");
        updateInterface();

        return view;
    }

    private AndroidConfigStorageHelper configStorageHelper() { // TODO: Maybe make this the interface instead of the individual methods?
        return new AndroidConfigStorageHelper(getContext(), configName(), keyProtection(), keyAuthenticationHandler());
    }

    private View.OnClickListener loadConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            updateLabels("", "Loading Config...", "");

            Config.loadConfigSecurely(configStorageHelper(), new ConfigStorageHelper.LoadConfigHandler() {
                @Override
                public void loadConfigDidSucceed(String config) {
                    try {
                        mConfig = Config.fromJson(config);
                    } catch (IOException e) {
                        loadConfigDidFail(e);
                    }

                    mState = State.CONFIG_LOADED;

                    updateLabels("", "Config Loaded", mConfig.json());
                    updateInterface();
                }

                @Override
                public void loadConfigDidCancel() {
                    updateLabels("", "Config Load Canceled", "");
                }

                @Override
                public void loadConfigNotFound() {
                    mState = State.CONFIG_DELETED;

                    updateLabels("", "No Config Found", "");
                    updateInterface();
                }

                @Override
                public void loadConfigDidFail(Throwable e) {
                    e.printStackTrace();

                    mState = State.ERROR_FOUND;

                    updateLabels(e.getLocalizedMessage(), "Load Config Failed", "");
                    updateInterface();
                }
            });
        }
    };

    private View.OnClickListener saveConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateLabels("", "Saving Config...", "");

            Config.saveConfigSecurely(configStorageHelper(), mConfig.json(), new ConfigStorageHelper.SaveConfigHandler() {
                @Override
                public void saveConfigDidSucceed() {
                    updateLabels("", "Config Saved", mConfig.json());
                }

                @Override
                public void saveConfigDidCancel() {
                    updateLabels("", "Config Save Canceled", "");
                }

                @Override
                public void saveConfigDidFail(Throwable e) {
                    e.printStackTrace();
                    updateLabels(e.getLocalizedMessage(), "Save Config Failed", "");
                }
            });
        }
    };

    private View.OnClickListener deleteConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateLabels("", "Deleting Config...", "");

            Config.removeConfigSecurely(configStorageHelper(), new ConfigStorageHelper.RemoveConfigHandler() {
                @Override
                public void removeConfigDidSucceed() {
                    mConfig = null;

                    mState = State.CONFIG_DELETED;

                    updateLabels("", "Config Deleted", "");
                    updateInterface();
                }

                @Override
                public void removeConfigDidFail(Throwable e) {
                    e.printStackTrace();

                    mState = State.ERROR_FOUND;

                    updateLabels(e.getLocalizedMessage(), "Delete Config Failed", "");
                    updateInterface();
                }
            });
        }
    };

    private View.OnClickListener newConfigButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            updateLabels("", "Registering new client...", "");

            Client.register(TOKEN, CLIENT_NAME + UUID.randomUUID().toString(), HOST, new ResultHandler<Config>() {
                @Override
                public void handle(Result<Config> r) {
                    if (!r.isError()) {

                        mConfig = r.asValue();
                        Config.saveConfigSecurely(configStorageHelper(), mConfig.json(), new ConfigStorageHelper.SaveConfigHandler() {
                            @Override
                            public void saveConfigDidSucceed() {
                                mState = State.CONFIG_LOADED;

                                updateLabels("", "New Config Created and Saved", mConfig.json());
                                updateInterface();
                            }

                            @Override
                            public void saveConfigDidCancel() {
                                updateLabels("", "Config Create Canceled", "");

                            }

                            @Override
                            public void saveConfigDidFail(Throwable e) {
                                e.printStackTrace();

                                mState = State.ERROR_FOUND;

                                updateLabels(e.getLocalizedMessage(), "Create Config Failed", "");
                                updateInterface();
                            }
                        });

                    } else {
                        mState = State.ERROR_FOUND;

                        updateLabels((r.asError().error() == null ? r.asError().toString() : r.asError().error().getMessage()),"Create Config Failed", "");
                        updateInterface();

                    }
                }
            });
        }
    };

    protected void clearLabels() {
        mErrorTextView.setText("");
        mStatusTextView.setText("");
        mConfigTextView.setText("");
    }

    protected void updateLabels(String errorText, String statusText, String configText) {
        mErrorTextView.setText(errorText);
        mStatusTextView.setText(statusText);
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
}
