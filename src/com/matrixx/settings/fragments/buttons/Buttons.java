/*
 * Copyright (C) 2022-2026 Project Matrixx
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

package com.matrixx.settings.fragments.buttons;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import lineageos.hardware.LineageHardwareManager;

@SearchIndexable
public class Buttons extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "Buttons";

    private static final String HWKEYS_DISABLED = "hardware_keys_disable";

    private SwitchPreferenceCompat mHardwareKeysDisable;
    private LineageHardwareManager mHardware;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHardware = LineageHardwareManager.getInstance(getActivity());

        addPreferencesFromResource(R.xml.matrixx_settings_button);

        final ContentResolver resolver = getActivity().getContentResolver();

        mHardwareKeysDisable = findPreference(HWKEYS_DISABLED);

        if (mHardwareKeysDisable != null) {
            if (isKeyDisablerSupported(getActivity())) {
                int enabled = Settings.System.getIntForUser(
                        resolver,
                        Settings.System.HARDWARE_KEYS_DISABLE,
                        0,
                        UserHandle.USER_CURRENT
                );

                mHardwareKeysDisable.setChecked(enabled == 1);
                mHardwareKeysDisable.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mHardwareKeysDisable);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHardwareKeysDisable) {
            boolean value = (Boolean) newValue;

            Settings.System.putIntForUser(
                    getContentResolver(),
                    Settings.System.HARDWARE_KEYS_DISABLE,
                    value ? 1 : 0,
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
                Settings.System.HARDWARE_KEYS_DISABLE,
                0,
                UserHandle.USER_CURRENT
        );
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final LineageHardwareManager hardware =
                LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }

    /**
     * Search indexing
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.matrixx_settings_button) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    if (!isKeyDisablerSupported(context)) {
                        keys.add(HWKEYS_DISABLED);
                    }

                    return keys;
                }
            };
}
