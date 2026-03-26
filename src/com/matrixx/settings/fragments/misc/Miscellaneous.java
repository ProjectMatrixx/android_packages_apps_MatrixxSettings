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
package com.matrixx.settings.fragments.misc;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class Miscellaneous extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "Miscellaneous";

    private static final String KEY_THREE_FINGERS_SWIPE = "three_fingers_swipe";
    private static final String POCKET_JUDGE = "pocket_judge";

    // System setting key
    private static final String THREE_FINGERS_SWIPE_ACTION = "three_fingers_swipe_action";

    private ListPreference mThreeFingersSwipe;
    private Preference mPocketJudge;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.matrixx_settings_misc);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        mPocketJudge = (Preference) prefScreen.findPreference(POCKET_JUDGE);
        boolean mPocketJudgeSupported = res.getBoolean(
                com.android.internal.R.bool.config_pocketModeSupported);
        if (!mPocketJudgeSupported)
            prefScreen.removePreference(mPocketJudge);

        // Init Three Finger Swipe
        mThreeFingersSwipe = (ListPreference) prefScreen.findPreference(KEY_THREE_FINGERS_SWIPE);

        if (mThreeFingersSwipe != null) {
            int value = Settings.System.getIntForUser(
                    getContentResolver(),
                    THREE_FINGERS_SWIPE_ACTION,
                    0,
                    UserHandle.USER_CURRENT
            );

            mThreeFingersSwipe.setValue(String.valueOf(value));
            mThreeFingersSwipe.setSummary(mThreeFingersSwipe.getEntry());
            mThreeFingersSwipe.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mThreeFingersSwipe) {
            String value = (String) newValue;
            int intValue = Integer.parseInt(value);

            int index = mThreeFingersSwipe.findIndexOfValue(value);
            mThreeFingersSwipe.setSummary(mThreeFingersSwipe.getEntries()[index]);

            Settings.System.putIntForUser(
                    getContentResolver(),
                    THREE_FINGERS_SWIPE_ACTION,
                    intValue,
                    UserHandle.USER_CURRENT
            );
            return true;
        }
        return false;
    }

    public static void reset(Context context) {
        ContentResolver resolver = context.getContentResolver();

        Settings.System.putIntForUser(
                resolver,
                THREE_FINGERS_SWIPE_ACTION,
                0,
                UserHandle.USER_CURRENT
        );
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.matrixx_settings_misc) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    boolean mPocketJudgeSupported = res.getBoolean(
                            com.android.internal.R.bool.config_pocketModeSupported);
                    if (!mPocketJudgeSupported)
                        keys.add(POCKET_JUDGE);
                    return super.getNonIndexableKeys(context);
                }
            };
}
