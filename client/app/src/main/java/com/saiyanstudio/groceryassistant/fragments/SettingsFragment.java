package com.saiyanstudio.groceryassistant.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.saiyanstudio.groceryassistant.R;

import java.util.prefs.Preferences;

/**
 * Created by MAHE on 3/10/2016.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        EditTextPreference ipEditTextPref = (EditTextPreference) findPreference("ip_address");
        ipEditTextPref.setSummary(ipEditTextPref.getText());

        EditTextPreference editTextPref = (EditTextPreference) findPreference("port");
        if(editTextPref.toString().equalsIgnoreCase(""))
            editTextPref.setSummary("8080");
        else
            editTextPref.setSummary(editTextPref.getText());

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);

        if (pref instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            if (pref.getTitle().toString().toLowerCase().contains("ip_address")) {
                pref.setSummary(editTextPref.getText());
            } else {
                pref.setSummary(editTextPref.getText());
            }
        }
    }
}
