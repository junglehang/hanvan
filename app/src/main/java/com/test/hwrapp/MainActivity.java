package com.test.hwrapp;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.hanvon.HWCloudManager;

import org.json.JSONObject;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private SurfaceView surface;

    private TextView lineResult1;
    private TextView lineResult2;
    private TextView lineResult3;
    private TextView lineResult4;
    private TextView lineResult5;
    private TextView lineResult6;
    private TextView lineResult7;
    private TextView lineResult8;
    private TextView lineResult9;
    private TextView lineResult10;

    private Paint paint;
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Path path;

    private int top;
    private int bottom;
    private long now, init;
    private boolean isRunning = true, start = false;


    private HWCloudManager hwCloudManagerHandSingle;
    StringBuilder sbuilder = new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //remove title bar
        setContentView(R.layout.activity_main);
        /**
         * your_android_key 是您在开发者中心申请的android_key 并 申请了云手写单字识别的服务
         * 开发者中心：http://developer.hanvon.com/
         *
         */
        hwCloudManagerHandSingle = new HWCloudManager(this, "1ae8bde7-1b21-4348-a66d-8e0e90344707");
        surface = (SurfaceView) this.findViewById(R.id.surface);
        lineResult1 = (TextView) findViewById(R.id.line_result1);
        lineResult2 = (TextView) findViewById(R.id.line_result2);
        lineResult3 = (TextView) findViewById(R.id.line_result3);
        lineResult4 = (TextView) findViewById(R.id.line_result4);
        lineResult5 = (TextView) findViewById(R.id.line_result5);
        lineResult6 = (TextView) findViewById(R.id.line_result6);
        lineResult7 = (TextView) findViewById(R.id.line_result7);
        lineResult8 = (TextView) findViewById(R.id.line_result8);
        lineResult9 = (TextView) findViewById(R.id.line_result9);
        lineResult10 = (TextView) findViewById(R.id.line_result10);

        paint = new Paint();
        path = new Path();
        surfaceHolder = surface.getHolder();
        surface.setZOrderOnTop(true);
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);

        surfaceHolder.addCallback(this);

        int[] location = new int[2];
        surface.getLocationOnScreen(location);
        System.out.println(surface.getHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getY() >= top && event.getY() <= bottom) {
            start = true;
            init = now;
            now = System.currentTimeMillis();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (now - init >= 100 && now - init <= 1000) {
                        sbuilder.append("-1,").append("0,");
                    }
                    path.moveTo(event.getX(), event.getY() - top);

                    sbuilder.append((int) (event.getX())).append(",").append((int) (event.getY() - top)).append(",");
                    break;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(event.getX(), event.getY() - top);
                    sbuilder.append((int) (event.getX())).append(",").append((int) (event.getY() - top)).append(",");
                    break;

                default:
                    break;
            }

        }
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        int[] location = new int[2];
        surface.getLocationOnScreen(location);
        top = location[1];
        bottom = top + surface.getHeight();
        new Thread(wlineThread).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        System.out.println("-----------surface Destroyed-----------");
        isRunning = false;

    }

    Runnable wlineThread = new Runnable() {

        @Override
        public void run() {
            while (isRunning) {
                drawView();
                if (start) {
                    long temp = System.currentTimeMillis() - now;
                    if (temp > 1000) {
                        sbuilder.append("-1").append(",").append("0");

                        String content = hwCloudManagerHandSingle.handSingleLanguage("1", "chns", sbuilder.toString());

                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("content", content);
                        message.setData(bundle);
                        MainActivity.this.writeLineHandler.sendMessage(message);

                        start = false;

                        clearCanvas();
                        sbuilder = new StringBuilder();
                    }
                }
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };

    private void clearCanvas() {
        for (int i = 0; i < 4; i++) {
            try {
                if (surfaceHolder != null) {

                    canvas = surfaceHolder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    path.reset();

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null)
                    surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawView() {
        try {
            if (surfaceHolder != null) {
                canvas = surfaceHolder.lockCanvas();
                canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);

                canvas.drawPath(path, paint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null)
                surfaceHolder.unlockCanvasAndPost(canvas);
        }

    }

    Handler writeLineHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            String content = bundle.getString("content");
            showResult(content);
        };
    };

    private void showResult(String content) {
        JSONObject obj = null;
        try {
            System.out.println(content);
            obj = new JSONObject(content);
            if (obj != null) {
                if ("0".equals(obj.getString("code"))) {
                    String result = obj.getString("result");
                    if (null == result) {
                        Toast.makeText(getApplication(), "请重新出入", Toast.LENGTH_SHORT).show();
                    } else {
                        String[] words = result.split(",");
                        int len = words.length;
                        char[] wordsChar = new char[len];
                        int i = 0;
                        for (String word : words) {
                            if ("0".equals(word)) {
                                wordsChar[i++] = ' ';
                            } else {
                                wordsChar[i++] = (char) Integer.parseInt(word);
                            }
                        }
                        lineResult1.setText(String.valueOf(wordsChar[0]));
                        lineResult2.setText(String.valueOf(wordsChar[1]));
                        lineResult3.setText(String.valueOf(wordsChar[2]));
                        lineResult4.setText(String.valueOf(wordsChar[3]));
                        lineResult5.setText(String.valueOf(wordsChar[4]));
                        lineResult6.setText(String.valueOf(wordsChar[5]));
                        lineResult7.setText(String.valueOf(wordsChar[6]));
                        lineResult8.setText(String.valueOf(wordsChar[7]));
                        lineResult9.setText(String.valueOf(wordsChar[8]));
                        lineResult10.setText(String.valueOf(wordsChar[9]));
                    }
                } else {
                    Toast.makeText(getApplication(), obj.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isRunning = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        start = false;
    }
}
