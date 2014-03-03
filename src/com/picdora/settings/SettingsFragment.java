package com.picdora.settings;

import com.picdora.R;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings);
    }

}
