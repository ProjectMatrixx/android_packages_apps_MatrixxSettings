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
package com.matrixx.settings.fragments.notifications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class Notifications extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "Notifications";

    private static final String BATTERY_LIGHTS_PREF = "battery_lights";
    private static final String NOTIFICATION_LIGHTS_PREF = "notification_lights";
    private static final String LIGHT_BRIGHTNESS_CATEGORY = "light_brightness";

    private Preference mBatteryLights;
    private Preference mNotificationLights;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.matrixx_settings_notifications);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Context context = getActivity();
        final Resources res = context.getResources();

        // Battery Lights
        mBatteryLights = prefScreen.findPreference(BATTERY_LIGHTS_PREF);
        boolean batteryLightsSupported = res.getInteger(
                org.lineageos.platform.internal.R.integer.config_deviceLightCapabilities) >= 64;

        if (!batteryLightsSupported && mBatteryLights != null) {
            prefScreen.removePreference(mBatteryLights);
        }

        // Notification Lights
        mNotificationLights = prefScreen.findPreference(NOTIFICATION_LIGHTS_PREF);
        boolean notificationLightsSupported = res.getBoolean(
                com.android.internal.R.bool.config_intrusiveNotificationLed);

        if (!notificationLightsSupported && mNotificationLights != null) {
            prefScreen.removePreference(mNotificationLights);
        }

        // Remove category if both not supported
        if (!batteryLightsSupported && !notificationLightsSupported) {
            PreferenceCategory lightsCategory =
                    prefScreen.findPreference(LIGHT_BRIGHTNESS_CATEGORY);
            if (lightsCategory != null) {
                prefScreen.removePreference(lightsCategory);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    public static void reset(Context context) {
        ContentResolver resolver = context.getContentResolver();
        // No reset needed for these prefs (UI only)
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.matrixx_settings_notifications) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    final Resources res = context.getResources();

                    boolean batteryLightsSupported = res.getInteger(
                            org.lineageos.platform.internal.R.integer.config_deviceLightCapabilities) >= 64;
                    if (!batteryLightsSupported) {
                        keys.add(BATTERY_LIGHTS_PREF);
                    }

                    boolean notificationLightsSupported = res.getBoolean(
                            com.android.internal.R.bool.config_intrusiveNotificationLed);
                    if (!notificationLightsSupported) {
                        keys.add(NOTIFICATION_LIGHTS_PREF);
                    }

                    return keys;
                }
            };
}
