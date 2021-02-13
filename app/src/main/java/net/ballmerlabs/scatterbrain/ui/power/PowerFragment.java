    package net.ballmerlabs.scatterbrain.ui.power;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.ballmerlabs.scatterbrain.R;
import net.ballmerlabs.uscatterbrain.ScatterRoutingService;

    public class PowerFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_power, container, false);
        final TextView textView = root.findViewById(R.id.text_home);

        ToggleButton enableDisableRouterButton = root.findViewById(R.id.toggleButton);
        enableDisableRouterButton.setChecked(false);
        enableDisableRouterButton.setOnCheckedChangeListener((compoundButton, b) -> {
            Intent startIntent = new Intent(getActivity().getApplicationContext(), ScatterRoutingService.class);
            if (b) {
                getActivity().startService(startIntent);
            } else {
                getActivity().stopService(startIntent);
            }
        });
        return root;
    }
}