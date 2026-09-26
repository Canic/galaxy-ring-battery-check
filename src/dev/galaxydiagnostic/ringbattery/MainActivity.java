package dev.galaxydiagnostic.ringbattery;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = "RingBatteryTest";
    private static final String BT_PERMISSION = "android.permission.BLUETOOTH_CONNECT";
    private static final UUID DATA_SERVICE = UUID.fromString("00001b1b-0000-1000-8000-00805f9b34fb");
    private static final UUID TX = UUID.fromString("797ae4e9-2e58-4fe8-b48d-b5c79599fb9b");
    private static final UUID RX = UUID.fromString("63e30bad-4206-4596-839f-e47cbf7a4b5d");
    private static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final int INK = Color.rgb(15, 23, 42);
    private static final int MUTED = Color.rgb(71, 85, 105);
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int GREEN = Color.rgb(4, 120, 87);
    private static final int AMBER = Color.rgb(180, 83, 9);
    private static final int RED = Color.rgb(185, 28, 28);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusTitle, statusDetail, deviceName;
    private TextView healthValue, consumptionValue, capacityValue;
    private View statusDot;
    private LinearLayout resultCard;
    private ProgressBar progressBar;
    private Button startButton;
    private volatile BluetoothGatt gatt;
    private volatile BluetoothGattCharacteristic writeCharacteristic;
    private volatile boolean busy, testSent, handshakeSeen;
    private boolean servicesStarted, permissionPermanentlyDenied;
    private Runnable timeout;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(245, 247, 251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildScreen();
        if (checkSelfPermission(BT_PERMISSION) == PackageManager.PERMISSION_GRANTED) showReady();
        else showPermissionNeeded();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(int stringId, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(stringId);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(round(Color.WHITE, 20));
        card.setElevation(dp(2));
        return card;
    }

    private LinearLayout.LayoutParams cardParams(int marginTop) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginTop);
        return params;
    }

    private TextView addMetric(LinearLayout parent, int labelId) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(226, 232, 240));
        LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, dp(1));
        line.topMargin = dp(17);
        parent.addView(divider, line);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(15);
        parent.addView(row, rowParams);
        row.addView(text(labelId, 14, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView value = text(R.string.normal_value, 14, INK, true);
        value.setGravity(Gravity.END);
        row.addView(value, new LinearLayout.LayoutParams(-2, -2));
        return value;
    }

    private void buildScreen() {
        final ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 251));
        scroll.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                scroll.setPadding(0, insets.getSystemWindowInsetTop(), 0,
                        insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            }
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(32), dp(24), dp(36));
        scroll.addView(content);
        setContentView(scroll);

        View ringIcon = new View(this);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dp(6), BLUE);
        ringIcon.setBackground(ring);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconParams.bottomMargin = dp(24);
        content.addView(ringIcon, iconParams);
        TextView eyebrow = text(R.string.eyebrow, 12, BLUE, true);
        eyebrow.setLetterSpacing(0.14f);
        content.addView(eyebrow);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(6);
        content.addView(text(R.string.screen_title, 31, INK, true), titleParams);
        TextView intro = text(R.string.screen_intro, 16, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.topMargin = dp(12);
        content.addView(intro, introParams);

        LinearLayout statusCard = card();
        content.addView(statusCard, cardParams(28));
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.addView(statusRow);
        statusDot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(11), dp(11));
        dotParams.rightMargin = dp(12);
        statusRow.addView(statusDot, dotParams);
        statusTitle = text(R.string.ready_title, 21, INK, true);
        statusRow.addView(statusTitle, new LinearLayout.LayoutParams(-1, -2));
        statusDetail = text(R.string.ready_detail, 15, MUTED, false);
        statusDetail.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(12);
        statusCard.addView(statusDetail, detailParams);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(BLUE));
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(4));
        progressParams.topMargin = dp(18);
        statusCard.addView(progressBar, progressParams);

        startButton = new Button(this);
        startButton.setAllCaps(false);
        startButton.setTextSize(16);
        startButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        startButton.setTextColor(Color.WHITE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onStartPressed(); }
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(56));
        buttonParams.topMargin = dp(20);
        content.addView(startButton, buttonParams);

        LinearLayout deviceCard = card();
        content.addView(deviceCard, cardParams(20));
        deviceCard.addView(text(R.string.device_label, 13, MUTED, false));
        deviceName = text(R.string.device_unknown, 16, INK, true);
        LinearLayout.LayoutParams deviceParams = new LinearLayout.LayoutParams(-1, -2);
        deviceParams.topMargin = dp(4);
        deviceCard.addView(deviceName, deviceParams);

        resultCard = card();
        content.addView(resultCard, cardParams(16));
        resultCard.addView(text(R.string.details_title, 20, INK, true));
        healthValue = addMetric(resultCard, R.string.health_label);
        consumptionValue = addMetric(resultCard, R.string.consumption_label);
        capacityValue = addMetric(resultCard, R.string.capacity_label);
        TextView note = text(R.string.capacity_note, 13, MUTED, false);
        note.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(20);
        resultCard.addView(note, noteParams);
        resultCard.setVisibility(View.GONE);
    }

    private void updateStatus(int title, int detail, int color, boolean loading) {
        statusTitle.setText(title);
        statusTitle.setTextColor(color);
        statusDetail.setText(detail);
        statusDot.setBackground(round(color, 100));
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void styleButton(int label, boolean enabled) {
        startButton.setText(label);
        startButton.setEnabled(enabled);
        startButton.setBackground(round(enabled ? BLUE : Color.rgb(148, 163, 184), 14));
    }

    private void showReady() {
        updateStatus(R.string.ready_title, R.string.ready_detail, BLUE, false);
        styleButton(R.string.start_test, true);
        resultCard.setVisibility(View.GONE);
    }

    private void showPermissionNeeded() {
        updateStatus(R.string.permission_title, R.string.permission_detail, AMBER, false);
        styleButton(permissionPermanentlyDenied ? R.string.open_settings : R.string.allow_access, true);
        resultCard.setVisibility(View.GONE);
    }

    private void onStartPressed() {
        if (permissionPermanentlyDenied && checkSelfPermission(BT_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } else startTest();
    }

    @Override protected void onResume() {
        super.onResume();
        if (permissionPermanentlyDenied && checkSelfPermission(BT_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            permissionPermanentlyDenied = false;
            showReady();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != 1) return;
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            permissionPermanentlyDenied = false;
            startTest();
        } else {
            permissionPermanentlyDenied = !shouldShowRequestPermissionRationale(BT_PERMISSION);
            showPermissionNeeded();
        }
    }

    private void startTest() {
        if (busy) return;
        if (checkSelfPermission(BT_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{BT_PERMISSION}, 1);
            return;
        }
        closeGatt();
        testSent = false;
        handshakeSeen = false;
        servicesStarted = false;
        resultCard.setVisibility(View.GONE);
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            showError(R.string.bluetooth_off_title, R.string.bluetooth_off_detail);
            return;
        }
        BluetoothDevice ring = null;
        try {
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            for (BluetoothDevice device : bonded) {
                String name = device.getName();
                if (name != null && name.startsWith("Galaxy Ring")) { ring = device; break; }
            }
        } catch (SecurityException error) {
            showPermissionNeeded();
            return;
        }
        if (ring == null) {
            showError(R.string.ring_missing_title, R.string.ring_missing_detail);
            return;
        }
        deviceName.setText(ring.getName());
        busy = true;
        updateStatus(R.string.connecting_title, R.string.progress_detail, BLUE, true);
        styleButton(R.string.testing, false);
        try {
            gatt = ring.connectGatt(this, false, callback);
            if (gatt == null) {
                showError(R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
        } catch (SecurityException error) {
            busy = false;
            showPermissionNeeded();
            return;
        } catch (RuntimeException error) {
            Log.w(TAG, "Cannot connect to ring", error);
            showError(R.string.connection_error_title, R.string.connection_error_detail);
            return;
        }
        final BluetoothGatt attempt = gatt;
        timeout = new Runnable() {
            @Override public void run() {
                if (busy && gatt == attempt) showError(R.string.test_timeout_title, R.string.test_timeout_detail);
            }
        };
        handler.postDelayed(timeout, 30000);
    }

    private void cancelTimeout() {
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null;
    }

    private void closeGatt() {
        BluetoothGatt old = gatt;
        gatt = null;
        writeCharacteristic = null;
        if (old != null) old.close();
    }

    private void showError(int title, int detail) {
        Log.w(TAG, getString(title));
        busy = false;
        cancelTimeout();
        closeGatt();
        updateStatus(title, detail, RED, false);
        styleButton(R.string.try_again, true);
        resultCard.setVisibility(View.GONE);
    }

    private void progress(final BluetoothGatt current, final int title) {
        handler.post(new Runnable() {
            @Override public void run() {
                if (busy && gatt == current) updateStatus(title, R.string.progress_detail, BLUE, true);
            }
        });
    }

    private void fail(final BluetoothGatt current, final int title, final int detail) {
        handler.post(new Runnable() {
            @Override public void run() {
                if (busy && gatt == current) showError(title, detail);
            }
        });
    }

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt current, int status, int newState) {
            if (current != gatt || !busy) return;
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                progress(current, R.string.preparing_title);
                if (!current.requestMtu(498)) discoverOnce(current);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "GATT disconnected: " + status);
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
            }
        }

        @Override public void onMtuChanged(BluetoothGatt current, int mtu, int status) {
            if (current == gatt && busy) discoverOnce(current);
        }

        @Override public void onServicesDiscovered(BluetoothGatt current, int status) {
            if (current != gatt || !busy) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
            BluetoothGattService service = current.getService(DATA_SERVICE);
            if (service == null) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
            BluetoothGattCharacteristic notify = service.getCharacteristic(TX);
            writeCharacteristic = service.getCharacteristic(RX);
            if (notify == null || writeCharacteristic == null || !current.setCharacteristicNotification(notify, true)) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
            BluetoothGattDescriptor descriptor = notify.getDescriptor(CCC);
            if (descriptor == null) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
            progress(current, R.string.waiting_title);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!current.writeDescriptor(descriptor)) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
            }
        }

        @Override public void onDescriptorWrite(final BluetoothGatt current, BluetoothGattDescriptor descriptor, int status) {
            if (current != gatt || !busy) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
            // An existing shared connection may have already completed the handshake.
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    if (busy && current == gatt && !handshakeSeen && !testSent) sendBatteryTest(current);
                }
            }, 5000);
        }

        @Override public void onCharacteristicWrite(BluetoothGatt current, BluetoothGattCharacteristic characteristic, int status) {
            if (current != gatt || !busy) return;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                fail(current, R.string.connection_error_title, R.string.connection_error_detail);
                return;
            }
            byte[] value = characteristic.getValue();
            if (value != null && value.length == 167 && (value[0] & 255) == 0xa7) sendBatteryTest(current);
        }

        @Override public void onCharacteristicChanged(BluetoothGatt current, BluetoothGattCharacteristic characteristic) {
            if (current != gatt || !busy) return;
            byte[] value = characteristic.getValue();
            if (value != null && value.length == 167 && (value[0] & 255) == 0xa7 && !testSent) {
                handshakeSeen = true;
                sendHandshake(current);
            } else if (value != null && value.length >= 3 &&
                    (value[0] & 255) == 0x21 && (value[1] & 255) == 0x21 &&
                    (value[2] & 0x7f) == 0x45) {
                decodeBatteryResult(current, value);
            }
        }
    };

    private synchronized void discoverOnce(BluetoothGatt current) {
        if (servicesStarted || current != gatt || !busy) return;
        servicesStarted = true;
        if (!current.discoverServices()) {
            fail(current, R.string.connection_error_title, R.string.connection_error_detail);
        }
    }

    private void sendHandshake(BluetoothGatt current) {
        BluetoothGattCharacteristic characteristic = writeCharacteristic;
        if (characteristic == null) return;
        byte[] packet = new byte[167];
        packet[0] = (byte) 0xa7;
        packet[2] = 1;
        packet[3] = 1;
        packet[4] = 1;
        packet[5] = 1;
        packet[6] = (byte) Build.VERSION.SDK_INT;
        packet[8] = (byte) 0xf2;
        packet[9] = 1;
        packet[10] = (byte) 0xd4;
        packet[11] = 3;
        packet[12] = 10;
        byte[] vendor = new byte[]{'S', 'A', 'M', 'S', 'U', 'N', 'G'};
        System.arraycopy(vendor, 0, packet, 13, vendor.length);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        characteristic.setValue(packet);
        if (!current.writeCharacteristic(characteristic)) {
            fail(current, R.string.connection_error_title, R.string.connection_error_detail);
        }
    }

    private void sendBatteryTest(BluetoothGatt current) {
        BluetoothGattCharacteristic characteristic = writeCharacteristic;
        if (!busy || current != gatt || testSent || characteristic == null) return;
        testSent = true;
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        characteristic.setValue(new byte[]{0x21, 0x21, 0x05});
        progress(current, R.string.testing_title);
        Log.i(TAG, "Battery self-test request sent");
        if (!current.writeCharacteristic(characteristic)) {
            fail(current, R.string.connection_error_title, R.string.connection_error_detail);
        }
    }

    private void decodeBatteryResult(final BluetoothGatt current, byte[] data) {
        Integer health = null, consumption = null;
        String capacity = null;
        for (int pos = 3; pos < data.length;) {
            int id = data[pos++] & 255;
            if ((id == 6 || id == 7) && pos < data.length) {
                int value = data[pos++] & 255;
                if (id == 6) health = value;
                else consumption = value;
            } else if (id == 8 && pos < data.length) {
                int length = data[pos++] & 255;
                if (length > data.length - pos) break;
                capacity = new String(data, pos, length, StandardCharsets.US_ASCII);
                pos += length;
            } else break;
        }
        if (health == null || consumption == null || capacity == null) {
            fail(current, R.string.test_error_title, R.string.test_error_detail);
            return;
        }
        final int healthCode = health, consumptionCode = consumption;
        final String capacityText = capacity;
        Log.i(TAG, "Battery result: health=" + healthCode + ", consumption=" + consumptionCode + ", capacity=" + capacityText);
        handler.post(new Runnable() {
            @Override public void run() {
                if (!busy || current != gatt) return;
                busy = false;
                cancelTimeout();
                closeGatt();
                boolean attention = healthCode != 0 || consumptionCode != 0;
                updateStatus(attention ? R.string.attention_title : R.string.normal_title,
                        attention ? R.string.attention_detail : R.string.normal_detail,
                        attention ? AMBER : GREEN, false);
                healthValue.setText(healthCode == 0 ? R.string.normal_value : R.string.attention_value);
                healthValue.setTextColor(healthCode == 0 ? GREEN : AMBER);
                consumptionValue.setText(consumptionCode == 0 ? R.string.normal_value : R.string.attention_value);
                consumptionValue.setTextColor(consumptionCode == 0 ? GREEN : AMBER);
                capacityValue.setText(capacityText);
                capacityValue.setTextColor(INK);
                resultCard.setVisibility(View.VISIBLE);
                styleButton(R.string.run_again, true);
            }
        });
    }

    @Override protected void onDestroy() {
        busy = false;
        cancelTimeout();
        closeGatt();
        super.onDestroy();
    }
}
