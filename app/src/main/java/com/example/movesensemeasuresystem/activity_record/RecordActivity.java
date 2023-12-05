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
import com.example.movesensemeasuresystem.ImuModel;
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
    private MdsSubscription mdsSubscription1;
    private MdsSubscription mdsSubscription2;
    private Mds mds;

    //info
    private long first_time;

    //file
    private File file1;
    private File file2;

    private final String IMU6_PATH = "Meas/IMU6/";
    private final String IMU9_PATH = "Meas/IMU9/";

    private CsvLogger csvLogger1;
    private CsvLogger csvLogger2;

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
                StringBuilder sb1 = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();

                sb1.append(movesenseList.get(0).getSerial()).append("_").append(spinner1.getSelectedItem().toString()).append("_").append(currentTimestamp).append("_acc.csv");
                sb2.append(movesenseList.get(0).getSerial()).append("_").append(spinner1.getSelectedItem().toString()).append("_").append(currentTimestamp).append("_acc.csv");

                Context context = getApplicationContext();
                file1 = new File(context.getFilesDir(),sb1.toString());
                file2 = new File(context.getFilesDir(),sb2.toString());

                csvLogger1 = new CsvLogger(file1);
                csvLogger2 = new CsvLogger(file2);

                isRecord = false;
                first_time=System.currentTimeMillis();
                //subscribeToSensor2(movesenseList.get(1).getSerial());
                subscribeToSensor1(movesenseList.get(0).getSerial());
                btnRecord.setText("Stop Recording");
            }else{
                isRecord = true;
                unsubscribe1();
                unsubscribe2();
                btnRecord.setText("Start Recording");
                csvLogger1.finishSavingLogs();
                csvLogger2.finishSavingLogs();
            }
        }
    }

    private void subscribeToSensor1(String deviceSerial1) {
        // //Movesense1の加速度の登録
        if (mdsSubscription1 != null) {
            unsubscribe1();
        }
        String Uri = Constants.IMU6_PATH;
        String strContract2 = "{\"Uri\": \"" + deviceSerial1 + Uri + "\"}";
        mdsSubscription2 = Mds.builder().build(this).subscribe(Constants.URI_EVENTLISTENER, strContract2, new MdsNotificationListener() {

            // センサ値を受け取るメソッド
            @Override
            public void onNotification(String data) {
                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);
                double time=System.currentTimeMillis()-first_time;
                double x = imuModel.getBody().getArrayAcc()[0].getX();
                double y = imuModel.getBody().getArrayAcc()[0].getY();
                double z = imuModel.getBody().getArrayAcc()[0].getZ();
                double gx = imuModel.getBody().getArrayAcc()[0].getX();
                double gy = imuModel.getBody().getArrayAcc()[0].getY();
                double gz = imuModel.getBody().getArrayAcc()[0].getZ();
                Log.d(TAG,deviceSerial1+"\n経過時刻Time:"+ String.valueOf(time)
                        +"\nx:"+String.valueOf(x)
                        +"\ny:"+ String.valueOf(y)
                        +"\nz:"+String.valueOf(z)
                        +"\ngx:"+String.valueOf(gx)
                        +"\ngy:"+ String.valueOf(gy)
                        +"\ngz:"+String.valueOf(gz));
//                sb.append("経過時刻:"+(double)Math.round((double)time/10)/100+"秒");
//                sb.append("\n");
//                sb.append("x:"+x);
//                sb.append("\n");
//                sb.append("y:"+y);
//                sb.append("\n");
//                sb.append("z:"+z);
                //tvSensorInfo2.setText(sb.toString());
//                csvLogger2.appendHeader("Sensor time (ms),System time (ms),X (m/s^2),Y (m/s^2),Z (m/s^2)");
//                csvLogger2.appendLine(String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f", imuModel.body.timestamp, time/1000,x,y,z));
            }

            @Override
            public void onError(MdsException error) {
                Log.e(TAG, "subscription onError(): ", error);
                unsubscribe2();
                onDestroy();
            }
        });

    }

//    private void subscribeToSensor2(String deviceSerial2) {
//        if (mdsSubscription2 != null) {
//            unsubscribe2();
//        }
//
//        String Uri = Constants.IMU6_PATH;
//        String strContract2 = "{\"Uri\": \"" + deviceSerial2 + Uri + "\"}";
//        mdsSubscription2 = Mds.builder().build(this).subscribe(Constants.URI_EVENTLISTENER, strContract2, new MdsNotificationListener() {
//
//            // センサ値を受け取るメソッド
//            @Override
//            public void onNotification(String data) {
//                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);
//                double time=System.currentTimeMillis()-first_time;
//                double x = imuModel.getBody().getArrayAcc()[0].getX();
//                double y = imuModel.getBody().getArrayAcc()[0].getY();
//                double z = imuModel.getBody().getArrayAcc()[0].getZ();
//                double gx = imuModel.getBody().getArrayAcc()[0].getX();
//                double gy = imuModel.getBody().getArrayAcc()[0].getY();
//                double gz = imuModel.getBody().getArrayAcc()[0].getZ();
//                Log.d(TAG,deviceSerial2+"\n経過時刻Time:"+ String.valueOf(time)
//                        +"\nsensorTime:"+String.valueOf(accResponse.body.timestamp)
//                        +"\nx:"+String.valueOf(x)
//                        +"\ny:"+ String.valueOf(y)
//                        +"\nz:"+String.valueOf(z));
////                sb.append("経過時刻:"+(double)Math.round((double)time/10)/100+"秒");
////                sb.append("\n");
////                sb.append("x:"+x);
////                sb.append("\n");
////                sb.append("y:"+y);
////                sb.append("\n");
////                sb.append("z:"+z);
//                //tvSensorInfo2.setText(sb.toString());
//                csvLogger2.appendHeader("Sensor time (ms),System time (ms),X (m/s^2),Y (m/s^2),Z (m/s^2)");
//                csvLogger2.appendLine(String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f", accResponse.body.timestamp, time/1000,x,y,z));
//            }
//
//            @Override
//            public void onError(MdsException error) {
//                Log.e(TAG, "subscription onError(): ", error);
//                unsubscribe2();
//                onDestroy();
//            }
//        });
//
//    }

    private void unsubscribe1() {
        if (mdsSubscription1 != null) {
            mdsSubscription1.unsubscribe();
            mdsSubscription1 = null;
        }
    }

    private void unsubscribe2() {
        if (mdsSubscription2 != null) {
            mdsSubscription2.unsubscribe();
            mdsSubscription2 = null;
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