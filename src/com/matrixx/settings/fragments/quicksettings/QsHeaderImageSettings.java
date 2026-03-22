/*
 * Copyright (C) 2024 crDroid Android Project
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.matrixx.settings.utils.ImageUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SearchIndexable
public class QsHeaderImageSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "QsHeaderImageSettings";
    
    private static final String CUSTOM_HEADER_BROWSE = "custom_header_browse";
    private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
    private static final String CUSTOM_HEADER_PROVIDER = "qs_header_provider";
    private static final String STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header";
    private static final String FILE_HEADER_SELECT = "file_header_select";
    private static final int REQUEST_PICK_IMAGE = 10001;

    private static final String PROVIDER_DAYLIGHT = "daylight";
    private static final String PROVIDER_FILE = "file";

    private Preference mHeaderBrowse;
    private ListPreference mDaylightHeaderPack;
    private ListPreference mHeaderProvider;
    private Preference mFileHeader;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.qs_header_image_settings);

        ContentResolver resolver = getActivity().getContentResolver();

        mHeaderBrowse = findPreference(CUSTOM_HEADER_BROWSE);
        if (mHeaderBrowse != null) {
            mHeaderBrowse.setEnabled(isBrowseHeaderAvailable());
        }

        mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
        if (mDaylightHeaderPack != null) {
            List<String> entries = new ArrayList<>();
            List<String> values = new ArrayList<>();
            getAvailableHeaderPacks(entries, values);
            mDaylightHeaderPack.setEntries(entries.toArray(new String[0]));
            mDaylightHeaderPack.setEntryValues(values.toArray(new String[0]));
            updateHeaderProviderSummary();
            mDaylightHeaderPack.setOnPreferenceChangeListener(this);
        }

        String providerName = Settings.System.getStringForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER,
                UserHandle.USER_CURRENT);
        if (providerName == null) {
            providerName = PROVIDER_DAYLIGHT;
        }

        mHeaderProvider = (ListPreference) findPreference(CUSTOM_HEADER_PROVIDER);
        if (mHeaderProvider != null) {
            int valueIndex = mHeaderProvider.findIndexOfValue(providerName);
            mHeaderProvider.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
            mHeaderProvider.setSummary(mHeaderProvider.getEntry());
            mHeaderProvider.setOnPreferenceChangeListener(this);
        }

        updatePreferencesForProvider(providerName);

        mFileHeader = findPreference(FILE_HEADER_SELECT);
        if (mFileHeader != null) {
            mFileHeader.setEnabled(PROVIDER_FILE.equals(providerName));
        }
    }

    private void updatePreferencesForProvider(String providerName) {
        boolean isDaylight = PROVIDER_DAYLIGHT.equals(providerName);
        boolean isFile = PROVIDER_FILE.equals(providerName);

        if (mDaylightHeaderPack != null) {
            mDaylightHeaderPack.setEnabled(isDaylight);
        }

        if (mHeaderBrowse != null) {
            mHeaderBrowse.setEnabled(isBrowseHeaderAvailable() && !isFile);
        }

        if (mFileHeader != null) {
            mFileHeader.setEnabled(isFile);
        }
    }

    private void updateHeaderProviderSummary() {
        if (mDaylightHeaderPack == null) return;
        
        String settingHeaderPackage = Settings.System.getStringForUser(
                getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK,
                UserHandle.USER_CURRENT);
        
        int valueIndex = mDaylightHeaderPack.findIndexOfValue(settingHeaderPackage);
        if (valueIndex >= 0) {
            mDaylightHeaderPack.setValueIndex(valueIndex);
            mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == null || newValue == null) {
            return false;
        }

        ContentResolver resolver = getActivity().getContentResolver();
        String key = preference.getKey();

        if (DAYLIGHT_HEADER_PACK.equals(key)) {
            String value = (String) newValue;
            Settings.System.putStringForUser(resolver,
                    Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value,
                    UserHandle.USER_CURRENT);
            int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
            if (valueIndex >= 0) {
                mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
            }
            return true;

        } else if (CUSTOM_HEADER_PROVIDER.equals(key)) {
            String value = (String) newValue;
            Settings.System.putStringForUser(resolver,
                    Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, value,
                    UserHandle.USER_CURRENT);
            
            int valueIndex = mHeaderProvider.findIndexOfValue(value);
            if (valueIndex >= 0) {
                mHeaderProvider.setSummary(mHeaderProvider.getEntries()[valueIndex]);
            }
            
            updatePreferencesForProvider(value);
            
            if (mHeaderBrowse != null) {
                boolean isDaylight = PROVIDER_DAYLIGHT.equals(value);
                mHeaderBrowse.setTitle(isDaylight ? 
                        R.string.qs_header_browse_title : R.string.qs_header_pick_title);
                mHeaderBrowse.setSummary(isDaylight ? 
                        R.string.qs_header_browse_summary : R.string.qs_header_pick_summary);
            }
            
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == null || preference.getKey() == null) {
            return super.onPreferenceTreeClick(preference);
        }

        if (FILE_HEADER_SELECT.equals(preference.getKey())) {
            return handleFileHeaderSelect();
        }

        return super.onPreferenceTreeClick(preference);
    }

    private boolean handleFileHeaderSelect() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            String[] mimeTypes = {"image/gif", "image/webp", "image/png", "image/jpeg"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open image picker", e);
            Toast.makeText(getActivity(), 
                    R.string.qs_header_needs_gallery, 
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean isBrowseHeaderAvailable() {
        try {
            PackageManager pm = getActivity().getPackageManager();
            Intent browse = new Intent();
            browse.setClassName("org.omnirom.omnistyle", 
                    "org.omnirom.omnistyle.PickHeaderActivity");
            return pm.resolveActivity(browse, 0) != null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to check browse header availability", e);
            return false;
        }
    }

    private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        try {
            Map<String, String> headerMap = new HashMap<>();
            PackageManager pm = getActivity().getPackageManager();
            
            Intent intent = new Intent("org.omnirom.DaylightHeaderPack");
            for (ResolveInfo r : pm.queryIntentActivities(intent, 0)) {
                String packageName = r.activityInfo.packageName;
                String label = r.activityInfo.loadLabel(pm).toString();
                if (label == null || label.isEmpty()) {
                    label = packageName;
                }
                headerMap.put(label, packageName);
            }
            
            intent.setAction("org.omnirom.DaylightHeaderPack1");
            for (ResolveInfo r : pm.queryIntentActivities(intent, 0)) {
                if (r.activityInfo.name.endsWith(".theme")) {
                    continue;
                }
                String packageName = r.activityInfo.packageName;
                String label = r.activityInfo.loadLabel(pm).toString();
                if (label == null || label.isEmpty()) {
                    label = packageName;
                }
                headerMap.put(label, packageName + "/" + r.activityInfo.name);
            }
            
            List<String> labelList = new ArrayList<>(headerMap.keySet());
            Collections.sort(labelList);
            for (String label : labelList) {
                entries.add(label);
                values.add(headerMap.get(label));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get available header packs", e);
        }
    }

    public static void reset(Context context) {
        if (context == null) return;
        
        ContentResolver resolver = context.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER_HEIGHT, 142, UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(resolver,
                Settings.System.STATUS_BAR_FILE_HEADER_IMAGE, null, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MATRIXX;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        
        if (requestCode != REQUEST_PICK_IMAGE) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "Image picker cancelled");
            return;
        }

        if (result == null || result.getData() == null) {
            Log.w(TAG, "Image picker returned null data");
            Toast.makeText(getActivity(), 
                    R.string.qs_header_image_error, 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final Uri imageUri = result.getData();
        handleImageSelection(imageUri);
    }

    private void handleImageSelection(Uri imageUri) {
        Toast.makeText(getActivity(), 
                R.string.qs_header_image_processing, 
                Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String savedImagePath = ImageUtils.saveImageToInternalStorage(
                    getActivity(), imageUri, "qs_header_image", "QS_HEADER_IMAGE");
            
            getActivity().runOnUiThread(() -> {
                if (savedImagePath != null) {
                    ContentResolver resolver = getActivity().getContentResolver();
                    Settings.System.putStringForUser(resolver, 
                            Settings.System.STATUS_BAR_FILE_HEADER_IMAGE, 
                            savedImagePath, 
                            UserHandle.USER_CURRENT);
                    
                    Toast.makeText(getActivity(), 
                            R.string.qs_header_image_success, 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to save image");
                    Toast.makeText(getActivity(), 
                            R.string.qs_header_image_error, 
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    List<SearchIndexableResource> result = new ArrayList<>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.qs_header_image_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return new ArrayList<>();
                }
            };
}
