package com.chaos.wechatjump;

import android.graphics.PointF;

/**
 * Created by zhouchao on 18-1-2.
 */

public class JumpConfigSet {
    public static class JumpConfig {
        public int underGameScoreY = 0;
        public float pressCoefficient = 0;
        public int pieceBaseHeight12 = 0;
        public int pieceBodyWidth = 0;
        public PointF swipe;

        public JumpConfig(int a, float b, int c, int d) {
            underGameScoreY = a;
            pressCoefficient = b;
            pieceBaseHeight12 = c;
            pieceBodyWidth = d;
        }
    };


    private static JumpConfig sConfig1920 = new JumpConfig(300, 1.392f, 20, 70);
    private static JumpConfig sConfig2560 = new JumpConfig(410, 1.475f, 28, 110);
    private static JumpConfig sConfig2160 = new JumpConfig(420, 1.372f, 25, 85);
    private static JumpConfig sConfig1440 = new JumpConfig(200, 2.099f, 13, 47);
    private static JumpConfig sConfig1280 = new JumpConfig(200, 2.099f, 13, 47);
    private static JumpConfig sConfig960 = new JumpConfig(300, 2.732f, 20, 70);

    private static JumpConfig sConfigHonorNote8 = new JumpConfig(400, 1.04f, 90, 120);

    private static JumpConfig sConfigXiaoMiMax2 = new JumpConfig(300, 1.5f, 20, 70);
    private static JumpConfig sConfigXiaoMiMi5 = new JumpConfig(300, 1.475f, 20, 70);
    private static JumpConfig sConfigXiaoMiMi5S = new JumpConfig(300, 1.475f, 20, 70);
    private static JumpConfig sConfigXiaoMiMi5X = new JumpConfig(300, 1.45f, 25, 80);
    private static JumpConfig sConfigXiaoMiMi6 = new JumpConfig(300, 1.44f, 20, 70);
    private static JumpConfig sConfigXiaoMiNote2 = new JumpConfig(300, 1.47f, 25, 80);

    private static JumpConfig sConfigSamsungS7Edge = new JumpConfig(384, 1.0f, 95, 102);
    private static JumpConfig sConfigSamsungS8 = new JumpConfig(460, 1.365f, 70, 75);

    private static JumpConfig sConfigSmartisanPro2 = new JumpConfig(411, 1.392f, 20, 70);

    private static JumpConfig[] sConfigs = {sConfig1920, sConfig2560, sConfig2160, sConfig1440, sConfig1280, sConfig960};
    private static JumpConfig[] sConfigsHuawei = {sConfigHonorNote8};
    private static JumpConfig[] sConfigsXiaomi = {sConfigXiaoMiMax2, sConfigXiaoMiMi5, sConfigXiaoMiMi5S, sConfigXiaoMiMi5X,
            sConfigXiaoMiMi6, sConfigXiaoMiNote2};
    private static JumpConfig[] sConfigsSamsung = {sConfigSamsungS7Edge, sConfigSamsungS8};
    private static JumpConfig[] sConfigsSmartisan = {sConfigSmartisanPro2};

    private static JumpConfig[][] sConfigsExt = {sConfigsHuawei, sConfigsXiaomi, sConfigsSamsung, sConfigsSmartisan};
    public static int[] sConfigsID = {R.array.confighuawei, R.array.configxiaomi, R.array.configsamsung, R.array.configsmartisan};

    private static JumpConfig sConfig = sConfig1920;

    public static JumpConfig getConfig(){
        return sConfig;
    }

    public static JumpConfig getDefaultConfig(){
        return sConfig1920;
    }

    public static void setConfig(int pos) {
        if (pos < 0 || pos >= sConfigs.length) {
            sConfig = sConfig1920;
        }
        sConfig = sConfigs[pos];
    }

    public static void setConfigExt(int pos, int type) {
        if (type < 0 || type >= sConfigsExt.length)
            return;

        JumpConfig[] configs = sConfigsExt[type];
        if (pos < 0 || pos >= configs.length) {
            return;
        }
        sConfig = configs[pos];
    }

    public static void setCustomConfig(float pressTime) {
        sConfig = new JumpConfig(300, pressTime, 20, 70);
    }

    public static int length() {
        return sConfigs.length;
    }
};
