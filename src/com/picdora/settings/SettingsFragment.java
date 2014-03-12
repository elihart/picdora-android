package com.picdora.settings;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

import com.picdora.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.settings);
    }

}
