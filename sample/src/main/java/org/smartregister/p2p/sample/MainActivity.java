package org.smartregister.p2p.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import org.smartregister.p2p.activity.P2pModeSelectActivity;

public class MainActivity extends AppCompatActivity {

    private String[] permissions = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE
            , Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void requestPermissions() {
        if (!arePermissionsGranted()) {
            ActivityCompat.requestPermissions(this, permissions, 8348);
        } else {
            startActivity(new Intent(this, P2pModeSelectActivity.class));
        }
    }

    private boolean arePermissionsGranted() {
        for (String permission: permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
}
