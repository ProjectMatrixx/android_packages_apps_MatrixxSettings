/*
 * Copyright (C) 2018 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.preferences;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;

import com.android.internal.util.crdroid.Utils;
import com.android.settings.DisplaySettings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.CUSTOM_UI_TOGGLE;

public class CustomUIPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_CUSTOM_UI_TOGGLE = "custom_ui_toggle";

    public CustomUIPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CUSTOM_UI_TOGGLE;
    }

    @Override
    public void updateState(Preference preference) {
        int customUIToggleValue = Settings.System.getInt(mContext.getContentResolver(),
                CUSTOM_UI_TOGGLE, 0);
        ((SwitchPreference) preference).setChecked(customUIToggleValue != 0);
    }

    @Override
    public boolean isAvailable() {
	return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean customUIToggleValue = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), CUSTOM_UI_TOGGLE, customUIToggleValue ? 1 : 0);
        Utils.showSettingsRestartDialog(mContext);
        return true;
    }
}
