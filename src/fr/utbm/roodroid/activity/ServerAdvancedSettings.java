package fr.utbm.roodroid.activity;

import fr.utbm.roodroid.ApplicationManager;
import fr.utbm.roodroid.ApplicationManager.Authentication;
import fr.utbm.roodroid.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.view.View.OnClickListener;

public class ServerAdvancedSettings extends Activity {
	
	private AuthorizedUsernamesAdapter adapter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_options);
        
        final EditText maxMess = (EditText) findViewById(R.id.nbMaxMessEdit);
        maxMess.setText(""+ApplicationManager.getInstance().getNbMaxMessages());
		
        final EditText maxClients = (EditText) findViewById(R.id.nbMaxClientsEdit);
        maxClients.setText(""+ApplicationManager.getInstance().getNbMaxClients());
        
        final ListView list = (ListView) findViewById(R.id.allowedIDsList);

        Button btnDone = (Button) findViewById(R.id.serverOptionDoneBtn);
        btnDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ApplicationManager.getInstance().saveAuthorizedUsernames();
				ApplicationManager.getInstance().setNbMaxClients(Integer.parseInt(maxClients.getText().toString()));
				ApplicationManager.getInstance().setNbMaxMessages(Integer.parseInt(maxMess.getText().toString()));
				finish();
			}
		});
        
		final Button btnAdd = (Button) findViewById(R.id.serverOptionAddBtn);
		btnAdd.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showPopUpUsername();
			}
		});
		
		final Button btnPassword = (Button) findViewById(R.id.serverOptionPasswordBtn);
		btnPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopUpPassword();
			}
		});
		
		final TextView textUsernames = (TextView) findViewById(R.id.listAllowedIdsTitle);
		
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.authentificationTypeSelector);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				btnPassword.setVisibility(android.view.View.INVISIBLE);
				btnAdd.setVisibility(android.view.View.INVISIBLE);
				list.setVisibility(android.view.View.INVISIBLE);
				textUsernames.setVisibility(android.view.View.INVISIBLE);
				switch (checkedId) {
				case R.id.idAuthentificationRadioBtn:
					btnAdd.setVisibility(android.view.View.VISIBLE);
					list.setVisibility(android.view.View.VISIBLE);
					textUsernames.setVisibility(android.view.View.VISIBLE);
					ApplicationManager.getInstance().setAuthenticationMode(Authentication.ID);
					break;
				case R.id.passwordAuthentificationRadioBtn:
					btnPassword.setVisibility(android.view.View.VISIBLE);
					ApplicationManager.getInstance().setAuthenticationMode(Authentication.Password);
					break;
				case R.id.noAuthentificationRadioBtn:
					ApplicationManager.getInstance().setAuthenticationMode(Authentication.None);
					break;
				}
			}
		});
		
		adapter = new AuthorizedUsernamesAdapter(this);
		list.setAdapter(adapter);
	}

	private void showPopUpPassword() {
		AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
		helpBuilder.setTitle("Set password");

		final EditText input = new EditText(this);
		input.setTransformationMethod(new PasswordTransformationMethod());
		input.setSingleLine();
		input.setText("");
		helpBuilder.setView(input);

		helpBuilder.setPositiveButton("Set password", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				ApplicationManager.getInstance().setPassword(input.getText().toString());
				adapter.notifyDataSetChanged();
			}
		});

		helpBuilder.setNegativeButton("Cancel", null);

		AlertDialog helpDialog = helpBuilder.create();
		helpDialog.show();
	}
	
	private void showPopUpUsername() {
		AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
		helpBuilder.setTitle("Set authorized usernames");

		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setText("");
		helpBuilder.setView(input);

		helpBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				ApplicationManager.getInstance().addAuthozizedUsername(input.getText().toString());
				adapter.notifyDataSetChanged();
			}
		});

		helpBuilder.setNegativeButton("Cancel", null);

		AlertDialog helpDialog = helpBuilder.create();
		helpDialog.show();
	}
}