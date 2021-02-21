package net.ballmerlabs.scatterbrain.ui.identity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.ballmerlabs.scatterbrain.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link IdentityFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class IdentityFragment extends Fragment {

    public IdentityFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment IdentityFragment.
     */
    public static IdentityFragment newInstance() {
        IdentityFragment fragment = new IdentityFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_identity, container, false);
    }
}