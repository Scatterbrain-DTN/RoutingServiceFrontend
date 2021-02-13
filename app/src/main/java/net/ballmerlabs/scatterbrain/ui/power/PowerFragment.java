    package net.ballmerlabs.scatterbrain.ui.power;

import android.content.ComponentName;
import android.content.Context;
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
import android.widget.Button;
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
    private ToggleButton enableDisableRouterButton;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = ScatterbrainAPI.Stub.asInterface(service);
            Log.v(TAG, "connected to ScatterRoutingService binder");
            try {
                binder.startDiscovery();
                updateCheckedStatus();
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException: " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            updateCheckedStatus();
            binder = null;
        }
    };

    private synchronized void updateCheckedStatus() {
        if (enableDisableRouterButton == null) {
            return;
        }

        try {
            if (binder != null) {
                enableDisableRouterButton.setChecked(binder.isDiscovering() || binder.isPassive());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: " + e);
            enableDisableRouterButton.setChecked(false);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_power, container, false);
        final TextView textView = root.findViewById(R.id.text_home);

        enableDisableRouterButton = root.findViewById(R.id.toggleButton);

        final Intent bindIntent = new Intent(getActivity().getApplicationContext(), ScatterRoutingService.class);
        getActivity().bindService(bindIntent, mServiceConnection, 0);
        updateCheckedStatus();
        enableDisableRouterButton.setOnCheckedChangeListener((compoundButton, b) -> {
            final Intent startIntent = new Intent(getActivity().getApplicationContext(), ScatterRoutingService.class);
            if (b) {
                getActivity().startForegroundService(startIntent);
            } else {
                getActivity().unbindService(mServiceConnection);
                getActivity().stopService(startIntent);
            }
        });
        return root;
    }
}