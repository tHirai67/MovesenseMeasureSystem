package com.example.movesensemeasuresystem.activity_connection;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.movesensemeasuresystem.R;
import com.example.movesensemeasuresystem.activity_record.RecordActivity;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsConnectionListener;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ConnectionActivity extends AppCompatActivity {
    private static final String TAG = "ConnectionActivity";

    private static final int REQUEST_CODE_BLUETOOTH_SCAN = 2;

    // MDS
    private Mds mMds;

    //UI
    private RecyclerView movesenseRecyclerView;
    private MovesenseAdapter movesenseAdapter;
    private ArrayList<MovesenseModel> movesenseModelArrayList;
    private Button btnMoveRecordActivity;
    private Button btnClickConnectButton;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        btnClickConnectButton = findViewById(R.id.btnConnect);
        btnMoveRecordActivity = findViewById(R.id.btnMoveRecordActivity);

        movesenseRecyclerView = findViewById(R.id.movesenseRecycleView);
        movesenseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemDecoration dividerItemDecorationDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        movesenseRecyclerView.addItemDecoration(dividerItemDecorationDecoration);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // MDS オブジェクトの初期化
        mMds = Mds.builder().build(this);
        if(ConnectedListMovesense.connectMovesenseList!=null){
            for(MovesenseModel m: ConnectedListMovesense.connectMovesenseList){
                mMds.disconnect(m.getSerial());
            }
        }
        if(ConnectedListMovesense.connectMovesenseList!=null){
            Log.d(TAG,"not null!!");
        }
        ConnectedListMovesense.connectMovesenseList = new ArrayList<>();
        resetRecyclerView();
    }

    public void onConnectButtonClick(View view){
        if(view.getId()==R.id.btnScan){
            resetRecyclerView();
            scanBluetoothDevices();
        }
    }

    private void resetRecyclerView(){
        movesenseModelArrayList = new ArrayList<>();
        movesenseAdapter = new MovesenseAdapter(movesenseModelArrayList);
        movesenseRecyclerView.setAdapter(movesenseAdapter);
    }

    private void scanBluetoothDevices() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 検出された端末情報を受信するための、BroadcastReceiver を登録
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        // 端末の検出を開始
        // 端末の検出を開始
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_CODE_BLUETOOTH_SCAN);

        }
        if (bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "Discovering");
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        showToast("デバイスを探しています...");
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "接続");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(ConnectionActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                }
                String deviceName = device.getName(); // Device name
                String deviceHardwareAddress = device.getAddress(); // MAC address

                // Movesense が見つかったときの処理
                if (deviceName != null && deviceName.contains("Movesense")) {
                    MovesenseModel movesenseModel = new MovesenseModel(deviceName.split(" ")[1], deviceHardwareAddress, false);
                    if(!movesenseModelArrayList.contains(movesenseModel)){
                        movesenseAdapter.add(movesenseModel);
                    }
                    movesenseAdapter.notifyDataSetChanged();
                    btnClickConnectButton.setVisibility(View.VISIBLE);
                    btnClickConnectButton.setEnabled(true);
                }
            }
        }
    };

    public void onClickConnectButton(View view) throws InterruptedException {
        if(movesenseModelArrayList != null){
            for (MovesenseModel m: movesenseModelArrayList) {
                if(m.isConnect){
                    connectMovesense(m);
                }
            }
        }
    }

    public void onClickMoveRecordActivity(View view){
        if(ConnectedListMovesense.connectMovesenseList!=null){
            startActivity(new Intent(ConnectionActivity.this, RecordActivity.class));
        }
    }


    private void connectMovesense(MovesenseModel m) {
        if(ConnectedListMovesense.connectMovesenseList == null) ConnectedListMovesense.connectMovesenseList = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_CODE_BLUETOOTH_SCAN);
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        Log.d(TAG, "movesenseModel:"+m.toString());

        // Movesense の MAC address と MdsConnectionListener を引数にして connect する
        mMds.connect(m.getMacAddress(), new MdsConnectionListener() {
            @Override
            public void onConnect(String macAddress) {
                Log.d(TAG, "onConnect: " + macAddress);
            }
            // onConnect から数秒間して onConnectionComplete が呼ばれる
            @Override
            public void onConnectionComplete(String macAddress, String serial) {
                Log.d(TAG, "onConnectionComplete: " + macAddress);
                showToast("Connection Complete\nserial:" + serial+"\nmacAddress:"+macAddress);
                //isConnectとBatteryLevelは最初からはめとく
                ConnectedListMovesense.connectMovesenseList.add(new MovesenseModel(serial,macAddress));
                getBatteryInfo(serial);
                Log.d(TAG, String.valueOf(ConnectedListMovesense.connectMovesenseList));
                btnClickConnectButton.setEnabled(false);
                btnClickConnectButton.setVisibility(View.GONE);
                btnMoveRecordActivity.setEnabled(true);
                btnMoveRecordActivity.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "onError:" + e);
                showConnectionError(e);
            }

            @Override
            public void onDisconnect(String bleAddress) {
                Log.d(TAG, "onDisconnect: " + bleAddress);
            }
        });

    }

    private void getBatteryInfo(String serial){
        mMds.get("suunto://"+serial+"/System/Energy/Level", null, new MdsResponseListener() {
            @Override
            public void onSuccess(String s) {
                Log.d(TAG,s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    int batteryLevel = jsonObject.getInt("Content");
                    movesenseAdapter.setBattery(batteryLevel);
                    movesenseAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Error getting battery info: " + e.getMessage());
            }
        });
    }


    private void showConnectionError(MdsException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Connection Error:")
                .setMessage(e.getMessage());
        builder.create().show();
    }

    private void showToast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(MovesenseModel m: ConnectedListMovesense.connectMovesenseList){
            mMds.disconnect(m.getMacAddress());
        }
    }
}