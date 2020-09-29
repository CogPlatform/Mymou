package mymou.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.*;

import mymou.R;
import mymou.Utils.SoundManager;
import mymou.task.backend.TaskManager;

/**
 * Preference Fragment for user to select secondary reinforcer sound
 * Currently hard-coded to give them all possible android options (98 in total)
 */


public class PrefsFragSoundPicker extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String TAG = "MymouColourPicker";
    private Context mContext;
    private SoundManager soundManager;
    private static int num_sounds = 98;
    private CheckBoxPreference[] checkBoxPreferences = new CheckBoxPreference[num_sounds];


    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_empty, rootKey);

        // Load parameters
        mContext = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Do you want to define your own tone (Frequency and duration) or use a predefined tone?")
                .setTitle("Choose tone type")
                .setPositiveButton("Custom", new DialogInterface.OnClickListener() {

                    // If click 'custom' tone
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                        // Duration dialog
                        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                        alert.setTitle("Input duration (s)");
                        final EditText input2 = new EditText(getContext());
                        input2.setInputType(InputType.TYPE_CLASS_NUMBER);
                        input2.setRawInputType(Configuration.KEYBOARD_12KEY);
                        alert.setView(input2);
                        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    int dur = Integer.valueOf(input2.getText().toString());
                                    if (dur > 10) {
                                        Toast.makeText(getActivity().getApplicationContext(), "Error: Cannot have tones greater in length than 10 seconds!", Toast.LENGTH_LONG).show();
                                        getActivity().onBackPressed();
                                        return;
                                    }

                                    // Frequency dialog
                                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                                    alert.setTitle("Input frequency");
                                    final EditText input = new EditText(getContext());
                                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                                    input.setRawInputType(Configuration.KEYBOARD_12KEY);
                                    alert.setView(input);
                                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            try {

                                                int freq = Integer.valueOf(input.getText().toString());

                                                // Store settings
                                                sharedPrefs.edit().putInt(getString(R.string.preftag_tone_freq), freq).commit();
                                                sharedPrefs.edit().putBoolean(getString(R.string.preftag_custom_tone), true).commit();
                                                sharedPrefs.edit().putInt(getString(R.string.preftag_tone_dur), dur).commit();

                                                // Now play tone to the user
                                                soundManager = new SoundManager(new PreferencesManager(mContext));
                                                soundManager.playTone();
                                                Toast.makeText(getActivity().getApplicationContext(), "Custom tone saved", Toast.LENGTH_LONG).show();
                                                getActivity().onBackPressed();

                                            } catch (NumberFormatException e) {
                                                Toast.makeText(getActivity().getApplicationContext(), "Invalid number", Toast.LENGTH_LONG).show();
                                            }

                                        }
                                    });
                                    alert.show();
                                } catch (NumberFormatException e) {
                                    Toast.makeText(getActivity().getApplicationContext(), "Invalid number", Toast.LENGTH_LONG).show();
                                }

                            }
                        });
                        alert.show();
                    }
                })

                .setNegativeButton("Predefined", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing and Continue to sound picker
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        soundManager = new SoundManager(new PreferencesManager(mContext));

        // Add preferences
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(mContext);
        setPreferenceScreen(preferenceScreen);
        TypedValue themeTypedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.preferenceTheme, themeTypedValue, true);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(mContext, themeTypedValue.resourceId);
        PreferenceCategory preferenceCategory = new PreferenceCategory(contextThemeWrapper);
        preferenceCategory.setTitle("Choose sound used for secondary reinforcement");
        getPreferenceScreen().addPreference(preferenceCategory);

        // Now iterate through sounds and add them as checkboxes
        for (int i = 0; i < num_sounds; i++) {
            checkBoxPreferences[i] = new CheckBoxPreference(contextThemeWrapper);
            checkBoxPreferences[i].setTitle("Sound " + i);
            checkBoxPreferences[i].setKey(String.valueOf(i));
            checkBoxPreferences[i].setChecked(false);
            preferenceCategory.addPreference(checkBoxPreferences[i]);

        }

        // Set onchange listener
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, key + " preference changed");
        try {
            Integer.valueOf(key);
        } catch (NumberFormatException e) {
            return;
        }

        // Store tone
        sharedPreferences.edit().putInt(getString(R.string.preftag_sound_to_play), Integer.valueOf(key)).commit();
        sharedPreferences.edit().putBoolean(getString(R.string.preftag_custom_tone), false).commit();

        // Play tone for the user
        new SoundManager(new PreferencesManager(mContext)).playTone();

        // Switch off other sounds so they can only ever pick one
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);  // Disable listener to stop recursion
        for (int i = 0; i < num_sounds; i++) {
            if (i != Integer.valueOf(key)) {
                checkBoxPreferences[i].setChecked(false);
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);  // Re-enable listener
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        Toast.makeText(getActivity().getApplicationContext(), "Predefined tone saved", Toast.LENGTH_LONG).show();

    }

}



