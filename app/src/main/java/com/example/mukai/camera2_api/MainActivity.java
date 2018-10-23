package com.example.mukai.camera2_api;

//import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static java.lang.Math.abs;


//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends Activity implements SensorEventListener {

    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private Handler mBackgroundHandler = new Handler();
    private CameraCaptureSession mCaptureSession = null;

    private String filePath;
    private TextView textView_X;
    private TextView TimerText;

    private SimpleDateFormat TimerFormat = new SimpleDateFormat("mm:ss.SSS", Locale.US);
    //  10sec = 10 * 1000 = 10000msec
    final long countNumber = 10000;
    //  interval 10msec
    final long interval = 10;
    final CountDown countDown = new CountDown(countNumber, interval);

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    //  Sensor
    private SensorManager sensorManager;

    //@TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView_X = findViewById(R.id.text_view_X);
        TimerText = findViewById(R.id.Timer_text);
        TimerText.setText(TimerFormat.format(0));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);



        mTextureView = (TextureView) findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // 先ほどのカメラを開く部分をメソッド化した
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        Button capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                countDown.start();

            }
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String selectedCameraId = "";
        try {

            selectedCameraId = manager.getCameraIdList()[0];

            // https://github.com/googlesamples/android-Camera2Basic/blob/5dad16c103715b5e7e3c001cc5f6067f8d23f29e/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java#L499
            // あたりにあるのですが、顔用カメラを使いたくないなどがあれば、CameraCharacteristicsを経由して確認可能
            //            CameraCharacteristics characteristics
            //                    = manager.getCameraCharacteristics(selectedCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            manager.openCamera(selectedCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }

    };

    private void createCameraPreviewSession() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(3492, 4656); // 自分の手元のデバイスで決めうちしてます
        Surface surface = new Surface(texture);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequest = mPreviewRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    // カメラがcloseされている場合
                    if (null == mCameraDevice) {
                        return;
                    }

                    mCaptureSession = session;

                    try {
                        session.setRepeatingRequest(mPreviewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //  画像保存
    public void saveBitmap() {

        try {
            mCaptureSession.stopRepeating(); // プレビューの更新を止める
            if(mTextureView.isAvailable()) {

                filePath = getExternalFilesDir(null).getPath();
                File file = new File(filePath);
                Date mDate = new Date();
                SimpleDateFormat filename = new SimpleDateFormat("yyyyMMddHHmmss");

                FileOutputStream fos = null;
                fos = new FileOutputStream(new File(file,filename.format(mDate) + ".jpg"));
                Bitmap bitmap = mTextureView.getBitmap();
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);

                fos.close();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  save index
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    }

    class CountDown extends CountDownTimer{

        CountDown(long millisInFuture, long countDownInterval){
            super(millisInFuture,countDownInterval);
        }



        //  完了時呼ばれる
        @Override
        public void onFinish(){

            TimerText.setText(TimerFormat.format(0));
            saveBitmap();

        }

        //  インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished){

            TimerText.setText(TimerFormat.format(millisUntilFinished));

        }
    }

    //  センサー停止
    protected void stop_sensor(){
        sensorManager.unregisterListener(this);
        countDown.start();
    }

    //  解除コード
    @Override
    protected void onResume(){
        super.onResume();
        //  Listenerの登録
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor gyro_uc = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);

        if (gyro != null){
            sensorManager.registerListener(this,gyro,SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this,gyro_uc,SensorManager.SENSOR_DELAY_UI);
        }
        else{
            String ns = "No Support!";
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        Log.d("debug","onSensorChanged");

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            float sensorX = event.values[0];
            float sensorY = event.values[1];
            float sensorZ = event.values[2];

            //  X軸
//            if (abs(sensorX) > 2.0){
//                textView_X.setTextColor(Color.RED);
//            }
//            else textView_X.setTextColor(Color.BLUE);
//
//            String strTmp_X = String.format(Locale.US,"Gyroscope[rad/s]\n"
//                    + " X: " + sensorX);
//
//            textView_X.setText(strTmp_X);

//            if (abs(sensorX) > 2.0 && abs(sensorY) > 2.0 && abs(sensorZ) > 2.0){
//                stop_sensor();
//            }

            if (abs(sensorX) < 0.5 && abs(sensorY) < 0.5 && abs(sensorZ) < 0.5){
                stop_sensor();
            }



        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }
}