/*
 * Copyright (C) 2023 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crdroid.settings.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.util.crdroid.SystemRestartUtils;

@SearchIndexable
public class QuickSwitch extends SettingsPreferenceFragment 
    implements Preference.OnPreferenceChangeListener, Indexable {
    
    private static final String TAG = "QuickSwitch";

    private static final String QUICKSWITCH_KEY = "persist.sys.default_launcher";
    
    private ListPreference quickSwitchPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_switch);

        int defaultLauncher = SystemProperties.getInt(QUICKSWITCH_KEY, 0);
        quickSwitchPref = findPreference(QUICKSWITCH_KEY);
        quickSwitchPref.setOnPreferenceChangeListener(this);
        Context context = getContext();
        Resources res = context.getResources();

        String[] launcherEntries = res.getStringArray(R.array.quickswitch_launcher_entries);
        String[] launcherValues = res.getStringArray(R.array.quickswitch_launcher_values);

        List<String> quickSwitchEntries = new ArrayList<>();
        List<String> quickSwitchValues = new ArrayList<>();

        quickSwitchEntries.add(launcherEntries[0]);
        quickSwitchValues.add(launcherValues[0]);

        if (SystemProperties.getInt("persist.sys.quickswitch_pixel_shipped", 0) != 0) {
            quickSwitchEntries.add(launcherEntries[1]);
            quickSwitchValues.add(launcherValues[1]);
        }

        if (SystemProperties.getInt("persist.sys.quickswitch_lawnchair_shipped", 0) != 0) {
            quickSwitchEntries.add(launcherEntries[2]);
            quickSwitchValues.add(launcherValues[2]);
        }

        quickSwitchPref.setEntries(quickSwitchEntries.toArray(new CharSequence[0]));
        quickSwitchPref.setEntryValues(quickSwitchValues.toArray(new CharSequence[0]));
        quickSwitchPref.setValue(String.valueOf(defaultLauncher));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == quickSwitchPref) {
            SystemRestartUtils.showSystemRestartDialog(getContext());
            return true;
        }
        return false;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.quick_switch;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
