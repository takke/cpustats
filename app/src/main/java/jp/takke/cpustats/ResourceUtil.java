package jp.takke.cpustats;


public class ResourceUtil {

    public static int getIconIdForCpuUsage(int[] cpuUsages) {
        
        if (cpuUsages.length == 0) {
            return R.drawable.single000;
        }
        
//        final int coreCount = MyUtil.calcCpuCoreCount();
//      MyLog.i("core:" + coreCount);
        final int coreCount = cpuUsages.length-1;

        switch (coreCount) {
        case 1:
            // シングルコア
            return getIconIdForCpuUsageSingle(cpuUsages[0]);
        case 2:
            // 2コア
            return getIconIdForCpuUsageDual(cpuUsages);
        case 3:
            // 3コア(6コアの2分割など)
            return QuadResourceUtil.getIconIdForCpuUsageTri(cpuUsages);
        default:
            // 4コア以上
            return QuadResourceUtil.getIconIdForCpuUsageQuad(cpuUsages);
        }
    }

    
    public static int getIconIdForCpuUsageSingle(int cpuUsage) {
        
        switch (ResourceUtil.cpuUsageToLevel5(cpuUsage)) {
        case 0: return R.drawable.single000;
        case 1: return R.drawable.single020;
        case 2: return R.drawable.single040;
        case 3: return R.drawable.single060;
        case 4: return R.drawable.single080;
        case 5: return R.drawable.single100;
        }
        return R.drawable.single000;
    }
    
    
    /**
     * CPU 使用率を「レベル」に変換する
     * 
     * @param cpuUsage CPU使用率[0,100]
     * @return レベル値[0,5]
     */
    public static int cpuUsageToLevel5(int cpuUsage) {
        
        if (cpuUsage < 5) {
            return 0;
        } else if (cpuUsage < 20) {
            return 1;
        } else if (cpuUsage < 40) {
            return 2;
        } else if (cpuUsage < 60) {
            return 3;
        } else if (cpuUsage < 80) {
            return 4;
        } else {
            return 5;
        }
    }

    private static int getIconIdForCpuUsageDual(int[] cpuUsages) {
        final int levelForCore1 = ResourceUtil.cpuUsageToLevel5(cpuUsages.length < 2 ? 0 : cpuUsages[1]);
        final int levelForCore2 = ResourceUtil.cpuUsageToLevel5(cpuUsages.length < 3 ? 0 : cpuUsages[2]);
        switch (levelForCore1) {
        case 0:
            switch (levelForCore2) {
            case 0: return R.drawable.dual_0_0;
            case 1: return R.drawable.dual_0_1;
            case 2: return R.drawable.dual_0_2;
            case 3: return R.drawable.dual_0_3;
            case 4: return R.drawable.dual_0_4;
            case 5: return R.drawable.dual_0_5;
            }
            break;
        
        case 1:
            switch (levelForCore2) {
            case 0: return R.drawable.dual_1_0;
            case 1: return R.drawable.dual_1_1;
            case 2: return R.drawable.dual_1_2;
            case 3: return R.drawable.dual_1_3;
            case 4: return R.drawable.dual_1_4;
            case 5: return R.drawable.dual_1_5;
            }
            break;
        
        case 2:
            switch (levelForCore2) {
            case 0: return R.drawable.dual_2_0;
            case 1: return R.drawable.dual_2_1;
            case 2: return R.drawable.dual_2_2;
            case 3: return R.drawable.dual_2_3;
            case 4: return R.drawable.dual_2_4;
            case 5: return R.drawable.dual_2_5;
            }
            break;
        
        case 3:
            switch (levelForCore2) {
            case 0: return R.drawable.dual_3_0;
            case 1: return R.drawable.dual_3_1;
            case 2: return R.drawable.dual_3_2;
            case 3: return R.drawable.dual_3_3;
            case 4: return R.drawable.dual_3_4;
            case 5: return R.drawable.dual_3_5;
            }
            break;
        
        case 4:
            switch (levelForCore2) {
            case 0: return R.drawable.dual_4_0;
            case 1: return R.drawable.dual_4_1;
            case 2: return R.drawable.dual_4_2;
            case 3: return R.drawable.dual_4_3;
            case 4: return R.drawable.dual_4_4;
            case 5: return R.drawable.dual_4_5;
            }
            break;
        
        case 5:
            switch (levelForCore2) {
            case 0: return R.drawable.dual_5_0;
            case 1: return R.drawable.dual_5_1;
            case 2: return R.drawable.dual_5_2;
            case 3: return R.drawable.dual_5_3;
            case 4: return R.drawable.dual_5_4;
            case 5: return R.drawable.dual_5_5;
            }
            break;
        }
        return R.drawable.dual_0_0;
    }


    /**
     * CPUクロック周波数のアイコンを取得する
     * 
     * @param currentFreq クロック周波数[KHz]
     * @return R.drawable.freq_01　～ R.drawable.freq_50 の値
     */
    public static int getIconIdForCpuFreq(int currentFreq) {
        
        // 下記のように変換する
        // 300MHz =>  3
        // 1.5GHz => 15
        final int freqAB = currentFreq / 1000 / 100;
        
        if (freqAB < 1) {
            return R.drawable.freq_01;
        }
        switch (freqAB) {
        case 1: return R.drawable.freq_01;
        case 2: return R.drawable.freq_02;
        case 3: return R.drawable.freq_03;
        case 4: return R.drawable.freq_04;
        case 5: return R.drawable.freq_05;
        case 6: return R.drawable.freq_06;
        case 7: return R.drawable.freq_07;
        case 8: return R.drawable.freq_08;
        case 9: return R.drawable.freq_09;
        case 10: return R.drawable.freq_10;
        case 11: return R.drawable.freq_11;
        case 12: return R.drawable.freq_12;
        case 13: return R.drawable.freq_13;
        case 14: return R.drawable.freq_14;
        case 15: return R.drawable.freq_15;
        case 16: return R.drawable.freq_16;
        case 17: return R.drawable.freq_17;
        case 18: return R.drawable.freq_18;
        case 19: return R.drawable.freq_19;
        case 20: return R.drawable.freq_20;
        case 21: return R.drawable.freq_21;
        case 22: return R.drawable.freq_22;
        case 23: return R.drawable.freq_23;
        case 24: return R.drawable.freq_24;
        case 25: return R.drawable.freq_25;
        case 26: return R.drawable.freq_26;
        case 27: return R.drawable.freq_27;
        case 28: return R.drawable.freq_28;
        case 29: return R.drawable.freq_29;
        case 30: return R.drawable.freq_30;
        case 31: return R.drawable.freq_31;
        case 32: return R.drawable.freq_32;
        case 33: return R.drawable.freq_33;
        case 34: return R.drawable.freq_34;
        case 35: return R.drawable.freq_35;
        case 36: return R.drawable.freq_36;
        case 37: return R.drawable.freq_37;
        case 38: return R.drawable.freq_38;
        case 39: return R.drawable.freq_39;
        case 40: return R.drawable.freq_40;
        case 41: return R.drawable.freq_41;
        case 42: return R.drawable.freq_42;
        case 43: return R.drawable.freq_43;
        case 44: return R.drawable.freq_44;
        case 45: return R.drawable.freq_45;
        case 46: return R.drawable.freq_46;
        case 47: return R.drawable.freq_47;
        case 48: return R.drawable.freq_48;
        case 49: return R.drawable.freq_49;
        case 50: return R.drawable.freq_50;
        default: return R.drawable.freq_50; // 5.0GHz over
        }
    }
    
}
