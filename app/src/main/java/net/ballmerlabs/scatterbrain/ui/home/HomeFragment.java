package net.ballmerlabs.scatterbrain.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import net.ballmerlabs.scatterbrain.R;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);

        ToggleButton enableDisableRouterButton = root.findViewById(R.id.toggleButton);
        enableDisableRouterButton.setChecked(false);
        enableDisableRouterButton.setOnCheckedChangeListener((compoundButton, b) -> {

        });
        return root;
    }
}