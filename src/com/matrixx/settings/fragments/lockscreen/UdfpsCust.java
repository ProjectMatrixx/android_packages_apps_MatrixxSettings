/*
 * Copyright (C) 2024-2025 Lunaris AOSP
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

package com.matrixx.settings.fragments.lockscreen;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.matrixx.settings.utils.UDFPSUtils;

import java.io.File;

public class UdfpsCust extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "UdfpsCust";

    private static final String UDFPS_ICON_TYPE = "udfps_icon_type";
    private static final String UDFPS_ICON = "udfps_icon";
    private static final String UDFPS_CUSTOM_ICON_SELECT = "udfps_custom_icon_select";

    private static final int REQUEST_PICK_UDFPS_ICON = 10003;
    private static final int ICON_TYPE_PREBUILT = 0;
    private static final int ICON_TYPE_CUSTOM = 1;

    private ListPreference mUdfpsIconType;
    private Preference mUdfpsIcon;
    private Preference mUdfpsCustomIconSelect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.matrixx_settings_udfps_custom);

        final ContentResolver resolver = getActivity().getContentResolver();
        initUdfpsIconPicker(resolver);
    }

    private void initUdfpsIconPicker(ContentResolver resolver) {
        mUdfpsIconType = findPreference(UDFPS_ICON_TYPE);
        if (mUdfpsIconType != null) {
            int iconType = Settings.System.getIntForUser(resolver,
                    Settings.System.UDFPS_ICON_TYPE, ICON_TYPE_PREBUILT,
                    UserHandle.USER_CURRENT);
            mUdfpsIconType.setValue(String.valueOf(iconType));
            mUdfpsIconType.setSummary(mUdfpsIconType.getEntry());
            mUdfpsIconType.setOnPreferenceChangeListener(this);
        }

        mUdfpsIcon = findPreference(UDFPS_ICON);
        mUdfpsCustomIconSelect = findPreference(UDFPS_CUSTOM_ICON_SELECT);

        int currentIconType = Settings.System.getIntForUser(resolver,
                Settings.System.UDFPS_ICON_TYPE, ICON_TYPE_PREBUILT,
                UserHandle.USER_CURRENT);
        updateUdfpsPreferencesForIconType(currentIconType);
    }

    private void updateUdfpsPreferencesForIconType(int iconType) {
        boolean isPrebuilt = iconType == ICON_TYPE_PREBUILT;
        boolean isCustom = iconType == ICON_TYPE_CUSTOM;

        if (mUdfpsIcon != null) {
            mUdfpsIcon.setEnabled(isPrebuilt);
        }

        if (mUdfpsCustomIconSelect != null) {
            mUdfpsCustomIconSelect.setEnabled(isCustom);
            if (isCustom) {
                String customPath = Settings.System.getStringForUser(
                        getActivity().getContentResolver(),
                        Settings.System.UDFPS_CUSTOM_FP_ICON_PATH,
                        UserHandle.USER_CURRENT);
                if (customPath != null && !customPath.isEmpty()) {
                    mUdfpsCustomIconSelect.setSummary(R.string.udfps_custom_icon_selected);
                } else {
                    mUdfpsCustomIconSelect.setSummary(R.string.udfps_custom_icon_select_summary);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == null || newValue == null) return false;

        ContentResolver resolver = getActivity().getContentResolver();
        String key = preference.getKey();

        if (UDFPS_ICON_TYPE.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.UDFPS_ICON_TYPE, value, UserHandle.USER_CURRENT);

            int index = mUdfpsIconType.findIndexOfValue((String) newValue);
            if (index >= 0) mUdfpsIconType.setSummary(mUdfpsIconType.getEntries()[index]);

            updateUdfpsPreferencesForIconType(value);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == null || preference.getKey() == null) return super.onPreferenceTreeClick(preference);

        if (UDFPS_CUSTOM_ICON_SELECT.equals(preference.getKey())) {
            return handleUdfpsCustomIconSelect();
        }

        return super.onPreferenceTreeClick(preference);
    }

    private boolean handleUdfpsCustomIconSelect() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"image/gif", "image/webp", "image/png", "image/jpeg"});
            startActivityForResult(intent, REQUEST_PICK_UDFPS_ICON);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open image picker", e);
            Toast.makeText(getActivity(),
                    R.string.udfps_custom_icon_picker_error, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode != REQUEST_PICK_UDFPS_ICON) return;

        if (resultCode != Activity.RESULT_OK || result == null || result.getData() == null) {
            Toast.makeText(getActivity(), R.string.udfps_custom_icon_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final Uri imageUri = result.getData();
        new Thread(() -> {
            String savedPath = UDFPSUtils.saveUDFPSIconToInternalStorage(getActivity(), imageUri);
            getActivity().runOnUiThread(() -> {
                if (savedPath != null) {
                    File checkFile = new File(savedPath);
                    if (!checkFile.exists() || !checkFile.canRead()) {
                        Log.w(TAG, "Saved file not accessible: " + savedPath);
                        Toast.makeText(getActivity(), "File not accessible", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Settings.System.putStringForUser(getActivity().getContentResolver(),
                            Settings.System.UDFPS_CUSTOM_FP_ICON_PATH, savedPath, UserHandle.USER_CURRENT);
                    
                    if (mUdfpsCustomIconSelect != null) {
                        mUdfpsCustomIconSelect.setSummary(R.string.udfps_custom_icon_selected);
                    }
                    
                    Toast.makeText(getActivity(), R.string.udfps_custom_icon_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.udfps_custom_icon_error, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }
}
