package com.chaos.wechatjump;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by zhouchao on 18-1-5.
 */

public class FloatingView extends FrameLayout implements JumpService.JumpObserver {
    private class FloatingManager {
        private WindowManager mWindowManager;
        private Context mContext;

        private FloatingManager(Context context) {
            mContext = context;
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);//获得WindowManager对象
        }

        /**
         * 添加悬浮窗
         * @param view
         * @param params
         * @return
         */
        protected boolean addView(View view, WindowManager.LayoutParams params) {
            try {
                mWindowManager.addView(view, params);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * 移除悬浮窗
         * @param view
         * @return
         */
        protected boolean removeView(View view) {
            try {
                mWindowManager.removeView(view);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * 更新悬浮窗参数
         * @param view
         * @param params
         * @return
         */
        protected boolean updateView(View view, WindowManager.LayoutParams params) {
            try {
                mWindowManager.updateViewLayout(view, params);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private final String mLogTag = "AutoJump";
    private Context mContext;
    private View mView;
    private ImageView mImageStart;
    private ImageView mImageStop;
    private TextView mInfoText;
    private int mTouchStartX, mTouchStartY;//手指按下时坐标
    private int mTouchBeginX, mTouchBeginY;
    private WindowManager.LayoutParams mParams;
    private FloatingManager mWindowManager;
    private JumpService mService;
    private float mScale;
    private boolean mPause;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == JumpService.SHOWINFO) {
                mInfoText.setText((String) msg.obj);
            }
        }
    };

    public FloatingView(JumpService service) {
        super(service);
        mService = service;
        mContext = service.getApplicationContext();
        LayoutInflater mLayoutInflater = LayoutInflater.from(service);
        mWindowManager = new FloatingManager(service);
        mView = mLayoutInflater.inflate(R.layout.floating_view, null);
        mImageStart = (ImageView) mView.findViewById(R.id.imagestart);
        mImageStart.setOnTouchListener(mOnTouchListener);
        mImageStart.setImageResource(R.drawable.pause);
        mImageStop = (ImageView) mView.findViewById(R.id.imagestop);
        mImageStop.setOnTouchListener(mOnTouchListener);
        mImageStop.setImageResource(R.drawable.stop);
        mInfoText = (TextView) mView.findViewById(R.id.dis_info);
        mScale = mContext.getResources().getDisplayMetrics().density;
        mPause = false;
    }

    @Override
    public void setDisplayInfo(String text) {
        Message msg = new Message();
        msg.what = JumpService.SHOWINFO;
        msg.obj = text;
        mHandler.sendMessage(msg);
    }

    public void show() {
        mParams = new WindowManager.LayoutParams();
        mParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        mParams.x = 0;
        mParams.y = 0;
        //总是出现在应用程序窗口之上
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        }
        //设置图片格式，效果为背景透明
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowManager.addView(mView, mParams);
    }

    public void hide() {
        mWindowManager.removeView(mView);
    }

    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            int deltaPressX = (int) event.getRawX() - mTouchBeginX;
            int deltaPressY = (int) event.getRawY() - mTouchBeginY;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    mTouchBeginX = mTouchStartX;
                    mTouchBeginY = mTouchStartY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(deltaPressX) + Math.abs(deltaPressY) < 12)
                        break;
                    int deltaX = (int) event.getRawX() - mTouchStartX;
                    mParams.x += -deltaX;
//                    mParams.y += mTouchStartY - (int) event.getRawY();//相对于屏幕右下角的位置
                    mWindowManager.updateView(mView, mParams);
                    mTouchStartX = (int) event.getRawX();
                    mTouchStartY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(deltaPressX) + Math.abs(deltaPressY) >= 12)
                        break;
                    if (view == mImageStart) {
                        if (mPause) {
                            mImageStart.setImageResource(R.drawable.pause);
                            mService.start();
                            mPause = false;
                        } else {
                            mImageStart.setImageResource(R.drawable.start);
                            mService.pause();
                            mPause = true;
                        }
                    } else if (view == mImageStop){
                        hide();
                        Intent intent = new Intent(mService, JumpService.class);
                        mService.stopService(intent);
                    }
                    break;
            }
            return true;
        }
    };
}
