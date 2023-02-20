package net.ballmerlabs.scatterroutingservice.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import net.ballmerlabs.scatterroutingservice.R
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment @Inject constructor() : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        for (key in arrayOf(
                requireContext().getString(R.string.pref_identitycap),
                requireContext().getString(R.string.pref_blockdatacap),
                requireContext().getString(R.string.pref_sizecap)
        )) {
            val editTextPreference = preferenceManager.findPreference<EditTextPreference>(key)!!
            editTextPreference.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }
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
        if (key == getString(R.string.pref_optout_crashlytics)) {
            val b = pref.getBoolean(key, false)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!b)
        }
    }


}