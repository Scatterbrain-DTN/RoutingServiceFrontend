package net.ballmerlabs.scatterroutingservice.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.preference.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.R
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment @Inject constructor() : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        for (key in arrayOf("identity_share_cap", "message_share_cap")) {
            val editTextPreference = preferenceManager.findPreference<EditTextPreference>(key)
            editTextPreference?.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_enable_crashlytics)) {
            val b = pref.getBoolean(key, false)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(b)
        }
    }


}