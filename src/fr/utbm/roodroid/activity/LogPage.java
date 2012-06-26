package fr.utbm.roodroid.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import fr.utbm.roodroid.ApplicationManager;
import fr.utbm.roodroid.R;

public class LogPage extends Activity{

	TextView logView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);

		logView = (TextView)findViewById(R.id.logView);
		
		File sdcard = Environment.getExternalStorageDirectory();

		File file = new File(sdcard,"/RoodroidLog.log");

		StringBuilder text = new StringBuilder();

		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;

		    while ((line = br.readLine()) != null) {
		        text.append(line);
		        text.append('\n');
		    }
		}
		catch (IOException e) {
		    e.printStackTrace();
		    ApplicationManager.appendLog(Log.ERROR, "Read log", "Log loading failed");
		}

		logView.setText(text);
	}
	
	

}
