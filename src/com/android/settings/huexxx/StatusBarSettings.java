
package com.android.settings.huexxx;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class StatusBarSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_BATTERY_PERCENTAGE = "status_bar_battery_percentage";
    private static final String STATUS_BAR_DOUBLE_TAP_TO_SLEEP = "status_bar_double_tap_to_sleep";
    private static final String QUICK_PULLDOWN = "quick_pulldown";

    private ListPreference mStatusBarBattery;
    private CheckBoxPreference mStatusBarBatteryPercentage;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mStatusBarDoubleTapToSleep;
    private ListPreference mQuickPulldown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

        mStatusBarBattery = (ListPreference) getPreferenceScreen().findPreference(STATUS_BAR_BATTERY);
        mStatusBarBrightnessControl = (CheckBoxPreference) getPreferenceScreen().findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBatteryPercentage = (CheckBoxPreference) getPreferenceScreen().
                findPreference(STATUS_BAR_BATTERY_PERCENTAGE);
        mStatusBarDoubleTapToSleep = (CheckBoxPreference) getPreferenceScreen().
                findPreference(STATUS_BAR_DOUBLE_TAP_TO_SLEEP);
        mQuickPulldown = (ListPreference) getPreferenceScreen().findPreference(QUICK_PULLDOWN);

        // Battery style
        mStatusBarBattery.setOnPreferenceChangeListener(this);
        int batteryStyleValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                    Settings.System.STATUS_BAR_BATTERY, 0);
        switch (batteryStyleValue) {
        case 0:
        case 2:
            mStatusBarBatteryPercentage.setChecked(false);
            break;
        case 1:
        case 3:
            mStatusBarBatteryPercentage.setChecked(true);
            batteryStyleValue--;
            break;
        default:
            mStatusBarBatteryPercentage.setEnabled(false);
            mStatusBarBatteryPercentage.setSummary(R.string.status_bar_battery_percentage_info);
            batteryStyleValue = 4;
            break;
        }
        mStatusBarBattery.setValue(String.valueOf(batteryStyleValue));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());

        // Status bar brightness
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

        // Quick pulldown
        mQuickPulldown.setOnPreferenceChangeListener(this);
        int quickPulldownValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.QS_QUICK_PULLDOWN, 0);
        mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
        updatePulldownSummary(quickPulldownValue);

        // Double-tap to sleep
        mStatusBarDoubleTapToSleep.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.STATUS_BAR_DOUBLE_TAP_TO_SLEEP, 0) == 1));
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {

        if (preference == mStatusBarBattery) {
            int batteryStyleValue = Integer.valueOf((String) objValue);
            int batteryStyleIndex = mStatusBarBattery.findIndexOfValue((String) objValue);
            switch (batteryStyleValue) {
            case 0:
            case 2:
                mStatusBarBatteryPercentage.setEnabled(true);
                mStatusBarBatteryPercentage.setSummary(R.string.status_bar_battery_percentage_summary);
                if (mStatusBarBatteryPercentage.isChecked())
                    batteryStyleValue++;
                break;
            default:
                mStatusBarBatteryPercentage.setEnabled(false);
                mStatusBarBatteryPercentage.setSummary(R.string.status_bar_battery_percentage_info);
                batteryStyleValue = 4;
                break;
            }
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, batteryStyleValue);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[batteryStyleIndex]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) objValue);
            int quickPulldownIndex = mQuickPulldown.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, quickPulldownValue);
            mQuickPulldown.setSummary(mQuickPulldown.getEntries()[quickPulldownIndex]);
            updatePulldownSummary(quickPulldownValue);
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarDoubleTapToSleep) {
            value = mStatusBarDoubleTapToSleep.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_DOUBLE_TAP_TO_SLEEP, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBatteryPercentage) {
            value = mStatusBarBatteryPercentage.isChecked();
            int batteryStyleValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                        Settings.System.STATUS_BAR_BATTERY, 0);
            switch (batteryStyleValue) {
            case 0:
            case 2:
                if (value) {
                    batteryStyleValue++;
                    Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                            Settings.System.STATUS_BAR_BATTERY, batteryStyleValue);
                }
                break;
            case 1:
            case 3:
                if (!value) {
                    batteryStyleValue--;
                    Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                            Settings.System.STATUS_BAR_BATTERY, batteryStyleValue);
                }
                break;
            default:
                break;
            }
            return true;
        }
        return false;
    }

    private void updatePulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            /* quick pulldown deactivated */
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_summary_left
                    : R.string.quick_pulldown_summary_right);
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_summary, direction));
        }
    }
}