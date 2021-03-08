package net.ballmerlabs.scatterbrain.ui

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import net.ballmerlabs.scatterbrain.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        for (key in arrayOf("identity_share_cap", "message_share_cap")) {
            val editTextPreference = preferenceManager.findPreference<EditTextPreference>(key)
            editTextPreference?.setOnBindEditTextListener { editText: EditText -> editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }
        }
    }
}