package net.ballmerlabs.scatterbrain.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

import net.ballmerlabs.scatterbrain.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        for (String key : new String[]{"identity_share_cap", "message_share_cap"}) {
            androidx.preference.EditTextPreference editTextPreference = getPreferenceManager().findPreference(key);
            if (editTextPreference != null) {
                editTextPreference.setOnBindEditTextListener(editText ->
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
            }
        }
    }
}