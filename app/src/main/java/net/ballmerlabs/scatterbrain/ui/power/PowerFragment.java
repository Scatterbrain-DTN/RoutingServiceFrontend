    package net.ballmerlabs.scatterbrain.ui.power;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.ballmerlabs.scatterbrain.R;
import net.ballmerlabs.uscatterbrain.API.ScatterMessage;
import net.ballmerlabs.uscatterbrain.ScatterRoutingService;
import net.ballmerlabs.uscatterbrain.ScatterbrainAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class PowerFragment extends Fragment {
    private static final String TAG = "PowerFragment";
    private ScatterbrainAPI binder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = ScatterbrainAPI.Stub.asInterface(service);
            Log.v(TAG, "connected to ScatterRoutingService binder");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
        }
    };

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