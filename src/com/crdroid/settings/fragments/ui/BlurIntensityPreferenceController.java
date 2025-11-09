/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.settings.fragments.miscellaneous;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class BlurIntensityPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY = "blur_intensity";

    public BlurIntensityPreferenceController(Context context, String key) {
        super(context, key);
    }

    public BlurIntensityPreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(KEY)) {
            int value = (Integer) newValue;
            // Blur intensity percentage (0-200%)
            // 0% = no blur, 100% = system default, 200% = double intensity
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.BLUR_INTENSITY, value);
            return true;
        }
        return false;
    }
}
