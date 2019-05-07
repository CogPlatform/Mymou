package com.example.jbutler.mymou;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class PreferencesCompat extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public PreferencesCompat() {
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

        // Get sharedpreferences
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        // Set onchange listener
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Only show times if auto start/stop is enabled
        if(sharedPrefs.getBoolean("autostartswitch",false)) {
            Preference editTextPreference = findPreference("startuptime");
            editTextPreference.setVisible(true);
        }
        if(sharedPrefs.getBoolean("autostopswitch",false)) {
            Preference editTextPreference = findPreference("startuptime");
            editTextPreference.setVisible(true);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("autostartswitch")) {
            Preference editTextPreference = findPreference("startuptime");
            editTextPreference.setVisible(sharedPreferences.getBoolean("autostartswitch",false));
        }
        if (key.equals("autostopswitch")) {
            Preference editTextPreference = findPreference("shutdowntime");
            editTextPreference.setVisible(sharedPreferences.getBoolean("autostopswitch",false));
        }
    }


    }
