
package com.android.settings.huexxx;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBarSettings extends SettingsPreferenceFragment {

    // General
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_NATIVE_BATTERY_PERCENTAGE = "status_bar_native_battery_percentage";

    // General
    private CheckBoxPreference mStatusBarNativeBatteryPercentage;
    private CheckBoxPreference mStatusBarBrightnessControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

            mStatusBarBrightnessControl = (CheckBoxPreference) getPreferenceScreen().findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
            mStatusBarNativeBatteryPercentage = (CheckBoxPreference) getPreferenceScreen().
                    findPreference(STATUS_BAR_NATIVE_BATTERY_PERCENTAGE);

            // Status bar brightness control
            if (!Utils.isPhone(getActivity())) {
                // only show on phones
                getPreferenceScreen().removePreference(mStatusBarBrightnessControl);
            } else {
                mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                        Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
                try {
                    if (Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        mStatusBarBrightnessControl.setEnabled(false);
                        mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
                    }
                } catch (SettingNotFoundException e) {
                }
            }

            // Native battery percentage
            mStatusBarNativeBatteryPercentage.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                    Settings.System.STATUS_BAR_NATIVE_BATTERY_PERCENTAGE, 0) == 1));

        }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarNativeBatteryPercentage) {
            value = mStatusBarNativeBatteryPercentage.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NATIVE_BATTERY_PERCENTAGE, value ? 1 : 0);
            return true;
        }
        return false;
    }
}
