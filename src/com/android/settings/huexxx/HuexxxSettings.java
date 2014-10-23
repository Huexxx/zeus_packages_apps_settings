
package com.android.settings.huexxx;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.net.NetworkUtils;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class HuexxxSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String ADB_TCPIP  = "adb_over_network";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_LIGHT_OPTIONS = "category_light_options";
    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_NOTIFICATION_LIGHT = "notification_light";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_BATTERY_PERCENTAGE = "status_bar_battery_percentage";
    private static final String STATUS_BAR_DOUBLE_TAP_TO_SLEEP = "status_bar_double_tap_to_sleep";
    private static final String TAG = "HuexxxSettings";

    // Dialog identifiers used in showDialog
    private static final int DLG_ADBTCP = 0;

    private CheckBoxPreference mAdbOverNetwork;
    private CheckBoxPreference mNotificationPulse;
    private CheckBoxPreference mStatusBarBatteryPercentage;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mStatusBarDoubleTapToSleep;
    private ListPreference mNavigationBarHeight;
    private ListPreference mQuickPulldown;
    private ListPreference mStatusBarBattery;
    private PreferenceCategory mLightOptions;
    private PreferenceScreen mNotificationLight;
    private PreferenceScreen mBatteryPulse;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.huexxx_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mAdbOverNetwork = (CheckBoxPreference) findPreference(ADB_TCPIP);
        mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
        mLightOptions = (PreferenceCategory) prefSet.findPreference(KEY_LIGHT_OPTIONS);
        mNavigationBarHeight = (ListPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        mNotificationLight = (PreferenceScreen) findPreference(KEY_NOTIFICATION_LIGHT);
        mQuickPulldown = (ListPreference) getPreferenceScreen().findPreference(QUICK_PULLDOWN);
        mStatusBarBattery = (ListPreference) getPreferenceScreen().findPreference(STATUS_BAR_BATTERY);
        mStatusBarBatteryPercentage = (CheckBoxPreference) getPreferenceScreen().
                findPreference(STATUS_BAR_BATTERY_PERCENTAGE);
        mStatusBarBrightnessControl = (CheckBoxPreference) getPreferenceScreen().findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarDoubleTapToSleep = (CheckBoxPreference) getPreferenceScreen().
                findPreference(STATUS_BAR_DOUBLE_TAP_TO_SLEEP);

        // ADB over network
        updateAdbOverNetwork();

        // Navigation bar height
        mNavigationBarHeight.setOnPreferenceChangeListener(this);
        int statusNavigationBarHeight = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.NAVIGATION_BAR_HEIGHT, 48);
        mNavigationBarHeight.setValue(String.valueOf(statusNavigationBarHeight));
        mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntry());

        // LED notifications
        if (mNotificationPulse != null && mNotificationLight != null && mBatteryPulse != null) {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                 if (getResources().getBoolean(
                         com.android.internal.R.bool.config_multiColorNotificationLed)) {
                     mLightOptions.removePreference(mNotificationPulse);
                     updateLightPulseDescription();
                 } else {
                     mLightOptions.removePreference(mNotificationLight);
                     try {
                         mNotificationPulse.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                                 Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                     } catch (SettingNotFoundException e) {
                         e.printStackTrace();
                     }
                 }
            } else {
                 mLightOptions.removePreference(mNotificationPulse);
                 mLightOptions.removePreference(mNotificationLight);
            }

            if (!getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveBatteryLed)) {
                mLightOptions.removePreference(mBatteryPulse);
            } else {
                updateBatteryPulseDescription();
            }

            //If we're removed everything, get rid of the category
            if (mLightOptions.getPreferenceCount() == 0) {
                prefSet.removePreference(mLightOptions);
            }
        }

        // Quick pulldown
        mQuickPulldown.setOnPreferenceChangeListener(this);
        int quickPulldownValue = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.QS_QUICK_PULLDOWN, 0);
        mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
        updatePulldownSummary(quickPulldownValue);

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

        // Double-tap to sleep
        mStatusBarDoubleTapToSleep.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), 
                Settings.System.STATUS_BAR_DOUBLE_TAP_TO_SLEEP, 0) == 1));
    }

    @Override
    public void onResume() {
        super.onResume();

        updateAdbOverNetwork();
        updateBatteryPulseDescription();
        updateLightPulseDescription();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mAdbOverNetwork) {
            if (mAdbOverNetwork.isChecked()) {
                showDialogInner(DLG_ADBTCP);
            } else {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ADB_PORT, -1);
                updateAdbOverNetwork();
            }
        } else if (preference == mNotificationPulse) {
            value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
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
        } else if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarDoubleTapToSleep) {
            value = mStatusBarDoubleTapToSleep.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_DOUBLE_TAP_TO_SLEEP, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNavigationBarHeight) {
            int statusNavigationBarHeight = Integer.valueOf((String) objValue);
            int index = mNavigationBarHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, statusNavigationBarHeight);
            mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntries()[index]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) objValue);
            int quickPulldownIndex = mQuickPulldown.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, quickPulldownValue);
            mQuickPulldown.setSummary(mQuickPulldown.getEntries()[quickPulldownIndex]);
            updatePulldownSummary(quickPulldownValue);
            return true;
        } else if (preference == mStatusBarBattery) {
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
        }
        return false;
    }

    private void updateAdbOverNetwork() {
        final ContentResolver resolver = getActivity().getContentResolver();

        int adb = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0);

        if (adb == 0) {
            mAdbOverNetwork.setEnabled(false);
            mAdbOverNetwork.setSummary(R.string.adb_disabled_summary);
            return;
        }

        int port = Settings.Secure.getInt(resolver, Settings.Secure.ADB_PORT, 0);
        boolean enabled = port > 0;

        mAdbOverNetwork.setChecked(enabled);

        WifiInfo wifiInfo = null;

        if (enabled) {
            IWifiManager wifiManager = IWifiManager.Stub.asInterface(
                    ServiceManager.getService(Context.WIFI_SERVICE));
            try {
                wifiInfo = wifiManager.getConnectionInfo();
            } catch (RemoteException e) {
                Log.e(TAG, "wifiManager, getConnectionInfo()", e);
            }
        }

        if (wifiInfo != null) {
            String hostAddress = NetworkUtils.intToInetAddress(
                    wifiInfo.getIpAddress()).getHostAddress();
            mAdbOverNetwork.setSummary(hostAddress + ":" + String.valueOf(port));
        } else {
            mAdbOverNetwork.setSummary(R.string.adb_over_network_summary);
        }
    }

    private void updateBatteryPulseDescription() {
        if (mBatteryPulse == null) {
            return;
        }
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.generic_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.generic_disabled));
        }
     }

    private void updateLightPulseDescription() {
        if (mNotificationLight == null) {
            return;
        }
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationLight.setSummary(getString(R.string.generic_enabled));
        } else {
            mNotificationLight.setSummary(getString(R.string.generic_disabled));
        }
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

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        HuexxxSettings getOwner() {
            return (HuexxxSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_ADBTCP:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.adb_over_network)
                    .setMessage(getActivity().getString(R.string.adb_over_network_warning))
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.ADB_PORT, 5555);
                            getOwner().updateAdbOverNetwork();
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Reset the toggle
                            getOwner().mAdbOverNetwork.setChecked(false);
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }
    }
}
