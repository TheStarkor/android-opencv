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
import android.widget.TextView;

import com.example.doatest.model.Location;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private Mat retMat;
    private List<Ball>balls = new CopyOnWriteArrayList<>();
    private List<Color> colors = new ArrayList<Color>() {{
        add(new Color(231, 51, 42));
        add(new Color(236, 141, 0));
        add(new Color(248, 228, 0));
        add(new Color(52, 174, 73));
        add(new Color(70, 186, 172));
        add(new Color(50, 140, 204));
        add(new Color(147, 84, 158));
    }};

    Random random_r = new Random();
    Random random_g = new Random();
    Random random_b = new Random();
    Random random_size = new Random();
    Random random_alpha = new Random();

    int iter_num = 0;
    int direction = 270;
    int volume = 0;
    int size = 10;

    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView text;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult, Ball balls);


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

        if (iter_num % 7 == 0) {

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
                        if (volume > 2000 && volume < 300) continue;
                        if (volume > 200 && volume < 1000) volume = (int) ((volume / 2000.0) * 60);
                        else volume = (int) ((volume / 2000.0) * 40);

//                        size = loc.size;
                        Log.d("DATA", String.format("%d %d", loc.location, volume));
////                        else if (direction >= 270 && direction <= 360) direction = direction - 360;
////                        if (direction > 90 && direction < 180) continue;
////                        else if (direction >= 0 && direction <= 90) direction = direction;
////                        int x = 1000 + ((-1) * (direction - 0) * (1000/90));
////                        int y = 400 - ((-1) * ((Math.abs(direction -);
//                        if (direction > 180 && direction < 270) contin 0) * (400/90))));

                        if (0 <= direction && direction <= 90) direction = (-1) * direction;
                        else if (90 < direction && direction < 270) continue;
                        else if (direction <= 360) direction = (-1) * (direction - 360);

                        int x = (int) (matResult.width() / 2 + (((double) direction / 90.0) * (matResult.width() / 2)));
                        int y = matResult.height() / 2;

                        Log.d("API", String.format("%d %d %d", direction, (int) ((double) (Math.abs(direction) / 90.0) * (matResult.width() / 2)), y));

                        Color new_color = colors.get(random_r.nextInt(colors.size()));

                        Ball new_ball = new Ball(x, y, 400, random_alpha.nextInt(360), new_color.r, new_color.g, new_color.b, volume, 10, (float) 0.3);
                        if (Math.abs(new_ball.dx) < 4 || Math.abs(new_ball.dy) < 4)
                            continue;
                        Log.d("API", String.format("%d %d", new_ball.dx, new_ball.dy));

                        balls.add(0, new_ball);
                    }
                }

                @Override
                public void onFailure(Call<List<Location>> call, Throwable t) {
                    Log.d("API", t.getMessage());
                }
            });
        }

        Mat temp = new Mat(matResult.height(), matResult.width(), CvType.CV_64FC1);
        matInput = matResult.clone();
//        matResult.setTo(new Scalar(255, 255, 255));
//        retMat = inputFrame.rgba();
//
        for (Ball ball: balls) {
//            Imgproc.circle(matResult, new Point(ball.x,ball.y), random_size.nextInt(100), new Scalar(ball.r, ball.g, ball.b, 0.1), -1);
            Imgproc.circle(matResult, new Point(ball.x,ball.y), ball.size, new Scalar(ball.r, ball.g, ball.b, 200), -1);
            ball.move();
            if (ball.delete()) {
                balls.remove(ball);
            }
        }

        Core.addWeighted(matResult, 0.6, matInput, 0.4, 0, matResult);

        iter_num = iter_num + 1;
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
