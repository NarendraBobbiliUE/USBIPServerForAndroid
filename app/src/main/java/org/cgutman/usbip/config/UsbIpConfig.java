package org.cgutman.usbip.config;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.pm.PackageManager;

import org.cgutman.usbip.service.UsbIpService;
import org.cgutman.usbipserverforandroid.R;

public class UsbIpConfig extends ComponentActivity {
    private Button serviceButton;
    private TextView serviceStatus;
    private TextView serviceReadyText;
    
    private boolean running;
    private final UsbReceiver usbReceiver = new UsbReceiver();

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

        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running) {
                    stopService(new Intent(UsbIpConfig.this, UsbIpService.class));
                }
                else {
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

        // Register receiver with the correct ContextCompat API
        IntentFilter filter = new IntentFilter("org.cgutman.usbipserverforandroid.USB_PERMISSION");
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver correctly
        unregisterReceiver(usbReceiver);
    }

    // Define UsbReceiver class
    public static class UsbReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle USB permission broadcast here
        }
    }
}
