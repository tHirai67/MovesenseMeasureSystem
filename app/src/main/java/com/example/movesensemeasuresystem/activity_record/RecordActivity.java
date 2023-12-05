package com.example.movesensemeasuresystem.activity_record;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.movesensemeasuresystem.Constants;
import com.example.movesensemeasuresystem.activity_connection.MovesenseModel;
import com.example.movesensemeasuresystem.R;
import com.example.movesensemeasuresystem.activity_connection.ConnectedListMovesense;
import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    private static final String TAG = "RecordActivity";

    private static ArrayList<MovesenseModel> movesenseList;

    //Movesense lib
    private MdsSubscription mdsSubscription1_acc;
    private MdsSubscription mdsSubscription1_gyro;
    private MdsSubscription mdsSubscription2_acc;
    private MdsSubscription mdsSubscription2_gyro;
    private Mds mds;

    //info
    private long first_time;

    //file
    private File file_acc_1;
    private File file_gyro_1;
    private File file_acc_2;
    private File file_gyro_2;

    private final String ANGULAR_VELOCITY_PATH = "Meas/Gyro/";
    private int sampleRate = 52;

    private CsvLogger csvLogger_acc_1;
    private CsvLogger csvLogger_gyro_1;
    private CsvLogger csvLogger_acc_2;
    private CsvLogger csvLogger_gyro_2;

    private boolean isRecord;


    //UI
    private TextView tvMovesenseInfo1;
    private TextView tvMovesenseInfo2;
    private TextView tvSensorInfo1;
    private TextView tvSensorInfo2;
    private Button btnRecord;
    private Spinner spinner1;
    private Spinner spinner2;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        tvMovesenseInfo1 = findViewById(R.id.tvMovesenseInfo1);
        tvMovesenseInfo2 = findViewById(R.id.tvMovesenseInfo2);

        tvSensorInfo1 = findViewById(R.id.tvSensorInfo1);
        tvSensorInfo2 = findViewById(R.id.tvSensorInfo2);

        btnRecord = findViewById(R.id.btnRecord);

        spinner1 = findViewById(R.id.spSensorPosition1);
        spinner2 = findViewById(R.id.spSensorPosition2);

        ArrayAdapter<CharSequence> adapterSensorPosition = ArrayAdapter.createFromResource(this, R.array.sensorPosition_array, android.R.layout.simple_spinner_item);
        adapterSensorPosition.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapterSensorPosition);
        spinner2.setAdapter(adapterSensorPosition);
        spinner1.setSelection(0);
        spinner2.setSelection(1);

        movesenseList = new ArrayList<>();
        if(ConnectedListMovesense.connectMovesenseList!=null){
            int i =0;
            for(MovesenseModel m:ConnectedListMovesense.connectMovesenseList){
                Log.d(TAG, m.toString());
                movesenseList.add(m);
                if(i==0){
                    tvMovesenseInfo1.setText("Movesense1\n"+m.getSerial()+"\n"+m.getMacAddress());
                }else if(i==1){
                    tvMovesenseInfo2.setText("Movesense2\n"+m.getSerial()+"\n"+m.getMacAddress());
                }
                i++;
            }
        }else{
            Log.d(TAG,"null");
        }
        mds = Mds.builder().build(this);
        isRecord = true;
    }

    public void onClickRecord(View view){
        if(view.getId() == R.id.btnRecord){
            if(isRecord){

                // Get Current Timestamp in format suitable for file names (i.e. no : or other bad chars)
                Date date = new Date();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String currentTimestamp = formatter.format(date);
                //file name1
                // timestamp + device serial + data type,
                StringBuilder sb_acc_1 = new StringBuilder();
                StringBuilder sb_gyro_1 = new StringBuilder();
                StringBuilder sb_acc_2 = new StringBuilder();
                StringBuilder sb_gyro_2 = new StringBuilder();

                sb_acc_1.append(movesenseList.get(0).getSerial()).append("_").append(spinner1.getSelectedItem().toString()).append("_").append(currentTimestamp).append("_acc.csv");
                sb_gyro_1.append(movesenseList.get(0).getSerial()).append("_").append(spinner1.getSelectedItem().toString()).append("_").append(currentTimestamp).append("_gyro.csv");
                sb_acc_2.append(movesenseList.get(0).getSerial()).append("_").append(spinner1.getSelectedItem().toString()).append("_").append(currentTimestamp).append("_acc.csv");
                sb_gyro_2.append(movesenseList.get(0).getSerial()).append("_").append(spinner1.getSelectedItem().toString()).append("_").append(currentTimestamp).append("_gyro.csv");

                Context context = getApplicationContext();
                file_acc_1 = new File(context.getFilesDir(),sb_acc_1.toString());
                file_gyro_1 = new File(context.getFilesDir(),sb_gyro_1.toString());
                file_acc_2 = new File(context.getFilesDir(),sb_acc_2.toString());
                file_gyro_2 = new File(context.getFilesDir(),sb_gyro_2.toString());

                csvLogger_acc_1 = new CsvLogger(file_acc_1);
                csvLogger_gyro_1 = new CsvLogger(file_gyro_1);
                csvLogger_acc_2 = new CsvLogger(file_acc_2);
                csvLogger_gyro_2 = new CsvLogger(file_gyro_2);

                isRecord = false;
                first_time=System.currentTimeMillis();
                subscribeToSensor2(movesenseList.get(1).getSerial());
                subscribeToSensor1(movesenseList.get(0).getSerial());
                btnRecord.setText("Stop Recording");
            }else{
                isRecord = true;
                unsubscribe1_acc();
                unsubscribe1_gyro();
                unsubscribe2_acc();
                unsubscribe2_gyro();
                btnRecord.setText("Start Recording");
                csvLogger_acc_1.finishSavingLogs();
                csvLogger_gyro_1.finishSavingLogs();
                csvLogger_acc_2.finishSavingLogs();
                csvLogger_gyro_2.finishSavingLogs();
            }
        }
    }

    private void subscribeToSensor1(String deviceSerial1) {
        // //Movesense1の加速度の登録
        if (mdsSubscription1_acc != null) {
            unsubscribe1_acc();
        }
        String accUri = Constants.URI_MEAS_ACC_52;
        String strContract_acc_1 = "{\"Uri\": \"" + deviceSerial1 + accUri + "\"}";

        mdsSubscription1_acc = Mds.builder().build(this).subscribe(Constants.URI_EVENTLISTENER, strContract_acc_1, new MdsNotificationListener() {

            // センサ値を受け取るメソッド
            @Override
            public void onNotification(String data) {
                MovesenseAccDataResponse accResponse = new Gson().fromJson(data, MovesenseAccDataResponse.class);

                if (accResponse != null && accResponse.body.array.length > 0) {
                    StringBuffer sb = new StringBuffer();
                    double time = System.currentTimeMillis() - first_time;
                    double x = accResponse.body.array[0].x;
                    double y = accResponse.body.array[0].y;
                    double z = accResponse.body.array[0].z;
                    Log.d(TAG, deviceSerial1 + "\n経過時刻Time:" + String.valueOf(time)
                            + "\nsensorTime:" + String.valueOf(accResponse.body.timestamp)
                            + "\nx:" + String.valueOf(x)
                            + "\ny:" + String.valueOf(y)
                            + "\nz:" + String.valueOf(z));
                    sb.append("経過時刻:" + (double) Math.round((double) time / 10) / 100 + "秒");
                    sb.append("\n");
                    sb.append("x:" + x);
                    sb.append("\n");
                    sb.append("y:" + y);
                    sb.append("\n");
                    sb.append("z:" + z);
                    tvSensorInfo1.setText(sb.toString());
                    csvLogger_acc_1.appendHeader("Sensor time (ms),System time (ms),X (m/s^2),Y (m/s^2),Z (m/s^2)");
                    csvLogger_acc_1.appendLine(String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f", accResponse.body.timestamp, time / 1000, x, y, z));
                }
            }

            @Override
            public void onError(MdsException error) {
                Log.e(TAG, "subscription onError(): ", error);
                unsubscribe1_acc();
                onDestroy();
            }
        });

        //Movesense1の角速度の登録
        if (mdsSubscription1_gyro != null) {
            unsubscribe1_gyro();
        }
        String gyroUri = ANGULAR_VELOCITY_PATH + sampleRate;
        String strContract_gyro_1 =  "{\"Uri\": \"" + deviceSerial1 + gyroUri+  "\"}";

        mdsSubscription1_gyro = Mds.builder().build(this).subscribe(Constants.URI_EVENTLISTENER, FormatHelper.formatContractToJson(deviceSerial1, ANGULAR_VELOCITY_PATH + sampleRate), new MdsNotificationListener() {

            // センサ値を受け取るメソッド
            @Override
            public void onNotification(String data) {
                MovesenseGyroDataResponse gyroResponse = new Gson().fromJson(data, MovesenseGyroDataResponse.class);

                if (gyroResponse != null & gyroResponse.body.array.length > 0) {
                    double time=System.currentTimeMillis()-first_time;
                    double x = gyroResponse.body.array[0].x;
                    double y = gyroResponse.body.array[0].y;
                    double z = gyroResponse.body.array[0].z;
                    csvLogger_gyro_1.appendHeader("Sensor time (ms),System time (ms),X (m/s^2),Y (m/s^2),Z (m/s^2)");
                    csvLogger_gyro_1.appendLine(String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f", gyroResponse.body.timestamp, time/1000,x,y,z));
                }
            }

            @Override
            public void onError(MdsException error) {
                Log.e(TAG, "subscription onError(): ", error);
                unsubscribe1_gyro();
                onDestroy();
            }
        });
    }

    private void subscribeToSensor2(String deviceSerial2) {
        if (mdsSubscription2_acc != null) {
            unsubscribe2_acc();
        }

        String accUri = Constants.URI_MEAS_ACC_52;
        String strContract_acc_2 = "{\"Uri\": \"" + deviceSerial2 + accUri + "\"}";
        mdsSubscription2_acc = Mds.builder().build(this).subscribe(Constants.URI_EVENTLISTENER, strContract_acc_2, new MdsNotificationListener() {

            // センサ値を受け取るメソッド
            @Override
            public void onNotification(String data) {
                MovesenseAccDataResponse accResponse = new Gson().fromJson(data, MovesenseAccDataResponse.class);
                MovesenseGyroDataResponse gyroResponse = new Gson().fromJson(data, MovesenseGyroDataResponse.class);
                if (accResponse != null && accResponse.body.array.length > 0) {
                    StringBuffer sb = new StringBuffer();
                    double time=System.currentTimeMillis()-first_time;
                    double x = accResponse.body.array[0].x;
                    double y = accResponse.body.array[0].y;
                    double z = accResponse.body.array[0].z;
                    Log.d(TAG,deviceSerial2+"\n経過時刻Time:"+ String.valueOf(time)
                            +"\nsensorTime:"+String.valueOf(accResponse.body.timestamp)
                            +"\nx:"+String.valueOf(x)
                            +"\ny:"+ String.valueOf(y)
                            +"\nz:"+String.valueOf(z));
                    sb.append("経過時刻:"+(double)Math.round((double)time/10)/100+"秒");
                    sb.append("\n");
                    sb.append("x:"+x);
                    sb.append("\n");
                    sb.append("y:"+y);
                    sb.append("\n");
                    sb.append("z:"+z);
                    tvSensorInfo2.setText(sb.toString());
                    csvLogger_acc_2.appendHeader("Sensor time (ms),System time (ms),X (m/s^2),Y (m/s^2),Z (m/s^2)");
                    csvLogger_acc_2.appendLine(String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f", accResponse.body.timestamp, time/1000,x,y,z));
                }
            }

            @Override
            public void onError(MdsException error) {
                Log.e(TAG, "subscription onError(): ", error);
                unsubscribe2_acc();
                onDestroy();
            }
        });

        //Movesense1の角速度の登録
        if (mdsSubscription2_gyro != null) {
            unsubscribe2_gyro();
        }
        String gyroUri = ANGULAR_VELOCITY_PATH + sampleRate;
        String strContract_gyro_2 =  "{\"Uri\": \"" + deviceSerial2 + gyroUri+  "\"}";

        mdsSubscription2_gyro = Mds.builder().build(this).subscribe(Constants.URI_EVENTLISTENER, FormatHelper.formatContractToJson(deviceSerial2, ANGULAR_VELOCITY_PATH + sampleRate), new MdsNotificationListener() {

            // センサ値を受け取るメソッド
            @Override
            public void onNotification(String data) {
                MovesenseGyroDataResponse gyroResponse = new Gson().fromJson(data, MovesenseGyroDataResponse.class);

                if (gyroResponse != null & gyroResponse.body.array.length > 0) {
                    double time=System.currentTimeMillis()-first_time;
                    double x = gyroResponse.body.array[0].x;
                    double y = gyroResponse.body.array[0].y;
                    double z = gyroResponse.body.array[0].z;
                    csvLogger_gyro_1.appendHeader("Sensor time (ms),System time (ms),X (m/s^2),Y (m/s^2),Z (m/s^2)");
                    csvLogger_gyro_1.appendLine(String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f", gyroResponse.body.timestamp, time/1000,x,y,z));
                }
            }

            @Override
            public void onError(MdsException error) {
                Log.e(TAG, "subscription onError(): ", error);
                unsubscribe2_gyro();
                onDestroy();
            }
        });

    }

    private void unsubscribe1_acc() {
        if (mdsSubscription1_acc != null) {
            mdsSubscription1_acc.unsubscribe();
            mdsSubscription1_acc = null;
        }
    }

    private void unsubscribe1_gyro() {
        if (mdsSubscription1_gyro != null) {
            mdsSubscription1_gyro.unsubscribe();
            mdsSubscription1_gyro = null;
        }
    }

    private void unsubscribe2_acc() {
        if(mdsSubscription2_acc != null){
            mdsSubscription2_acc.unsubscribe();
            mdsSubscription2_acc = null;
        }
    }

    private void unsubscribe2_gyro() {
        if (mdsSubscription2_gyro != null) {
            mdsSubscription2_gyro.unsubscribe();
            mdsSubscription2_gyro = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(MovesenseModel m: movesenseList){
            mds.disconnect(m.getMacAddress());
        }
    }
}