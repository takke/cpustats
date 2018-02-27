package jp.takke.cpustats;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

public class MyUtil {

    /**
     * CPU使用率の算出
     * 
     * @param currentInfo
     * @param lastInfo
     * @return CPU使用率の配列(要素数は必ず1以上、[0]は全CPU、[1]以降は各コア)、または null
     */
    @Nullable
    public static int[] calcCpuUsages(ArrayList<OneCpuInfo> currentInfo, @Nullable ArrayList<OneCpuInfo> lastInfo) {
        
        if (currentInfo == null || lastInfo == null) {
            // NPE対策(基本的に発生しないはず。サービスが死んだときにはありうるかな？)
            return null;
        }
        
        final int nLast = lastInfo.size();
        final int nCurr = currentInfo.size();
        if (nLast == 0 || nCurr == 0) {
            MyLog.d(" no info: [" + nLast + "][" + nCurr + "]");
            return null;
        }
        
        // 前回と今回で小さい方の個数で比較する
        // ※Galaxy S II 等で発生するコア数の変動状態でも少ない方のコア数で比較してなるべくCPU使用率を表示する
        final int n = (nLast < nCurr ? nLast : nCurr);  // min(nLast, nCurr)
        final int[] cpuUsages = new int[n];
        for (int i=0; i<n; i++) {
            final OneCpuInfo last = lastInfo.get(i);
            final OneCpuInfo curr = currentInfo.get(i);
            
            final int totalDiff = (int) (curr.total - last.total);  // 「差」なのでintに収まるはず。
            if (totalDiff > 0) {
                final int idleDiff = (int) (curr.idle - last.idle);
//              final double cpuUsage = 1.0 - (double)idleDiff / totalDiff;
//              cpuUsages[i] = (int)(cpuUsage * 100.0);
                // 高速化のため整数演算とする(切り上げの値になるけどいいでしょう)
                cpuUsages[i] = 100 - idleDiff*100 / totalDiff;
                
//              MyLog.i(" idle[" + idleDiff + "], total[" + totalDiff + "], " +
//                      "/[" + (100-idleDiff*100/totalDiff) + "], " +
//                      "rate[" + cpuUsages[i] + "], " +
//                      "rate[" + ((1.0-(double)idleDiff/totalDiff) * 100.0) + "]"
//                      );
            } else {
                cpuUsages[i] = 0;
            }
            
//          MyLog.d(" [" + (i == 0 ? "all" : i) + "] : [" + (int)(cpuUsage * 100.0) + "%]" +
//                  " idle[" + idleDiff + "], total[" + totalDiff + "]");
        }
        
        return cpuUsages;
    }

    /**
     * 擬似的なCPU使用率を各コアの周波数(およびその min/max 値)から算出する
     */
    @NonNull
    public static int[] calcCpuUsagesByCoreFrequencies(AllCoreFrequencyInfo fi) {

        final int coreCount = fi.freqs.length;

        // [0] は全体、[1]～[coreCount] は各コアの CPU 使用率
        final int[] cpuUsages = new int[coreCount+1];

        // 各コアの CPU 使用率を算出する
//        MyLog.i("---");
        for (int i = 0; i < coreCount; i++) {
            cpuUsages[i+1] = MyUtil.getClockPercent(fi.freqs[i], fi.minFreqs[i], fi.maxFreqs[i]);
//            MyLog.i("calc core[" + i + "] = " + cpuUsages[i+1] + "% (max=" + fi.maxFreqs[i] + ")");
        }

        // 全体の CPU 使用率を算出する
        // TODO big.LITTLE で停止するコアの考慮はしていない
        int freqSum = 0;
        int minFreqSum = 0;
        int maxFreqSum = 0;
        for (int i = 0; i < coreCount; i++) {
            freqSum += fi.freqs[i];
            minFreqSum += fi.minFreqs[i];
            maxFreqSum += fi.maxFreqs[i];
        }
        cpuUsages[0] = MyUtil.getClockPercent(freqSum, minFreqSum, maxFreqSum);

        return cpuUsages;
    }

    /**
     * クロック周波数の表示用整形
     * 
     * @param clockHz クロック周波数[KHz]
     * @return "XX MHz" または "X.X GHz"
     */
    public static String formatFreq(int clockHz) {
        
        if (clockHz < 1000*1000) {
            return (clockHz / 1000) + " MHz";
        }
        
        // a.b GHz
        final int a = (clockHz / 1000 / 1000);      // a.b GHz の a 値
        final int b = (clockHz / 1000 / 100) % 10;  // a.b GHz の b 値
        return a + "." + b + " GHz";
    }

    /**
     * 最もアクティブなコアのインデックスを返す
     */
    public static int getActiveCoreIndex(int[] freqs) {

        int targetCore = 0;
        for (int i = 1; i < freqs.length; i++) {
            if (freqs[i] > freqs[targetCore]) {
                targetCore = i;
            }
        }
        return targetCore;
    }

    /**
     * クロック周波数の current/min/max から [0, 100] % を算出する
     */
    public static int getClockPercent(int currentFreq, int minFreq, int maxFreq) {
        if (maxFreq - minFreq <= 0) {
            return 0;
        }
        return maxFreq >= 0 ? ((currentFreq - minFreq) * 100 / (maxFreq - minFreq)) : 0;
    }

}
