package org.sharpai.aicamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        SharedPreferences sharedPref = this.getSharedPreferences("SHARE_AI_CAMERA_PREF",Context.MODE_PRIVATE);
        String textViewDisplayString = "";
        String defaultServerIP = getResources().getString(R.string.default_api_server_ip);
        String defaultMinioIP = getResources().getString(R.string.default_minio_server_ip);
        String defaultServerPort = getResources().getString(R.string.default_api_server_port);
        String defaultMinioPort = getResources().getString(R.string.default_minio_server_port);

        String savedAPIServerIP = sharedPref.getString(getString(R.string.saved_api_server_ip), defaultServerIP);
        String savedMinioIP = sharedPref.getString(getString(R.string.saved_minio_server_ip), defaultMinioIP);

        String savedAPIServerPort = sharedPref.getString(getString(R.string.saved_api_server_port), defaultServerPort);
        String savedMinioPort = sharedPref.getString(getString(R.string.saved_minio_server_port), defaultMinioPort);
        Log.d("SETUP","API Server:"+savedAPIServerIP);

        EditText serverAddressView =  findViewById(R.id.apiServerAddress);
        serverAddressView.setText(savedAPIServerIP);

        EditText serverPortView =  findViewById(R.id.apiServerPort);
        serverPortView.setText(savedAPIServerPort);

        EditText minioAddressView =  findViewById(R.id.minioServerAddress);
        minioAddressView.setText(savedMinioIP);

        EditText minioPortView =  findViewById(R.id.minioServerPort);
        minioPortView.setText(savedMinioPort);

        Button button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String apiServerAddress = serverAddressView.getText().toString();
                String apiServerPort = serverPortView.getText().toString();
                String minioServerAddress = minioAddressView.getText().toString();
                String minioServerPort = minioPortView.getText().toString();

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.saved_api_server_ip), apiServerAddress);
                editor.putString(getString(R.string.saved_api_server_port), apiServerPort);
                editor.putString(getString(R.string.saved_minio_server_ip), minioServerAddress);
                editor.putString(getString(R.string.saved_minio_server_port), minioServerPort);
                editor.apply();
            }
        });
    }
}
