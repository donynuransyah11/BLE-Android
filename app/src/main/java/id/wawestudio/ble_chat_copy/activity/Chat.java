package id.wawestudio.ble_chat_copy.activity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import id.wawestudio.ble_chat_copy.R;
import id.wawestudio.ble_chat_copy.ble_service.RBLService;


/*
Class Chat adalah reveiver service
 */

public class Chat extends AppCompatActivity{
	private final static String TAG = Chat.class.getSimpleName();

	public static final String EXTRAS_DEVICE = "EXTRAS_DEVICE";
	private TextView tv = null;
	private EditText et = null;
	private Button btn = null;
	private String mDeviceName;
	private String mDeviceAddress;

	private RBLService mBluetoothLeService;

	private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<>();

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		//
		@Override
		public void onServiceConnected(ComponentName componentName,
									   IBinder service) {
			Log.i(TAG, "onServiceConnected start: ");
			//initialize RBLservice
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();

			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			//start connect
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
			Log.i(TAG, "onServiceDisconnected: ");
		}
	};


	/*
	Receive broadcast from service
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.i(TAG, "onReceive: "+action);

			if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				getGattService(mBluetoothLeService.getSupportedGattService());
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getByteArrayExtra(RBLService.EXTRA_DATA));
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.second);

		tv = (TextView) findViewById(R.id.textView);
		tv.setMovementMethod(ScrollingMovementMethod.getInstance());
		et = (EditText) findViewById(R.id.editText);
		btn = (Button) findViewById(R.id.send);


		Intent intent = getIntent();

		mDeviceAddress = intent.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
		mDeviceName = intent.getStringExtra(Device.EXTRA_DEVICE_NAME);

		Log.i(TAG, "onCreate: "+mDeviceName+" "+mDeviceAddress);


		getSupportActionBar().setTitle(mDeviceName);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent gattServiceIntent = new Intent(Chat.this, RBLService.class);
		//CONNECT ACTIVITY CHAT TO SERVICE
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				BluetoothGattCharacteristic characteristic = map.get(RBLService.UUID_BLE_SHIELD_TX);
				Log.i(TAG, "onClickChat: "+characteristic);

				String str = et.getText().toString();
				byte b = 0x00;
				byte[] tmp = str.getBytes();
				byte[] tx = new byte[tmp.length + 1];
				tx[0] = b;
				for (int i = 1; i < tmp.length + 1; i++) {
					tx[i] = tmp[i - 1];
				}


				characteristic.setValue(tx);
				mBluetoothLeService.writeCharacteristic(characteristic);

				et.setText("");
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume: register_receive");
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService.close();

			System.exit(0);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();

		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mBluetoothLeService.disconnect();
		mBluetoothLeService.close();

		System.exit(0);
	}

	private void displayData(byte[] byteArray) {
		if (byteArray != null) {
			String data = new String(byteArray);
			tv.append(data);
			// find the amount we need to scroll. This works by
			// asking the TextView's internal layout for the position
			// of the final line and then subtracting the TextView's height
			final int scrollAmount = tv.getLayout().getLineTop(
					tv.getLineCount())
					- tv.getHeight();
			// if there is no need to scroll, scrollAmount will be <=0
			if (scrollAmount > 0)
				tv.scrollTo(0, scrollAmount);
			else
				tv.scrollTo(0, 0);
		}
	}




	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null) {
			Log.i(TAG, "getGattService: null");
			return;
		}

		BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

		Log.i(TAG, "getGattService: hmm :"+characteristic.getUuid()+" - "+ characteristic);
		map.put(characteristic.getUuid(), characteristic);

		BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		Log.i(TAG, "getGattService charactRx: "+characteristicRx);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}



	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

		Log.i(TAG, "makeGattUpdateIntentFilter: "+intentFilter);
		return intentFilter;
	}
}
