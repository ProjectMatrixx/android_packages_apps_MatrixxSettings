/*
 * Copyright (C) 2016-2025 crDroid Android Project
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
package com.crdroid.settings.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.crdroid.settings.preferences.SystemSettingListPreference;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.crdroid.settings.fragments.ui.DozeSettings;
import com.crdroid.settings.fragments.ui.EdgeLightSettings;
import com.crdroid.settings.fragments.ui.SmartPixels;
import com.crdroid.settings.fragments.ui.MonetSettings;

import com.android.internal.util.crdroid.ThemeUtils;

import com.android.internal.util.crdroid.SystemRestartUtils;
import com.crdroid.settings.utils.ExternalFontInstaller;

import java.util.List;

@SearchIndexable
public class UserInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String KEY_FORCE_FULL_SCREEN = "display_cutout_force_fullscreen_settings";
    private static final String SMART_PIXELS = "smart_pixels";
        public static final String SETTINGS_DASHBOARD_STYLE = "settings_dashboard_style";
    private static final String KEY_FONT_MODE = "font_mode";
    private static final String KEY_PREBUILT_FONTS = "android.theme.customization.fonts";
    private static final String KEY_CUSTOM_FONT_PICKER = "custom_font_picker";
    private static final String KEY_CUSTOM_FONT_INFO = "custom_font_info";
    private static final String KEY_RESET_CUSTOM_FONT = "reset_custom_font";
    private static final String KEY_REBOOT_FOR_FONT = "reboot_for_font";
    private static final int REQUEST_PICK_FONT = 1001;


    private Preference mShowCutoutForce;
    private Preference mSmartPixels;
    private SystemSettingListPreference mSettingsDashBoardStyle;

    private SystemSettingListPreference mFontModePref;
    private Preference mPrebuiltFontsPref;
    private Preference mCustomFontPickerPref;
    private Preference mCustomFontInfoPref;
    private Preference mResetCustomFontPref;
    private Preference mRebootForFontPref;
    private ThemeUtils mThemeUtils;
    private ExternalFontInstaller mFontInstaller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.crdroid_settings_ui);

        mThemeUtils = ThemeUtils.getInstance(getActivity());
        mFontInstaller = new ExternalFontInstaller(getActivity());

        Context mContext = getActivity().getApplicationContext();
        final PreferenceScreen prefScreen = getPreferenceScreen();

	    final String displayCutout =
            mContext.getResources().getString(com.android.internal.R.string.config_mainBuiltInDisplayCutout);

        if (TextUtils.isEmpty(displayCutout)) {
            mShowCutoutForce = (Preference) findPreference(KEY_FORCE_FULL_SCREEN);
            prefScreen.removePreference(mShowCutoutForce);
        }

        mSmartPixels = (Preference) prefScreen.findPreference(SMART_PIXELS);
        boolean mSmartPixelsSupported = getResources().getBoolean(
                com.android.internal.R.bool.config_supportSmartPixels);
        if (!mSmartPixelsSupported)
            prefScreen.removePreference(mSmartPixels);

        mSettingsDashBoardStyle = (SystemSettingListPreference) findPreference(SETTINGS_DASHBOARD_STYLE);
        mSettingsDashBoardStyle.setOnPreferenceChangeListener(this);
        
        mFontModePref = findPreference(KEY_FONT_MODE);
        mFontModePref.setOnPreferenceChangeListener(this);
        mPrebuiltFontsPref = findPreference(KEY_PREBUILT_FONTS);
        mCustomFontPickerPref = findPreference(KEY_CUSTOM_FONT_PICKER);
        mCustomFontPickerPref.setOnPreferenceClickListener(preference -> {
            pickCustomFont();
            return true;
        });
        mCustomFontInfoPref = findPreference(KEY_CUSTOM_FONT_INFO);
        mResetCustomFontPref = findPreference(KEY_RESET_CUSTOM_FONT);
        mResetCustomFontPref.setOnPreferenceClickListener(preference -> {
            resetCustomFont();
            return true;
        });
        mRebootForFontPref = findPreference(KEY_REBOOT_FOR_FONT);
        mRebootForFontPref.setOnPreferenceClickListener(preference -> {
            showRebootDialog();
            return true;
        });
        updateFontPreferences();
    }

    private void updateFontPreferences() {
        final String fontMode = Settings.System.getStringForUser(
                getContext().getContentResolver(),
                KEY_FONT_MODE,
                UserHandle.USER_CURRENT
        );
        final String customFontName = Settings.Secure.getStringForUser(
                getContext().getContentResolver(),
                "custom_font_name",
                UserHandle.USER_CURRENT

        );

        final boolean isCustomMode = "custom".equals(fontMode);
        final boolean hasCustomFont = customFontName != null && !customFontName.isEmpty();
        mPrebuiltFontsPref.setVisible(!isCustomMode);
        mCustomFontPickerPref.setVisible(isCustomMode);
        mCustomFontInfoPref.setVisible(isCustomMode && hasCustomFont);
        mResetCustomFontPref.setVisible(isCustomMode && hasCustomFont);
        mRebootForFontPref.setVisible(isCustomMode && hasCustomFont);

        if (hasCustomFont) {
            mCustomFontInfoPref.setSummary(getString(R.string.custom_font_installed_summary, customFontName));
        }
    }

    private void pickCustomFont() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_PICK_FONT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FONT && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fontUri = data.getData();
                installCustomFont(fontUri);
            }
        }
    }

    private void installCustomFont(Uri fontUri) {
        new Thread(() -> {
            String postScriptName = mFontInstaller.installFontFromUri(getContext(), fontUri);

            if (postScriptName != null) {
                Settings.Secure.putStringForUser(
                        getContext().getContentResolver(),
                        "custom_font_name",
                        postScriptName,
                        UserHandle.USER_CURRENT
                );
                getActivity().runOnUiThread(() -> {
                    updateFontPreferences();
                    Toast.makeText(getContext(), R.string.custom_font_installed_success, Toast.LENGTH_SHORT).show();
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), R.string.custom_font_install_failed, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void resetCustomFont() {
        mFontInstaller.resetFontUpdates(getContext());
        Settings.Secure.putStringForUser(
                getContext().getContentResolver(),
                "custom_font_name",
                "",
                UserHandle.USER_CURRENT
        );
        if (mThemeUtils != null) {
            mThemeUtils.setOverlayEnabled(
                    "android.theme.customization.font",
                    "com.android.theme.font.googlesansflex",
                    "android"
            );
        }
        updateFontPreferences();
        Toast.makeText(getContext(), R.string.custom_font_reset_success, Toast.LENGTH_SHORT).show();
    }

    private void showRebootDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.reboot_required_title)
                .setMessage(R.string.reboot_required_message)
                .setPositiveButton(R.string.reboot_device, (dialog, which) -> {
                    ExternalFontInstaller.rebootDevice(getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        final String key = preference.getKey();
        ContentResolver resolver = getActivity().getContentResolver();
            
        if (preference == mFontModePref) {
            String fontMode = (String) newValue;
            Settings.System.putStringForUser(
                    resolver,
                    KEY_FONT_MODE,
                    fontMode,
                    UserHandle.USER_CURRENT
            );
            if ("prebuilt".equals(fontMode)) {
                String customFontName = Settings.Secure.getStringForUser(
                        resolver,
                        "custom_font_name",
                        UserHandle.USER_CURRENT
                );
                if (customFontName != null && !customFontName.isEmpty()) {
                    resetCustomFont();
                }
            }
            updateFontPreferences();
            return true;
        }
	    if (preference == mSettingsDashBoardStyle){
            SystemRestartUtils.showSettingsRestartDialog(getContext());
            return true;
            }

        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.CHARGING_ANIMATION, 1, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PULSE_ON_NEW_TRACKS, 0, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.DOZE_ALWAYS_ON_WALLPAPER_ENABLED, mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeSupportsAodWallpaper) ? 1 : 0,
                UserHandle.USER_CURRENT);

        DozeSettings.reset(mContext);
        EdgeLightSettings.reset(mContext);
        MonetSettings.reset(mContext);
        SmartPixels.reset(mContext);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CRDROID_SETTINGS;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.crdroid_settings_ui) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

	                final String displayCutout =
                        context.getResources().getString(com.android.internal.R.string.config_mainBuiltInDisplayCutout);

                    if (TextUtils.isEmpty(displayCutout)) {
                        keys.add(KEY_FORCE_FULL_SCREEN);
                    }

                    boolean mSmartPixelsSupported = context.getResources().getBoolean(
                            com.android.internal.R.bool.config_supportSmartPixels);
                    if (!mSmartPixelsSupported)
                        keys.add(SMART_PIXELS);

                    return keys;
                }
            };
}
