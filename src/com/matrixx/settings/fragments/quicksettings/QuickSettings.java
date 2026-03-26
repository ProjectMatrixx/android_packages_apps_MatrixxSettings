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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.matrixx.settings.fragments.quicksettings.LayoutSettings;
import com.matrixx.settings.fragments.quicksettings.QsHeaderImageSettings;
import com.matrixx.settings.utils.DeviceUtils;
import com.matrixx.settings.utils.SystemUtils;

import lineageos.providers.LineageSettings;

import com.android.internal.util.matrixx.VibrationUtils;

import java.util.List;
import java.util.ArrayList;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "QuickSettings";

    private static final String KEY_QS_COMPACT_PLAYER = "qs_compact_media_player_mode";
    private static final String QS_BRIGHTNESS_CATEGORY = "qs_brightness_slider_category";
    private static final String QS_LAYOUT_CATEGORY = "qs_layout_category";
    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String KEY_BRIGHTNESS_SLIDER_HAPTIC = "qs_brightness_slider_haptic";
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_SINGLE_QS_TONE = "single_qs_tone_enabled";
    private static final String KEY_DUAL_TARGET_TILE_STYLE = "dual_target_tile_style";
    private static final String KEY_QS_TILE_ALTERNATE_COLOR = "qs_tile_alternate_color";

    private Preference mQsCompactPlayer;
    private ListPreference mShowBrightnessSlider;
    private ListPreference mBrightnessSliderPosition;
    private SwitchPreferenceCompat mBrightnessSliderHaptic;
    private SwitchPreferenceCompat mShowAutoBrightness;
    private SwitchPreferenceCompat mSingleQsTone;
    private Preference mDualTargetTileStyle;
    private SwitchPreferenceCompat mQsTileAlternateColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.matrixx_settings_quicksettings);

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();

        PreferenceCategory brightnessCategory = (PreferenceCategory) findPreference(QS_BRIGHTNESS_CATEGORY);
        PreferenceCategory tileCategory = (PreferenceCategory) findPreference(QS_LAYOUT_CATEGORY);

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        mShowBrightnessSlider.setOnPreferenceChangeListener(this);
        boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

        mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
        mBrightnessSliderPosition.setEnabled(showSlider);

        mSingleQsTone = findPreference(KEY_SINGLE_QS_TONE);
        if (mSingleQsTone != null) {
            mSingleQsTone.setOnPreferenceChangeListener(this);
        }

        mDualTargetTileStyle = findPreference(KEY_DUAL_TARGET_TILE_STYLE);
        if (mDualTargetTileStyle != null) {
            mDualTargetTileStyle.setOnPreferenceChangeListener(this);
        }

        mQsTileAlternateColor = findPreference(KEY_QS_TILE_ALTERNATE_COLOR);
        if (mQsTileAlternateColor != null) {
            mQsTileAlternateColor.setOnPreferenceChangeListener(this);
        }

        mBrightnessSliderHaptic = findPreference(KEY_BRIGHTNESS_SLIDER_HAPTIC);
        boolean hapticAvailable = DeviceUtils.hasVibrator(context);

        if (hapticAvailable) {
            mBrightnessSliderHaptic.setEnabled(showSlider);
        } else {
            brightnessCategory.removePreference(mBrightnessSliderHaptic);
        }

        mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
        boolean automaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);

        if (automaticAvailable) {
            mShowAutoBrightness.setEnabled(showSlider);
        } else {
            brightnessCategory.removePreference(mShowAutoBrightness);
        }

        mQsCompactPlayer = (Preference) findPreference(KEY_QS_COMPACT_PLAYER);
        mQsCompactPlayer.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();

        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            mBrightnessSliderPosition.setEnabled(value > 0);
            if (mBrightnessSliderHaptic != null)
                mBrightnessSliderHaptic.setEnabled(value > 0);
            if (mShowAutoBrightness != null)
                mShowAutoBrightness.setEnabled(value > 0);
            return true;
        } else if (preference == mSingleQsTone) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mDualTargetTileStyle) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mQsTileAlternateColor) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mQsCompactPlayer) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        }
        return false;
    }

    public static void reset(Context context) {
        ContentResolver resolver = context.getContentResolver();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != null && preference.getKey() != null) {
            VibrationUtils.triggerVibration(getContext(), 3);
        }
        return super.onPreferenceTreeClick(preference);
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
                    List<String> keys = super.getNonIndexableKeys(context);
                    final Resources res = context.getResources();
                    final ContentResolver resolver = context.getContentResolver();

                    boolean automaticAvailable = res.getBoolean(
                            com.android.internal.R.bool.config_automatic_brightness_available);
                    if (!automaticAvailable) {
                        keys.add(KEY_SHOW_AUTO_BRIGHTNESS);
                    }

                    boolean hapticAvailable = DeviceUtils.hasVibrator(context);
                    if (!hapticAvailable) {
                        keys.add(KEY_BRIGHTNESS_SLIDER_HAPTIC);
                    }

                    return keys;
                }
            };
}
