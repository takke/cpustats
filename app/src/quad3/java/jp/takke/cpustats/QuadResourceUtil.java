package jp.takke.cpustats;

public class QuadResourceUtil {

    public static int getIconIdForCpuUsageTri(int[] cpuUsages) {

        // 3コア
        return getIconIdForCpuUsageTriLevel3(cpuUsages);
    }


    public static int getIconIdForCpuUsageQuad(int[] cpuUsages) {
        
        // 4コア
        return getIconIdForCpuUsageQuadLevel3(cpuUsages);
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

    
    /**
     * CPU 使用率を「レベル」に変換する
     * 
     * @param cpuUsage CPU使用率[0,100]
     * @return レベル値[0,1,3,5]
     */
    private static int cpuUsageToLevel3(int cpuUsage) {
    
        // 0, 1, 3, 5
        if (cpuUsage < 5) {
            return 0;
        } else if (cpuUsage < 33) {
            return 1;
        } else if (cpuUsage < 66) {
            return 3;
        } else {
            return 5;
        }
    }

    
    private static int getIconIdForCpuUsageQuadLevel3(int[] cpuUsages) {
        
        // リソース節約のため 0, 1, 3, 5 のみとする
        int digit = 0;
        switch (cpuUsageToLevel3(cpuUsages.length < 2 ? 0 : cpuUsages[1])) {
        case 0: digit += 1000*0;    break;
        case 1: digit += 1000*1;    break;
        case 2: digit += 1000*1;    break;
        case 3: digit += 1000*3;    break;
        case 4: digit += 1000*3;    break;
        case 5: digit += 1000*5;    break;
        }
        switch (cpuUsageToLevel3(cpuUsages.length < 3 ? 0 : cpuUsages[2])) {
        case 0: digit += 100*0; break;
        case 1: digit += 100*1; break;
        case 2: digit += 100*1; break;
        case 3: digit += 100*3; break;
        case 4: digit += 100*3; break;
        case 5: digit += 100*5; break;
        }
        switch (cpuUsageToLevel3(cpuUsages.length < 4 ? 0 : cpuUsages[3])) {
        case 0: digit += 10*0;  break;
        case 1: digit += 10*1;  break;
        case 2: digit += 10*1;  break;
        case 3: digit += 10*3;  break;
        case 4: digit += 10*3;  break;
        case 5: digit += 10*5;  break;
        }
        switch (cpuUsageToLevel3(cpuUsages.length < 5 ? 0 : cpuUsages[4])) {
        case 0: digit += 1*0;   break;
        case 1: digit += 1*1;   break;
        case 2: digit += 1*1;   break;
        case 3: digit += 1*3;   break;
        case 4: digit += 1*3;   break;
        case 5: digit += 1*5;   break;
        }
        
//      MyLog.i("digit:" + digit);
        
        // for Level3
        switch (digit) {
        case 0: return R.drawable.quad_0000;
        case 1: return R.drawable.quad_0001;
        case 3: return R.drawable.quad_0003;
        case 5: return R.drawable.quad_0005;
        case 10: return R.drawable.quad_0010;
        case 11: return R.drawable.quad_0011;
        case 13: return R.drawable.quad_0013;
        case 15: return R.drawable.quad_0015;
        case 30: return R.drawable.quad_0030;
        case 31: return R.drawable.quad_0031;
        case 33: return R.drawable.quad_0033;
        case 35: return R.drawable.quad_0035;
        case 50: return R.drawable.quad_0050;
        case 51: return R.drawable.quad_0051;
        case 53: return R.drawable.quad_0053;
        case 55: return R.drawable.quad_0055;
        case 100: return R.drawable.quad_0100;
        case 101: return R.drawable.quad_0101;
        case 103: return R.drawable.quad_0103;
        case 105: return R.drawable.quad_0105;
        case 110: return R.drawable.quad_0110;
        case 111: return R.drawable.quad_0111;
        case 113: return R.drawable.quad_0113;
        case 115: return R.drawable.quad_0115;
        case 130: return R.drawable.quad_0130;
        case 131: return R.drawable.quad_0131;
        case 133: return R.drawable.quad_0133;
        case 135: return R.drawable.quad_0135;
        case 150: return R.drawable.quad_0150;
        case 151: return R.drawable.quad_0151;
        case 153: return R.drawable.quad_0153;
        case 155: return R.drawable.quad_0155;
        case 300: return R.drawable.quad_0300;
        case 301: return R.drawable.quad_0301;
        case 303: return R.drawable.quad_0303;
        case 305: return R.drawable.quad_0305;
        case 310: return R.drawable.quad_0310;
        case 311: return R.drawable.quad_0311;
        case 313: return R.drawable.quad_0313;
        case 315: return R.drawable.quad_0315;
        case 330: return R.drawable.quad_0330;
        case 331: return R.drawable.quad_0331;
        case 333: return R.drawable.quad_0333;
        case 335: return R.drawable.quad_0335;
        case 350: return R.drawable.quad_0350;
        case 351: return R.drawable.quad_0351;
        case 353: return R.drawable.quad_0353;
        case 355: return R.drawable.quad_0355;
        case 500: return R.drawable.quad_0500;
        case 501: return R.drawable.quad_0501;
        case 503: return R.drawable.quad_0503;
        case 505: return R.drawable.quad_0505;
        case 510: return R.drawable.quad_0510;
        case 511: return R.drawable.quad_0511;
        case 513: return R.drawable.quad_0513;
        case 515: return R.drawable.quad_0515;
        case 530: return R.drawable.quad_0530;
        case 531: return R.drawable.quad_0531;
        case 533: return R.drawable.quad_0533;
        case 535: return R.drawable.quad_0535;
        case 550: return R.drawable.quad_0550;
        case 551: return R.drawable.quad_0551;
        case 553: return R.drawable.quad_0553;
        case 555: return R.drawable.quad_0555;
        case 1000: return R.drawable.quad_1000;
        case 1001: return R.drawable.quad_1001;
        case 1003: return R.drawable.quad_1003;
        case 1005: return R.drawable.quad_1005;
        case 1010: return R.drawable.quad_1010;
        case 1011: return R.drawable.quad_1011;
        case 1013: return R.drawable.quad_1013;
        case 1015: return R.drawable.quad_1015;
        case 1030: return R.drawable.quad_1030;
        case 1031: return R.drawable.quad_1031;
        case 1033: return R.drawable.quad_1033;
        case 1035: return R.drawable.quad_1035;
        case 1050: return R.drawable.quad_1050;
        case 1051: return R.drawable.quad_1051;
        case 1053: return R.drawable.quad_1053;
        case 1055: return R.drawable.quad_1055;
        case 1100: return R.drawable.quad_1100;
        case 1101: return R.drawable.quad_1101;
        case 1103: return R.drawable.quad_1103;
        case 1105: return R.drawable.quad_1105;
        case 1110: return R.drawable.quad_1110;
        case 1111: return R.drawable.quad_1111;
        case 1113: return R.drawable.quad_1113;
        case 1115: return R.drawable.quad_1115;
        case 1130: return R.drawable.quad_1130;
        case 1131: return R.drawable.quad_1131;
        case 1133: return R.drawable.quad_1133;
        case 1135: return R.drawable.quad_1135;
        case 1150: return R.drawable.quad_1150;
        case 1151: return R.drawable.quad_1151;
        case 1153: return R.drawable.quad_1153;
        case 1155: return R.drawable.quad_1155;
        case 1300: return R.drawable.quad_1300;
        case 1301: return R.drawable.quad_1301;
        case 1303: return R.drawable.quad_1303;
        case 1305: return R.drawable.quad_1305;
        case 1310: return R.drawable.quad_1310;
        case 1311: return R.drawable.quad_1311;
        case 1313: return R.drawable.quad_1313;
        case 1315: return R.drawable.quad_1315;
        case 1330: return R.drawable.quad_1330;
        case 1331: return R.drawable.quad_1331;
        case 1333: return R.drawable.quad_1333;
        case 1335: return R.drawable.quad_1335;
        case 1350: return R.drawable.quad_1350;
        case 1351: return R.drawable.quad_1351;
        case 1353: return R.drawable.quad_1353;
        case 1355: return R.drawable.quad_1355;
        case 1500: return R.drawable.quad_1500;
        case 1501: return R.drawable.quad_1501;
        case 1503: return R.drawable.quad_1503;
        case 1505: return R.drawable.quad_1505;
        case 1510: return R.drawable.quad_1510;
        case 1511: return R.drawable.quad_1511;
        case 1513: return R.drawable.quad_1513;
        case 1515: return R.drawable.quad_1515;
        case 1530: return R.drawable.quad_1530;
        case 1531: return R.drawable.quad_1531;
        case 1533: return R.drawable.quad_1533;
        case 1535: return R.drawable.quad_1535;
        case 1550: return R.drawable.quad_1550;
        case 1551: return R.drawable.quad_1551;
        case 1553: return R.drawable.quad_1553;
        case 1555: return R.drawable.quad_1555;
        case 3000: return R.drawable.quad_3000;
        case 3001: return R.drawable.quad_3001;
        case 3003: return R.drawable.quad_3003;
        case 3005: return R.drawable.quad_3005;
        case 3010: return R.drawable.quad_3010;
        case 3011: return R.drawable.quad_3011;
        case 3013: return R.drawable.quad_3013;
        case 3015: return R.drawable.quad_3015;
        case 3030: return R.drawable.quad_3030;
        case 3031: return R.drawable.quad_3031;
        case 3033: return R.drawable.quad_3033;
        case 3035: return R.drawable.quad_3035;
        case 3050: return R.drawable.quad_3050;
        case 3051: return R.drawable.quad_3051;
        case 3053: return R.drawable.quad_3053;
        case 3055: return R.drawable.quad_3055;
        case 3100: return R.drawable.quad_3100;
        case 3101: return R.drawable.quad_3101;
        case 3103: return R.drawable.quad_3103;
        case 3105: return R.drawable.quad_3105;
        case 3110: return R.drawable.quad_3110;
        case 3111: return R.drawable.quad_3111;
        case 3113: return R.drawable.quad_3113;
        case 3115: return R.drawable.quad_3115;
        case 3130: return R.drawable.quad_3130;
        case 3131: return R.drawable.quad_3131;
        case 3133: return R.drawable.quad_3133;
        case 3135: return R.drawable.quad_3135;
        case 3150: return R.drawable.quad_3150;
        case 3151: return R.drawable.quad_3151;
        case 3153: return R.drawable.quad_3153;
        case 3155: return R.drawable.quad_3155;
        case 3300: return R.drawable.quad_3300;
        case 3301: return R.drawable.quad_3301;
        case 3303: return R.drawable.quad_3303;
        case 3305: return R.drawable.quad_3305;
        case 3310: return R.drawable.quad_3310;
        case 3311: return R.drawable.quad_3311;
        case 3313: return R.drawable.quad_3313;
        case 3315: return R.drawable.quad_3315;
        case 3330: return R.drawable.quad_3330;
        case 3331: return R.drawable.quad_3331;
        case 3333: return R.drawable.quad_3333;
        case 3335: return R.drawable.quad_3335;
        case 3350: return R.drawable.quad_3350;
        case 3351: return R.drawable.quad_3351;
        case 3353: return R.drawable.quad_3353;
        case 3355: return R.drawable.quad_3355;
        case 3500: return R.drawable.quad_3500;
        case 3501: return R.drawable.quad_3501;
        case 3503: return R.drawable.quad_3503;
        case 3505: return R.drawable.quad_3505;
        case 3510: return R.drawable.quad_3510;
        case 3511: return R.drawable.quad_3511;
        case 3513: return R.drawable.quad_3513;
        case 3515: return R.drawable.quad_3515;
        case 3530: return R.drawable.quad_3530;
        case 3531: return R.drawable.quad_3531;
        case 3533: return R.drawable.quad_3533;
        case 3535: return R.drawable.quad_3535;
        case 3550: return R.drawable.quad_3550;
        case 3551: return R.drawable.quad_3551;
        case 3553: return R.drawable.quad_3553;
        case 3555: return R.drawable.quad_3555;
        case 5000: return R.drawable.quad_5000;
        case 5001: return R.drawable.quad_5001;
        case 5003: return R.drawable.quad_5003;
        case 5005: return R.drawable.quad_5005;
        case 5010: return R.drawable.quad_5010;
        case 5011: return R.drawable.quad_5011;
        case 5013: return R.drawable.quad_5013;
        case 5015: return R.drawable.quad_5015;
        case 5030: return R.drawable.quad_5030;
        case 5031: return R.drawable.quad_5031;
        case 5033: return R.drawable.quad_5033;
        case 5035: return R.drawable.quad_5035;
        case 5050: return R.drawable.quad_5050;
        case 5051: return R.drawable.quad_5051;
        case 5053: return R.drawable.quad_5053;
        case 5055: return R.drawable.quad_5055;
        case 5100: return R.drawable.quad_5100;
        case 5101: return R.drawable.quad_5101;
        case 5103: return R.drawable.quad_5103;
        case 5105: return R.drawable.quad_5105;
        case 5110: return R.drawable.quad_5110;
        case 5111: return R.drawable.quad_5111;
        case 5113: return R.drawable.quad_5113;
        case 5115: return R.drawable.quad_5115;
        case 5130: return R.drawable.quad_5130;
        case 5131: return R.drawable.quad_5131;
        case 5133: return R.drawable.quad_5133;
        case 5135: return R.drawable.quad_5135;
        case 5150: return R.drawable.quad_5150;
        case 5151: return R.drawable.quad_5151;
        case 5153: return R.drawable.quad_5153;
        case 5155: return R.drawable.quad_5155;
        case 5300: return R.drawable.quad_5300;
        case 5301: return R.drawable.quad_5301;
        case 5303: return R.drawable.quad_5303;
        case 5305: return R.drawable.quad_5305;
        case 5310: return R.drawable.quad_5310;
        case 5311: return R.drawable.quad_5311;
        case 5313: return R.drawable.quad_5313;
        case 5315: return R.drawable.quad_5315;
        case 5330: return R.drawable.quad_5330;
        case 5331: return R.drawable.quad_5331;
        case 5333: return R.drawable.quad_5333;
        case 5335: return R.drawable.quad_5335;
        case 5350: return R.drawable.quad_5350;
        case 5351: return R.drawable.quad_5351;
        case 5353: return R.drawable.quad_5353;
        case 5355: return R.drawable.quad_5355;
        case 5500: return R.drawable.quad_5500;
        case 5501: return R.drawable.quad_5501;
        case 5503: return R.drawable.quad_5503;
        case 5505: return R.drawable.quad_5505;
        case 5510: return R.drawable.quad_5510;
        case 5511: return R.drawable.quad_5511;
        case 5513: return R.drawable.quad_5513;
        case 5515: return R.drawable.quad_5515;
        case 5530: return R.drawable.quad_5530;
        case 5531: return R.drawable.quad_5531;
        case 5533: return R.drawable.quad_5533;
        case 5535: return R.drawable.quad_5535;
        case 5550: return R.drawable.quad_5550;
        case 5551: return R.drawable.quad_5551;
        case 5553: return R.drawable.quad_5553;
        case 5555: return R.drawable.quad_5555;
        default:
            return R.drawable.quad_0000;
        }
    }


    private static int getIconIdForCpuUsageTriLevel3(int[] cpuUsages) {

        // リソース節約のため 0, 1, 3, 5 のみとする
        int digit = 0;
        switch (cpuUsageToLevel3(cpuUsages.length < 2 ? 0 : cpuUsages[1])) {
        case 0: digit += 100*0;    break;
        case 1: digit += 100*1;    break;
        case 2: digit += 100*1;    break;
        case 3: digit += 100*3;    break;
        case 4: digit += 100*3;    break;
        case 5: digit += 100*5;    break;
        }
        switch (cpuUsageToLevel3(cpuUsages.length < 3 ? 0 : cpuUsages[2])) {
        case 0: digit += 10*0; break;
        case 1: digit += 10*1; break;
        case 2: digit += 10*1; break;
        case 3: digit += 10*3; break;
        case 4: digit += 10*3; break;
        case 5: digit += 10*5; break;
        }
        switch (cpuUsageToLevel3(cpuUsages.length < 4 ? 0 : cpuUsages[3])) {
        case 0: digit += 1*0;  break;
        case 1: digit += 1*1;  break;
        case 2: digit += 1*1;  break;
        case 3: digit += 1*3;  break;
        case 4: digit += 1*3;  break;
        case 5: digit += 1*5;  break;
        }

//      MyLog.i("digit:" + digit);

        // for Level3
        switch (digit) {
        case 0: return R.drawable.tri_000;
        case 1: return R.drawable.tri_001;
        case 3: return R.drawable.tri_003;
        case 5: return R.drawable.tri_005;
        case 10: return R.drawable.tri_010;
        case 11: return R.drawable.tri_011;
        case 13: return R.drawable.tri_013;
        case 15: return R.drawable.tri_015;
        case 30: return R.drawable.tri_030;
        case 31: return R.drawable.tri_031;
        case 33: return R.drawable.tri_033;
        case 35: return R.drawable.tri_035;
        case 50: return R.drawable.tri_050;
        case 51: return R.drawable.tri_051;
        case 53: return R.drawable.tri_053;
        case 55: return R.drawable.tri_055;
        case 100: return R.drawable.tri_100;
        case 101: return R.drawable.tri_101;
        case 103: return R.drawable.tri_103;
        case 105: return R.drawable.tri_105;
        case 110: return R.drawable.tri_110;
        case 111: return R.drawable.tri_111;
        case 113: return R.drawable.tri_113;
        case 115: return R.drawable.tri_115;
        case 130: return R.drawable.tri_130;
        case 131: return R.drawable.tri_131;
        case 133: return R.drawable.tri_133;
        case 135: return R.drawable.tri_135;
        case 150: return R.drawable.tri_150;
        case 151: return R.drawable.tri_151;
        case 153: return R.drawable.tri_153;
        case 155: return R.drawable.tri_155;
        case 300: return R.drawable.tri_300;
        case 301: return R.drawable.tri_301;
        case 303: return R.drawable.tri_303;
        case 305: return R.drawable.tri_305;
        case 310: return R.drawable.tri_310;
        case 311: return R.drawable.tri_311;
        case 313: return R.drawable.tri_313;
        case 315: return R.drawable.tri_315;
        case 330: return R.drawable.tri_330;
        case 331: return R.drawable.tri_331;
        case 333: return R.drawable.tri_333;
        case 335: return R.drawable.tri_335;
        case 350: return R.drawable.tri_350;
        case 351: return R.drawable.tri_351;
        case 353: return R.drawable.tri_353;
        case 355: return R.drawable.tri_355;
        case 500: return R.drawable.tri_500;
        case 501: return R.drawable.tri_501;
        case 503: return R.drawable.tri_503;
        case 505: return R.drawable.tri_505;
        case 510: return R.drawable.tri_510;
        case 511: return R.drawable.tri_511;
        case 513: return R.drawable.tri_513;
        case 515: return R.drawable.tri_515;
        case 530: return R.drawable.tri_530;
        case 531: return R.drawable.tri_531;
        case 533: return R.drawable.tri_533;
        case 535: return R.drawable.tri_535;
        case 550: return R.drawable.tri_550;
        case 551: return R.drawable.tri_551;
        case 553: return R.drawable.tri_553;
        case 555: return R.drawable.tri_555;
        default:
            return R.drawable.tri_000;
        }
    }
}
