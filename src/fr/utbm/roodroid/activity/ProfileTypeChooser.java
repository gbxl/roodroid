package fr.utbm.roodroid.activity;

import fr.utbm.roodroid.ApplicationManager;
import fr.utbm.roodroid.ApplicationManager.ConnectionMode;
import fr.utbm.roodroid.ApplicationManager.ProfileType;
import fr.utbm.roodroid.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;

public class ProfileTypeChooser extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		RadioButton rbServer = (RadioButton) findViewById(R.id.serverRadioBtn);
		RadioButton rbClient = (RadioButton) findViewById(R.id.clientRadioBtn);
		RadioButton rbWifi = (RadioButton) findViewById(R.id.wifiRadioBtn);
		RadioButton rbBluetooth = (RadioButton) findViewById(R.id.BtRadioBtn);
		
		if (ApplicationManager.getInstance().getProfilType() == ProfileType.Client) {
			rbClient.setChecked(true);
			rbServer.setChecked(false);
		} else {
			rbServer.setChecked(true);
			rbClient.setChecked(false);
		}
		if (ApplicationManager.getInstance().getConnectionMode() == ConnectionMode.Wifi) {
			rbWifi.setChecked(true);
			rbBluetooth.setChecked(false);
		} else {
			rbBluetooth.setChecked(true);
			rbWifi.setChecked(false);
		}
		
        Button next = (Button)findViewById(R.id.nextBtn);
        next.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				RadioButton rbClient = (RadioButton) findViewById(R.id.clientRadioBtn);
				RadioButton rbWifi = (RadioButton) findViewById(R.id.wifiRadioBtn);
				Intent myIntent;
				Context context = v.getContext();
				if (rbClient.isChecked()) {
					ApplicationManager.getInstance().setProfileType(ProfileType.Client);
					if (rbWifi.isChecked()) {
						ApplicationManager.getInstance().setConnectionMode(ConnectionMode.Wifi);
						myIntent = new Intent(context, ClientWifiSettings.class);
					} else {
						ApplicationManager.getInstance().setConnectionMode(ConnectionMode.Bluetooth);
						myIntent = new Intent(context, ClientBluetoothSettings.class);
					}
				} else {
					ApplicationManager.getInstance().setProfileType(ProfileType.Server);
					if (rbWifi.isChecked()) {
						ApplicationManager.getInstance().setConnectionMode(ConnectionMode.Wifi);
						myIntent = new Intent(context, ServerWifiSettings.class);
					} else {
						ApplicationManager.getInstance().setConnectionMode(ConnectionMode.Bluetooth);
						myIntent = new Intent(context, ServerBluetoothSettings.class);
					}
				}
                startActivity(myIntent);
			}
		});
    }

}