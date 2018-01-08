package com.chaos.wechatjump;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import static android.R.attr.id;

public class JumpActivity extends AppCompatActivity {
    private Activity mActivity;
    private final String mLogTag = "AutoJump";
    private final String mPicturePath = "/sdcard/autojump.png";
    // permission
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    private static String[] PERMISSIONS_FLOAT = {
            "android.permission.SYSTEM_ALERT_WINDOW" };

    private class ExtentConfigItemSelectedListener implements Spinner.OnItemSelectedListener {
        private int mType;
        ExtentConfigItemSelectedListener(int type) {
            mType = type;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            JumpConfigSet.setConfigExt(i, mType);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission_sdcard = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            int permission_float = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.SYSTEM_ALERT_WINDOW");
            if (permission_sdcard != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, 1);
            }
            if (permission_float != PackageManager.PERMISSION_GRANTED) {
                // 没有悬浮窗权限
                ActivityCompat.requestPermissions(activity, PERMISSIONS_FLOAT, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        verifyStoragePermissions(this);

        Spinner mSpinner;
        final Spinner mExtendSpinner;
        final EditText mEditPress;

        setContentView(R.layout.activity_jump);

        View startBtn = findViewById(R.id.start_button);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity, JumpService.class);
                CheckBox checkBox = (CheckBox) findViewById(R.id.check_float);
                if (checkBox.isChecked())
                    intent.putExtra(JumpService.ACTION, JumpService.SHOW);
                startService(intent);
                moveTaskToBack(true);
            }
        });

        findViewById(R.id.end_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity, JumpService.class);
                stopService(intent);
            }
        });

        mSpinner = (Spinner) findViewById(R.id.spinner);
        mExtendSpinner = (Spinner) findViewById(R.id.spinner_ext);
        mEditPress = (EditText) findViewById(R.id.editText);
        mSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < JumpConfigSet.length()) {
                    mEditPress.setVisibility(View.GONE);
                    mExtendSpinner.setVisibility(View.GONE);
                    JumpConfigSet.setConfig(i);
                } else if (i >= JumpConfigSet.length() && i < JumpConfigSet.length() + 4) {
                    int type = i - JumpConfigSet.length();
                    mEditPress.setVisibility(View.GONE);
                    mExtendSpinner.setVisibility(View.VISIBLE);
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mActivity,
                            JumpConfigSet.sConfigsID[type], android.R.layout.simple_spinner_item);
                    mExtendSpinner.setAdapter(adapter);
                    mExtendSpinner.setOnItemSelectedListener(new ExtentConfigItemSelectedListener(type));
                } else {
                    mEditPress.setVisibility(View.VISIBLE);
                    mExtendSpinner.setVisibility(View.GONE);
                    mEditPress.setText(Float.toString(JumpConfigSet.getDefaultConfig().pressCoefficient));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mEditPress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    JumpConfigSet.setCustomConfig(Float.parseFloat(charSequence.toString()));
                } catch (Exception e) {
                    Log.e(mLogTag, e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        RootShellCmd cmd = new RootShellCmd();
        TextView info3 = (TextView) findViewById(R.id.info3_text);
        String testPath = "/sdcard/autojump.png";
        // check screenshot
        try {
            cmd.screenCapture(testPath);
            SystemClock.sleep(1500);

            Bitmap test = cmd.getBitmapFromPath(testPath);
            if (test == null || test.getWidth() == 0 || test.getHeight() == 0) {
                String info = "Not Support the device. Wait for a new version.";
                Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
                info3.setText(info);
                info3.setVisibility(View.VISIBLE);
                startBtn.setEnabled(false);
            }
        } catch (Exception e) {
            String info = "Not Support the device. Wait for a new version.";
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
            info3.setText(info);
            info3.setVisibility(View.VISIBLE);
            startBtn.setEnabled(false);
        }
        // check root
        try {
            cmd.checkRoot();
        } catch (Exception e) {
            String info = "Need to Root";
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
            info3.setText(info);
            info3.setVisibility(View.VISIBLE);
            startBtn.setEnabled(false);
        }
        // delete file
        File file = new File(testPath);
        if(file.isFile()){
            file.delete();
        }
        file.exists();
    }
}
