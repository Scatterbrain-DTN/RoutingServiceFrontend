package net.ballmerlabs.scatterbrain.ui;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import net.ballmerlabs.scatterbrain.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}