package org.cgutman.usbip.config;

import org.cgutman.usbip.service.UsbIpService;
import org.cgutman.usbipserverforandroid.R;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class UsbIpConfig extends ComponentActivity {
    private static final String ACTION_USB_PERMISSION = "org.cgutman.usbipserverforandroid.USB_PERMISSION";

    private Button serviceButton;
    private TextView serviceStatus;
    private TextView serviceReadyText;
    
    private boolean running;

    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private BroadcastReceiver usbReceiver;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // We don't actually care if the permission is granted or not. We will launch the service anyway.
                startService(new Intent(UsbIpConfig.this, UsbIpService.class));
            });
    
    private void updateStatus() {
        if (running) {
            serviceButton.setText("Stop Service");
            serviceStatus.setText("USB/IP Service Running");
            serviceReadyText.setText(R.string.ready_text);
        }
        else {
            serviceButton.setText("Start Service");
            serviceStatus.setText("USB/IP Service Stopped");
            serviceReadyText.setText("");
        }
    }
    
    // Elegant Stack Overflow solution to querying running services
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbip_config);

        serviceButton = findViewById(R.id.serviceButton);
        serviceStatus = findViewById(R.id.serviceStatus);
        serviceReadyText = findViewById(R.id.serviceReadyText);
        
        running = isMyServiceRunning(UsbIpService.class);
        
        updateStatus();

        // Initialize UsbManager and PendingIntent
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        // Register the BroadcastReceiver
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                handleUsbDevice(device);  // Handle the USB device here
                            }
                        } else {
                            // Permission denied
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && !usbManager.hasPermission(device)) {
                        usbManager.requestPermission(device, permissionIntent);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);

        serviceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running) {
                    stopService(new Intent(UsbIpConfig.this, UsbIpService.class));
                } else {
                    if (ContextCompat.checkSelfPermission(UsbIpConfig.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        startService(new Intent(UsbIpConfig.this, UsbIpService.class));
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                }

                running = !running;
                updateStatus();
            }
        });
    }

    private void handleUsbDevice(UsbDevice device) {
        // Implement your code to handle the USB device here
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }
}
