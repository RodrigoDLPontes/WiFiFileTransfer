package com.rodrigopontes.wififiletransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MenuActivity extends AppCompatActivity {

	TextView instructionsTextView;
	TextView ipAddressTextView;
	ImageButton switchButton;
	ImageView wifiLed;
	ImageView hddLed;
	ImageButton settingsButton;
	AdView adView;

	static HttpFileServer httpFileServer;
	static String formattedIpAddress;
	static ConnectivityManager cm;
	static boolean hasConnection;
	static short port = 8080;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		instructionsTextView = (TextView)findViewById(R.id.instructions_text_view);
		ipAddressTextView = (TextView)findViewById(R.id.ip_address_text_view);
		switchButton = (ImageButton)findViewById(R.id.switch_button);
		wifiLed = (ImageView)findViewById(R.id.wifi_led);
		hddLed = (ImageView)findViewById(R.id.hdd_led);
		adView = (AdView)findViewById(R.id.adView);

		new AsyncTask<Void, Void, AdRequest>() {
			@Override
			protected AdRequest doInBackground(Void... params) {
				return new AdRequest.Builder()
						.addTestDevice("F8D7EE5FF969EB12BE4735D286D3D767")
						.build();
			}

			@Override
			protected void onPostExecute(AdRequest request) {
				adView.loadAd(request);
			}
		}.execute();

		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		httpFileServer = new HttpFileServer(port, getApplicationContext(), this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(new BroadcastReceiver() {
			                 @Override
			                 public void onReceive(Context context, Intent intent) {
				                 hasConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
				                 if(hasConnection) {
					                 WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
					                 final int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
					                 formattedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
							                 (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
					                 switchButton.setImageResource(R.drawable.switch_button_off);
					                 instructionsTextView.setText(R.string.serverOffline);
					                 ipAddressTextView.setText("");
				                 } else {
					                 if(httpFileServer != null) httpFileServer.terminate();
					                 instructionsTextView.setText(R.string.noActiveConnection);
					                 ipAddressTextView.setText(R.string.pleaseConnectToWiFiFirst);
					                 switchButton.setImageResource(R.drawable.switch_button_off);
				                 }
			                 }
		                 }

				, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		hasConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
		if(hasConnection) {
			WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
			final int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
			formattedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
			if(httpFileServer.isAlive()) {
				switchButton.setImageResource(R.drawable.switch_button_on);
				instructionsTextView.setText(R.string.typeInBrowser);
				ipAddressTextView.setText(formattedIpAddress + ":" + port);
			} else {
				switchButton.setImageResource(R.drawable.switch_button_off);
				instructionsTextView.setText(R.string.serverOffline);
				ipAddressTextView.setText("");
			}
		} else {
			instructionsTextView.setText(R.string.noActiveConnection);
			ipAddressTextView.setText(R.string.pleaseConnectToWiFiFirst);
			switchButton.setImageResource(R.drawable.switch_button_off);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(httpFileServer != null) httpFileServer.terminate();
	}

	public void switchButtonPressed(View view) {
		if(hasConnection) {
			if(httpFileServer.isAlive()) {
				switchButton.setImageResource(R.drawable.switch_button_off);
				instructionsTextView.setText(R.string.serverOffline);
				ipAddressTextView.setText("");
				httpFileServer.terminate();
			} else {
				switchButton.setImageResource(R.drawable.switch_button_on);
				instructionsTextView.setText(R.string.typeInBrowser);
				ipAddressTextView.setText(formattedIpAddress + ":" + port);
				httpFileServer.create();
			}
		}
	}

	public void settingsButtonPressed(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.putExtra("Port", port);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(data != null) {
			final boolean serverWasRunning = httpFileServer.isAlive();
			final int newPort = data.getIntExtra("Port", 8080);
			if(newPort != port) {
				port = (short)newPort;
				httpFileServer.terminate();
				httpFileServer = new HttpFileServer(port, getApplicationContext(), this);
				if(serverWasRunning) {
					httpFileServer.create();
				}
			}
		}
	}

	protected void activateWiFiLED() {
		new AsyncTask<Void, Boolean, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for(byte i = 0 ; i < 2 ; i++) {
					publishProgress(true);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					publishProgress(false);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Boolean... state) {
				super.onProgressUpdate(state);
				if(state[0]) {
					wifiLed.setImageResource(R.drawable.led_on);
				} else {
					wifiLed.setImageResource(R.drawable.led_off);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void activateHDDLED() {
		new AsyncTask<Void, Boolean, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for(byte i = 0 ; i < 2 ; i++) {
					publishProgress(true);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					publishProgress(false);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Boolean... state) {
				super.onProgressUpdate(state);
				if(state[0]) {
					hddLed.setImageResource(R.drawable.led_on);
				} else {
					hddLed.setImageResource(R.drawable.led_off);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
