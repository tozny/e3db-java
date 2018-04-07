package com.tozny.e3dbtest;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.w3c.dom.Text;


/**
 * A simple {@link Fragment} subclass.
 */
public class BaseFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Integer mIndex;
    private String mName;

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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_base, container, false);

        TextView nameLabel = view.findViewById(R.id.name_label);
        nameLabel.setText(mName);

        return view;
    }
}
