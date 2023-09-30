/*
 * Copyright (C) 2021 AospExtended ROM Project
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

package com.crdroid.settings.fragments.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.text.TextUtils;
import androidx.preference.PreferenceViewHolder;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.util.crdroid.ThemeUtils;
import com.android.internal.util.crdroid.Utils;

import com.crdroid.settings.preferences.SystemSettingListPreference;
import com.crdroid.settings.preferences.SystemSettingSwitchPreference;
import com.crdroid.settings.preferences.SystemSettingEditTextPreference;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONException;

public class UIStyles extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String SETTINGS_DASHBOARD_STYLE = "settings_dashboard_style";
    private static final String SETTINGS_HEADER_IMAGE = "settings_header_image";
    private static final String SETTINGS_HEADER_IMAGE_RANDOM = "settings_header_image_random";
    private static final String SETTINGS_HEADER_TEXT = "settings_header_text";
    private static final String SETTINGS_HEADER_TEXT_ENABLED = "settings_header_text_enabled";
    private static final String SETTINGS_CONTEXTUAL_MESSAGES = "settings_contextual_messages";
    private static final String USE_STOCK_LAYOUT = "use_stock_layout";
    private static final String ABOUT_PHONE_STYLE = "about_card_style";
    private static final String HIDE_USER_CARD = "hide_user_card";

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private String mCategory = "android.theme.customization.style.android";

    private SystemSettingListPreference mSettingsDashBoardStyle;
    private SystemSettingListPreference mAboutPhoneStyle;
    private SystemSettingSwitchPreference mUseStockLayout;
    private SystemSettingSwitchPreference mHideUserCard;
    private Preference mSettingsHeaderImage;
    private Preference mSettingsHeaderImageRandom;
    private Preference mSettingsMessage;
    private SystemSettingEditTextPreference mSettingsHeaderText;
    private SystemSettingSwitchPreference mSettingsHeaderTextEnabled;

    private List<String> mPkgs;

    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler();
    private final AtomicBoolean mApplyingOverlays = new AtomicBoolean(false);

    Map<String, String> overlayMap = new HashMap<String, String>();
    {
        overlayMap.put("com.android.settings", "android.theme.customization.style.settings");
        overlayMap.put("com.android.systemui", "android.theme.customization.style.systemui");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_customization_ui_style_title);

        mThemeUtils = new ThemeUtils(getActivity());
        mPkgs = mThemeUtils.getOverlayPackagesForCategory(mCategory, "android");

        mCustomSettingsObserver.observe();

        mSettingsDashBoardStyle = (SystemSettingListPreference) findPreference(SETTINGS_DASHBOARD_STYLE);
        mSettingsDashBoardStyle.setOnPreferenceChangeListener(this);
        mSettingsHeaderImageRandom = findPreference(SETTINGS_HEADER_IMAGE_RANDOM);
        mSettingsHeaderImageRandom.setOnPreferenceChangeListener(this);
        mSettingsMessage = findPreference(SETTINGS_CONTEXTUAL_MESSAGES);
        mSettingsMessage.setOnPreferenceChangeListener(this);
        mSettingsHeaderImage = findPreference(SETTINGS_HEADER_IMAGE);
        mSettingsHeaderImage.setOnPreferenceChangeListener(this);
        mUseStockLayout = (SystemSettingSwitchPreference) findPreference(USE_STOCK_LAYOUT);
        mUseStockLayout.setOnPreferenceChangeListener(this);
        mAboutPhoneStyle = (SystemSettingListPreference) findPreference(ABOUT_PHONE_STYLE);
        mAboutPhoneStyle.setOnPreferenceChangeListener(this);
        mHideUserCard = (SystemSettingSwitchPreference) findPreference(HIDE_USER_CARD);
        mHideUserCard.setOnPreferenceChangeListener(this);
        mSettingsHeaderText = (SystemSettingEditTextPreference) findPreference(SETTINGS_HEADER_TEXT);
        mSettingsHeaderText.setOnPreferenceChangeListener(this);
        mSettingsHeaderTextEnabled = (SystemSettingSwitchPreference) findPreference(SETTINGS_HEADER_TEXT_ENABLED);
        mSettingsHeaderTextEnabled.setOnPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.item_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 1);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        Adapter mAdapter = new Adapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSettingsDashBoardStyle) {
            mCustomSettingsObserver.observe();
            return true;
        } else if (preference == mUseStockLayout) {
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mHideUserCard) {
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mAboutPhoneStyle) {
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderImage) {
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderImageRandom) {
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsMessage) {
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderTextEnabled) {
            boolean enable = (Boolean) newValue;
            SystemProperties.set("persist.sys.settings.header_text_enabled", enable ? "true" : "false");
            Utils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mSettingsHeaderText) {
            String value = (String) newValue;
            SystemProperties.set("persist.sys.settings.header_text", value);
            Utils.showSettingsRestartDialog(getContext());
            return true;
         }
         return false;
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SETTINGS_DASHBOARD_STYLE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.SETTINGS_DASHBOARD_STYLE))) {
                updateSettingsStyle();
            }
        }
    }

    private void updateSettingsStyle() {
        ContentResolver resolver = getActivity().getContentResolver();

        int settingsPanelStyle = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SETTINGS_DASHBOARD_STYLE, 0, UserHandle.USER_CURRENT);

	// reset all overlays before applying
        mThemeUtils.setOverlayEnabled("android.theme.customization.icon_pack.settings", "com.android.settings", "com.android.settings");

	if (settingsPanelStyle == 0) return;

        switch (settingsPanelStyle) {
            case 1:
              setSettingsStyle("com.android.system.settings.rui");
              break;
            case 2:
              setSettingsStyle("com.android.system.settings.arc");
              break;
            case 3:
              setSettingsStyle("com.android.system.settings.aosp");
              break;
            case 4:
              setSettingsStyle("com.android.system.settings.mt");
              break;
            case 5:
              setSettingsStyle("com.android.system.settings.card");
              break;
            default:
              break;
        }
    }

    public void setSettingsStyle(String overlayName) {
       mThemeUtils.setOverlayEnabled("android.theme.customization.icon_pack.settings", overlayName, "com.android.settings");
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CRDROID_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        Context context;
        String mSelectedPkg;
        String mAppliedPkg;

        public Adapter(Context context) {
            this.context = context;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fonts_option, parent, false);
            CustomViewHolder vh = new CustomViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            String pkg = mPkgs.get(position);
            String label = getLabel(holder.itemView.getContext(), pkg);

            String currentPackageName = mThemeUtils.getOverlayInfos(mCategory).stream()
                .filter(info -> info.isEnabled())
                .map(info -> info.packageName)
                .findFirst()
                .orElse("android");

            holder.title.setText("android".equals(pkg) ? "Default" : label);
            holder.title.setTextSize(20);
            holder.name.setVisibility(View.GONE);

            if (currentPackageName.equals(pkg)) {
                mAppliedPkg = pkg;
                if (mSelectedPkg == null) {
                    mSelectedPkg = pkg;
                }
            }

            holder.itemView.setActivated(pkg == mSelectedPkg);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mApplyingOverlays.get()) return;
                    updateActivatedStatus(mSelectedPkg, false);
                    updateActivatedStatus(pkg, true);
                    mSelectedPkg = pkg;
                    enableOverlays(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView title;
            public CustomViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.option_title);
                name = (TextView) itemView.findViewById(R.id.option_label);
            }
        }

        private void updateActivatedStatus(String pkg, boolean isActivated) {
            int index = mPkgs.indexOf(pkg);
            if (index < 0) {
                return;
            }
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && holder.itemView != null) {
                holder.itemView.setActivated(isActivated);
            }
        }
    }

    public String getLabel(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(pkg, 0)
                    .loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pkg;
    }

    public void enableOverlays(int position) {
        mApplyingOverlays.set(true);
        mExecutor.execute(() -> {
            mThemeUtils.setOverlayEnabled(mCategory, mPkgs.get(position), "android");
            String pattern = "android".equals(mPkgs.get(position)) ? ""
                    : mPkgs.get(position).split("\\.")[4];
            for (Map.Entry<String, String> entry : overlayMap.entrySet()) {
                enableOverlay(entry.getValue(), entry.getKey(), pattern);
            }
            mHandler.post(() -> mApplyingOverlays.set(false));
        });
    }

    public void enableOverlay(String category, String target, String pattern) {
        if (pattern.isEmpty()) {
            mThemeUtils.setOverlayEnabled(category, "android", "android");
            return;
        }
        for (String pkg: mThemeUtils.getOverlayPackagesForCategory(category, target)) {
            if (pkg.contains(pattern)) {
                mThemeUtils.setOverlayEnabled(category, pkg, target);
            }
        }
    }
}
