package com.example.doatest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.example.doatest.model.Location;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;

    private List<Ball>balls = new CopyOnWriteArrayList<>();

    Random random_theta = new Random();

    int itNum = 0;
    int direction = 270;
    int volume = 0;
    int freq = 0;

    int colorIdx = 0;

    ///////////////////////////////////////////////////////////////
    private List<Color> colors = new ArrayList<Color>() {{
        add(new Color(214, 41, 52));
        add(new Color(214, 89, 46));
        add(new Color(214, 150, 56));
        add(new Color(214, 192, 62));
        add(new Color(207, 214, 64));
        add(new Color(143, 214, 68));
        add(new Color(50, 214, 114));
        add(new Color(62, 214, 192));
        add(new Color(63, 200, 214));
        add(new Color(57, 155, 214));
        add(new Color(118, 66, 214));
        add(new Color(214, 48, 214));
    }};

    int CALC_RATE = 10;         // 몇 frame 마다 새로운 ball 정보를 받아 올 것인가
    int MIN_VOLUME = 200;       // 어느 볼륨 미만은 무시 할 것인가
    int MAX_VOLUME = 2000;      // 어느 볼륨 이상은 무시 할 것인가
    int MIN_BALL_SIZE = 10;     // 최소 원 사이즈
    int BALL_SIZE = 40;         // 최대 원 사이즈 => MIN_BALL_SIZE + (현재 볼륨 / MAX_VOLUME) * BALL_SIZE
    int STEP = 10;              // 몇 번 움직이고 끝날 것인가
    int RADIUS = 400;           // 공이 중심부터 움직일 반경
    double SPEED = 0.3;         // 공이 움직이는 속도
    double ALPHA = 0.6;

    // ps : 이동 속도 = (radius / STEP) * SPEED

    ///////////////////////////////////////////////////////////////


    private CameraBridgeViewBase mOpenCvCameraView;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matResult = inputFrame.rgba();

        if (itNum % CALC_RATE == 0) {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://13.209.217.37/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            JsonPlaceholderApi server = retrofit.create(JsonPlaceholderApi.class);

            Call<List<Location>> temp = server.get_data();

            temp.enqueue(new Callback<List<Location>>() {
                @Override
                public void onResponse(Call<List<Location>> call, Response<List<Location>> response) {
                    List<Location> locs = response.body();

                    for (Location loc : locs) {
                        direction = loc.location;
                        volume = loc.volume;
                        freq = loc.freq;

                        if (freq > 1200) colorIdx = 11;
                        else colorIdx = freq / 100;

                        Log.d("DATA", String.format("%d %d", loc.location, volume));


                        if (0 <= direction && direction <= 90) direction = (-1) * direction;
                        else if (90 < direction && direction < 270) continue;
                        else if (direction <= 360) direction = (-1) * (direction - 360);

                        if (volume > MAX_VOLUME || volume < MIN_VOLUME) continue;
                        volume = (int) ((volume / (double) MAX_VOLUME) * BALL_SIZE) + MIN_BALL_SIZE;

                        int x = (int) (matResult.width() / 2 + (((double) direction / 90.0) * (matResult.width() / 2)));
                        int y = matResult.height() / 2;

                        Color new_color = colors.get(colorIdx);

                        Ball new_ball = new Ball(x, y, RADIUS, random_theta.nextInt(360), new_color.r, new_color.g, new_color.b, volume, STEP, SPEED);
                        if (Math.abs(new_ball.dx) < 4 || Math.abs(new_ball.dy) < 4)
                            continue;

                        Log.d("DATA", String.format("%d %d", loc.location, volume));
                        Log.d("API", String.format("%d %d %d", direction, (int) ((Math.abs(direction) / 90.0) * (matResult.width() / 2)), y));
                        Log.d("BALL SIZE", String.format("%d %d", new_ball.dx, new_ball.dy));

                        balls.add(0, new_ball);
                    }
                }

                @Override
                public void onFailure(Call<List<Location>> call, Throwable t) {
                    Log.d("API", t.getMessage());
                }
            });
        }

        matInput = matResult.clone();
        for (Ball ball: balls) {
            Log.d("API", String.format("%d %d %d", ball.x, ball.y, ball.size));
            Imgproc.circle(matResult, new Point(ball.x,ball.y), ball.size, new Scalar(ball.r, ball.g, ball.b), -1);
            ball.move();
            if (ball.delete()) {
                balls.remove(ball);
            }
        }

        Core.addWeighted(matResult, ALPHA, matInput, (1 - ALPHA), 0, matResult);

        itNum = itNum + 1;
        return matResult;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


}
