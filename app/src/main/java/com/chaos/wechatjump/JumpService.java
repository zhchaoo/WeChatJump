package com.chaos.wechatjump;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by zhouchao on 18-1-2.
 */

public class JumpService extends Service {
    public static final String ACTION="action";
    public static final String SHOW="show";
    public static final String HIDE="hide";
    public static final int SHOWINFO = 101;

    private FloatingView mFloatingView;

    private RootShellCmd mCmd;
    private final String mPicturePath = "/sdcard/autojump.png";
    private final String mLogTag = "AutoJump";
    private int mScreenWidth;
    private int mScreenHeight;
    private PointSet mSwipePos;
    private JumpConfigSet.JumpConfig mConfig;
    private Thread mTask;
    private JumpRunnable mRun;

    private class PointSet {
        private PointF mStart;
        private PointF mEnd;
        public PointSet(PointF start, PointF end) {
            mStart = start;
            mEnd = end;
        }

        PointF start() {
            return mStart;
        }

        PointF end() {
            return mEnd;
        }
    };

    public interface JumpObserver {
        void setDisplayInfo(String text);
    }

    private class JumpRunnable implements Runnable {
        private boolean mPause = false;
        private boolean mStop = false;
        private JumpObserver mObserver;

        void addObserver(JumpObserver observer) {
            mObserver = observer;
        }

        void resume() {
            mPause = false;
        }

        void pause() {
            mPause = true;
        }

        void stop() { mStop = true; }

        @Override
        public void run() {
            SystemClock.sleep(8000);
            while (!mStop) {
                mCmd.screenCapture(mPicturePath);
                SystemClock.sleep(2000);

                Bitmap image = mCmd.getBitmapFromPath(mPicturePath);
                if (image == null) {
                    // wait againt.
                    SystemClock.sleep(4000);
                    image = mCmd.getBitmapFromPath(mPicturePath);
                }
                if (image == null) {
                    Log.d(mLogTag, "image is null");
                    continue;
                }
                setButtonPosition(image);
                PointSet points = findPieceAndBoard(image);
                double distance = Math.sqrt((points.end().x - points.start().x) * (points.end().x - points.start().x) +
                                (points.end().y - points.start().y) * (points.end().y - points.start().y));
                if (mPause || mStop) {
                    SystemClock.sleep(1000);
                    continue;
                }
                int pressTime = jump(distance);
                mObserver.setDisplayInfo(String.valueOf(pressTime));
                // wait for next jump
                SystemClock.sleep(2000 + Math.max(pressTime, 400) + (int) (Math.random() * 200));
                Log.d(mLogTag, "point dist:" + distance + " press:" + pressTime + " start:(" + points.start().x + "," + points.start().y
                        + ") end(" + points.end().x + "," + points.end().y + ")");
            }
        }
    };


    private void setButtonPosition(Bitmap image) {
        // 将swipe设置为 `再来一局` 按钮的位置
        int width = image.getWidth();
        int height = image.getHeight();
        int left = width / 2;
        int top = (int) (1584 * (height / 1920.0));
        left = (int) (left - 50 + Math.random() * 100);
        top = (int) (top - 50 + Math.random() * 100);
        mSwipePos = new PointSet(new PointF(left, top), new PointF(left, top + 100));
    }

    private int jump(double distance) {
        int pressTime = (int) (distance * mConfig.pressCoefficient);
        pressTime = Math.max(pressTime, 200);   // 设置 200 ms 是最小的按压时间
        mCmd.simulateJump(mSwipePos.start(), mSwipePos.end(), pressTime);
        return pressTime;
    }

    private PointSet findPieceAndBoard(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int lastPixel = 0;
        int pieceXSum = 0;
        int pieceXC = 0;
        int pieceYMax = 0;
        int boardX = 0;
        int boardY = 0;
        int boardXStart = 0;
        int boardXEnd = 0;
        int boardXSum = 0;
        int boardXC = 0;
        int scanXBorder = width / 8;  // 扫描棋子时的左右边界
        int scanStartY = 0;  // 扫描的起始y坐标

        int[] pixels = new int[width * height];
        image.getPixels(pixels, 0, width, 0, 0, width, height);

        // 以50px步长，尝试探测scan_start_y
        for (int i = height / 3; i < height * 2 / 3; i += 50) {
//            lastPixel = image.getPixel(0, i);
            lastPixel = pixels[0 + i * width];
            for (int j = 1; j < width; j++) {
//                int pixel = image.getPixel(j,i);
                int pixel = pixels[j + i * width];
                // 不是纯色的线，则记录scan_start_y的值，准备跳出循环
                if (lastPixel != pixel) {
                    scanStartY = i - 50;
                    break;
                }
                if (scanStartY > 0)
                    break;
            }
        }
        Log.d(mLogTag, "scanStartY: " + scanStartY);

        // 从scan_start_y开始往下扫描，棋子应位于屏幕上半部分，这里暂定不超过2/3
        for (int i = scanStartY; i < height * 2 / 3; i++) {
            for (int j = scanXBorder; j < width - scanXBorder; j++) {
//                int pixel = image.getPixel(j,i);
                int pixel = pixels[j + i * width];
                // 根据棋子的最低行的颜色判断，找最后一行那些点的平均值，这个颜色这样应该 OK，暂时不提出来
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                if (red > 50 && red < 60 && green > 53 && green < 63 && blue > 95 && blue < 110) {
                    pieceXSum += j;
                    pieceXC += 1;
                    pieceYMax = i > pieceYMax ? i : pieceYMax;
                }
            }
        }

        if (pieceXSum <= 0 || pieceXC <= 0){
            return new PointSet(new PointF(0, 0), new PointF(0, 0));
        }
        int pieceX = pieceXSum / pieceXC;
        int pieceY = pieceYMax - mConfig.pieceBaseHeight12;  // 上移棋子底盘高度的一半

        // 限制棋盘扫描的横坐标，避免音符bug
        if (pieceX < width / 2) {
            boardXStart = pieceX;
            boardXEnd = width;
        } else {
            boardXStart = 0;
            boardXEnd = pieceX;
        }

        int i;
        for (i = height / 3; i < height * 2 / 3; i++) {
//            lastPixel = image.getPixel(0, i);
            lastPixel = pixels[0 + i * width];
            int last_red = Color.red(lastPixel);
            int last_green = Color.green(lastPixel);
            int last_blue = Color.blue(lastPixel);
            if (boardX > 0 || boardY > 0)
                break;
            boardXSum = 0;
            boardXC = 0;

            for (int j = boardXStart; j < boardXEnd; j++) {
//                int pixel = image.getPixel(j, i);
                int pixel = pixels[j + i * width];
                // 修掉脑袋比下一个小格子还高的情况的 bug
                if (Math.abs(j - pieceX) < mConfig.pieceBodyWidth)
                    continue;
                // 修掉圆顶的时候一条线导致的小 bug，这个颜色判断应该 OK，暂时不提出来
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                if (Math.abs(red - last_red) + Math.abs(green - last_green) + Math.abs(blue - last_blue) > 10) {
                    boardXSum += j;
                    boardXC += 1;
                }
            }

            for (int j = boardXStart; j < boardXEnd; j++) {
                if (boardXSum > 0)
                    boardX = boardXSum / boardXC;
            }
        }
//        lastPixel = image.getPixel(boardX, i);
        lastPixel = pixels[boardX + i * width];
        int last_red = Color.red(lastPixel);
        int last_green = Color.green(lastPixel);
        int last_blue = Color.blue(lastPixel);

        // 从上顶点往下+274的位置开始向上找颜色与上顶点一样的点，为下顶点
        // 该方法对所有纯色平面和部分非纯色平面有效，对高尔夫草坪面、木纹桌面、药瓶和非菱形的碟机（好像是）会判断错误
        int k;
        for (k = i+274; k > i; k--) { // 274取开局时最大的方块的上下顶点距离
//            int pixel = image.getPixel(boardX, k);
            int pixel = pixels[boardX + k * width];
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);
            if (Math.abs(red - last_red) + Math.abs(green - last_green) + Math.abs(blue - last_blue) < 10)
                break;
        }
        boardY = ((i+k) / 2);

        // 如果上一跳命中中间，则下个目标中心会出现r245 g245 b245的点，利用这个属性弥补上一段代码可能存在的判断错误
        // 若上一跳由于某种原因没有跳到正中间，而下一跳恰好有无法正确识别花纹，则有可能游戏失败，由于花纹面积通常比较大，失败概率较低
        for (int l = i; l < i+200; l++) {
//            int pixel = image.getPixel(boardX, l);
            int pixel = pixels[boardX + l * width];
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);
            if (Math.abs(red - 245) + Math.abs(green - 245) + Math.abs(blue - 245) == 0) {
                boardY = l + 10;
                break;
            }
        }

        if (boardX <= 0 || boardY <=0)
            return new PointSet(new PointF(0, 0), new PointF(0, 0));

        return new PointSet(new PointF(pieceX, pieceY), new PointF(boardX, boardY));
    }

    public void start() {
        // start
        if (mTask == null) {
            mRun = new JumpRunnable();
            mRun.addObserver(mFloatingView);
            mTask = new Thread(mRun);
            mTask.start();
        // resume
        } else if (mRun != null){
            mRun.resume();
        }
    }

    public void stop() {
        // stop
        mRun.stop();
        mTask = null;
        mRun = null;
    }

    public void pause() {
        mRun.pause();
    }

    @Override
    public void onCreate() {
        mCmd = new RootShellCmd();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
        mConfig = JumpConfigSet.getConfig();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mFloatingView = new FloatingView(this);
        if (intent != null) {
            String action=intent.getStringExtra(ACTION);
            if(SHOW.equals(action)){
                mFloatingView.show();
            }else if(HIDE.equals(action)){
                mFloatingView.hide();
            }
        }

        start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mFloatingView != null) {
            mFloatingView = null;
        }
        stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mCmd = new RootShellCmd();

        return null;
    }
}
