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

package com.matrixx.settings.fragments.lockscreen;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.matrixx.OmniJawsClient;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.matrixx.settings.utils.DeviceUtils;
import com.matrixx.settings.utils.SystemUtils;
import com.matrixx.settings.utils.TelephonyUtils;

import java.util.List;

@SearchIndexable
public class LockScreen extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "LockScreen";

    private static final String LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category";
    private static final String LOCKSCREEN_INTERFACE_CATEGORY = "lockscreen_interface_category";
    private static final String KEY_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String KEY_SMARTSPACE = "lockscreen_smartspace_enabled";
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String KEY_KG_USER_SWITCHER = "kg_user_switcher_enabled";
    private static final String LUNARIS_UDFPS_CUSTOM_CATEGORY = "lockscreen_custom_category";

    private static final String KEY_FP_SUCCESS = "fp_success_vibrate";
    private static final String KEY_FP_ERROR = "fp_error_vibrate";

    private static final String KEY_CARRIER_NAME = "lockscreen_show_carrier";

    private static final String PROP_CUSTOM_UDFPS = "persist.sys.udfps.custom";

    private Preference mRippleEffect;

    private SwitchPreferenceCompat mSmartspace;
    private SwitchPreferenceCompat mWeather;
    private SwitchPreferenceCompat mKgUserSwitcher;
    private SwitchPreferenceCompat mFpSuccessVib;
    private SwitchPreferenceCompat mFpErrorVib;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.matrixx_settings_lockscreen);
        final Context context = getContext();

        PreferenceCategory gestCategory = (PreferenceCategory) findPreference(LOCKSCREEN_GESTURES_CATEGORY);
        PreferenceCategory lunarisUdfpsCategory = (PreferenceCategory) findPreference(LUNARIS_UDFPS_CUSTOM_CATEGORY);

        mFpSuccessVib = findPreference(KEY_FP_SUCCESS);
        mFpErrorVib = findPreference(KEY_FP_ERROR);
        mRippleEffect = findPreference(KEY_RIPPLE_EFFECT);

        boolean hasFingerprint = DeviceUtils.hasFingerprint(context);
        if (!hasFingerprint) {
            gestCategory.removePreference(mRippleEffect);
        }

        if (lunarisUdfpsCategory != null && !SystemProperties.getBoolean(PROP_CUSTOM_UDFPS, false)) {
            getPreferenceScreen().removePreference(lunarisUdfpsCategory);
        }

        boolean hapticAvailable = DeviceUtils.hasVibrator(context);
        if (!hasFingerprint || !hapticAvailable) {
            gestCategory.removePreference(mFpSuccessVib);
            gestCategory.removePreference(mFpErrorVib);
        }

        if (!TelephonyUtils.isVoiceCapable(context)) {
            PreferenceCategory intCategory = (PreferenceCategory) findPreference(LOCKSCREEN_INTERFACE_CATEGORY);
            SwitchPreferenceCompat carrierName = findPreference(KEY_CARRIER_NAME);
            intCategory.removePreference(carrierName);
        }

        mSmartspace = (SwitchPreferenceCompat) findPreference(KEY_SMARTSPACE);
        mSmartspace.setOnPreferenceChangeListener(this);

        mWeather = (SwitchPreferenceCompat) findPreference(KEY_WEATHER);
        mWeather.setOnPreferenceChangeListener(this);

        mKgUserSwitcher = (SwitchPreferenceCompat) findPreference(KEY_KG_USER_SWITCHER);
        mKgUserSwitcher.setOnPreferenceChangeListener(this);

        updateWeatherSettings();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSmartspace) {
            mSmartspace.setChecked((Boolean)newValue);
            updateWeatherSettings();
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mWeather) {
            mWeather.setChecked((Boolean)newValue);
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mKgUserSwitcher) {
            mKgUserSwitcher.setChecked((Boolean)newValue);
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        }
        return false;
    }

    private void updateWeatherSettings() {
        if (mWeather == null || mSmartspace == null) return;

        boolean weatherEnabled = OmniJawsClient.get().isOmniJawsEnabled(getContext());
        mWeather.setEnabled(!mSmartspace.isChecked() && weatherEnabled);
        mWeather.setSummary(!mSmartspace.isChecked() && weatherEnabled
                ? R.string.lockscreen_weather_summary
                : R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWeatherSettings();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.matrixx_settings_lockscreen) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    boolean hasFingerprint = DeviceUtils.hasFingerprint(context);
                    if (!hasFingerprint) {
                        keys.add(KEY_RIPPLE_EFFECT);
                    }
                    if (!SystemProperties.getBoolean(PROP_CUSTOM_UDFPS, false)) {
                        keys.add(LUNARIS_UDFPS_CUSTOM_CATEGORY);
                    }
                    boolean hapticAvailable = DeviceUtils.hasVibrator(context);
                    if (!hasFingerprint || !hapticAvailable) {
                        keys.add(KEY_FP_SUCCESS);
                        keys.add(KEY_FP_ERROR);
                    }
                    if (!TelephonyUtils.isVoiceCapable(context)) {
                        keys.add(KEY_CARRIER_NAME);
                    }
                    return keys;
                }
            };
}
