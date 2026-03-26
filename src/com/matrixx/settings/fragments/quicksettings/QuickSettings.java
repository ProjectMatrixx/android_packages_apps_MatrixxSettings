/*
 * Copyright (C) 2022-2026 Project Matrixx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.matrixx.settings.fragments.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.matrixx.settings.utils.SystemUtils;

import java.util.List;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "QuickSettings";

    private static final String KEY_QS_COMPACT_PLAYER = "qs_compact_media_player_mode";
    private static final String KEY_SINGLE_QS_TONE = "single_qs_tone_enabled";
    private static final String KEY_QS_TILE_ALTERNATE_COLOR = "qs_tile_alternate_color";

    private Preference mQsCompactPlayer;
    private SwitchPreferenceCompat mSingleQsTone;
    private SwitchPreferenceCompat mQsTileAlternateColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.matrixx_settings_quicksettings);

        mQsCompactPlayer = (Preference) findPreference(KEY_QS_COMPACT_PLAYER);
        mQsCompactPlayer.setOnPreferenceChangeListener(this);

        mSingleQsTone = findPreference(KEY_SINGLE_QS_TONE);
        if (mSingleQsTone != null) {
            mSingleQsTone.setOnPreferenceChangeListener(this);
        }

        mQsTileAlternateColor = findPreference(KEY_QS_TILE_ALTERNATE_COLOR);
        if (mQsTileAlternateColor != null) {
            mQsTileAlternateColor.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQsCompactPlayer) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mSingleQsTone) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mQsTileAlternateColor) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        }
        return false;
    }

    public static void reset(Context context) {
        ContentResolver resolver = context.getContentResolver();

    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.matrixx_settings_quicksettings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return super.getNonIndexableKeys(context);
                }
            };
}
