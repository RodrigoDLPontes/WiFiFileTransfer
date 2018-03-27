package com.rodrigopontes.wififiletransfer;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

	EditText portTextEdit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		portTextEdit = (EditText) findViewById(R.id.portTextEdit);
		portTextEdit.setText(String.valueOf(getIntent().getShortExtra("Port", (short)8080)));
		portTextEdit.requestFocus();
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
		portTextEdit.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
						(keyCode == KeyEvent.KEYCODE_ENTER)) {
					int port = Integer.parseInt(portTextEdit.getText().toString());
					if(port > 999 && port < 10000) {
						Intent intent = new Intent();
						intent.putExtra("Port", port);
						setResult(0, intent);
						finish();
					} else {
						Toast.makeText(SettingsActivity.this, "Please enter a 4 digit number", Toast.LENGTH_LONG).show();
					}
					return true;
				}
				return false;
			}
		});
	}
}
