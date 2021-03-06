package fr.utbm.roodroid.activity;

import fr.utbm.roodroid.ApplicationManager;
import fr.utbm.roodroid.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;;

public class ClientBluetoothSettings extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.client_bluetooth);
      
        EditText username = (EditText) findViewById(R.id.usernameText);
        username.setText(ApplicationManager.getInstance().getUsername());
        
        Button btnLookup = (Button) findViewById(R.id.lookupServerBtn);
        btnLookup.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
	            Intent serverIntent = new Intent(v.getContext(), BluetoothDiscovery.class);
	            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	        }
		});
        
        Button btnBack = (Button) findViewById(R.id.backBtn);
        btnBack.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
    }
    
    @Override
    public void onStart() {
    	super.onStart();

    	if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    	}
    	
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString(BluetoothDiscovery.EXTRA_DEVICE_ADDRESS);
               	String username = ((EditText)findViewById(R.id.usernameText)).getText().toString();
               	
               	try {
					ApplicationManager.getInstance().createClientBluetooth(username, address);
	                startActivity(new Intent(this, ConversationsList.class));
	                ApplicationManager.appendLog(Log.DEBUG, "ok", "client connected");
				} catch (Exception e) {
					e.printStackTrace();
				}
				
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}