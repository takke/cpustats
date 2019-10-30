package jp.takke.cpustats

import java.util.*

object MyUtil {

    /**
     * CPU使用率の算出
     *
     * @param currentInfo
     * @param lastInfo
     * @return CPU使用率の配列(要素数は必ず1以上、[0]は全CPU、[1]以降は各コア)、または null
     */
    fun calcCpuUsages(currentInfo: ArrayList<OneCpuInfo>?, lastInfo: ArrayList<OneCpuInfo>?): IntArray? {

        if (currentInfo == null || lastInfo == null) {
            // NPE対策(基本的に発生しないはず。サービスが死んだときにはありうるかな？)
            return null
        }

        val nLast = lastInfo.size
        val nCurr = currentInfo.size
        if (nLast == 0 || nCurr == 0) {
            MyLog.d(" no info: [$nLast][$nCurr]")
            return null
        }

        // 前回と今回で小さい方の個数で比較する
        // ※Galaxy S II 等で発生するコア数の変動状態でも少ない方のコア数で比較してなるべくCPU使用率を表示する
        val n = if (nLast < nCurr) nLast else nCurr  // min(nLast, nCurr)
        val cpuUsages = IntArray(n)
        for (i in 0 until n) {
            val last = lastInfo[i]
            val curr = currentInfo[i]

            val totalDiff = (curr.total - last.total).toInt()  // 「差」なのでintに収まるはず。
            if (totalDiff > 0) {
                val idleDiff = (curr.idle - last.idle).toInt()
//              final double cpuUsage = 1.0 - (double)idleDiff / totalDiff;
//              cpuUsages[i] = (int)(cpuUsage * 100.0);
                // 高速化のため整数演算とする(切り上げの値になるけどいいでしょう)
                cpuUsages[i] = 100 - idleDiff * 100 / totalDiff

//              MyLog.i(" idle[" + idleDiff + "], total[" + totalDiff + "], " +
//                      "/[" + (100-idleDiff*100/totalDiff) + "], " +
//                      "rate[" + cpuUsages[i] + "], " +
//                      "rate[" + ((1.0-(double)idleDiff/totalDiff) * 100.0) + "]"
//                      );
            } else {
                cpuUsages[i] = 0
            }

//          MyLog.d(" [" + (i == 0 ? "all" : i) + "] : [" + (int)(cpuUsage * 100.0) + "%]" +
//                  " idle[" + idleDiff + "], total[" + totalDiff + "]");
        }

        return cpuUsages
    }

    /**
     * 擬似的なCPU使用率を各コアの周波数(およびその min/max 値)から算出する
     */
    fun calcCpuUsagesByCoreFrequencies(fi: AllCoreFrequencyInfo): IntArray {

        val coreCount = fi.freqs.size

        // [0] は全体、[1]～[coreCount] は各コアの CPU 使用率
        val cpuUsages = IntArray(coreCount + 1)

        // 各コアの CPU 使用率を算出する
//        MyLog.i("---");
        for (i in 0 until coreCount) {
            cpuUsages[i + 1] = getClockPercent(fi.freqs[i], fi.minFreqs[i], fi.maxFreqs[i])
//            MyLog.i("calc core[" + i + "] = " + cpuUsages[i+1] + "% (max=" + fi.maxFreqs[i] + ")");
        }

        // 全体の CPU 使用率を算出する
        // TODO big.LITTLE で停止するコアの考慮はしていない
        var freqSum = 0
        var minFreqSum = 0
        var maxFreqSum = 0
        for (i in 0 until coreCount) {
            freqSum += fi.freqs[i]
            minFreqSum += fi.minFreqs[i]
            maxFreqSum += fi.maxFreqs[i]
        }
        cpuUsages[0] = getClockPercent(freqSum, minFreqSum, maxFreqSum)

        return cpuUsages
    }

    /**
     * クロック周波数の表示用整形
     *
     * @param clockHz クロック周波数(KHz)
     * @return "XX MHz" または "X.X GHz"
     */
    fun formatFreq(clockHz: Int): String {

        if (clockHz < 1000 * 1000) {
            return (clockHz / 1000).toString() + " MHz"
        }

        // a.b GHz
        val a = clockHz / 1000 / 1000      // a.b GHz の a 値
        val b = clockHz / 1000 / 100 % 10  // a.b GHz の b 値
        return "$a.$b GHz"
    }

    /**
     * 最もアクティブなコアのインデックスを返す
     */
    fun getActiveCoreIndex(freqs: IntArray): Int {

        var targetCore = 0
        for (i in 1 until freqs.size) {
            if (freqs[i] > freqs[targetCore]) {
                targetCore = i
            }
        }
        return targetCore
    }

    /**
     * クロック周波数の current/min/max から [0, 100] % を算出する
     */
    fun getClockPercent(currentFreq: Int, minFreq: Int, maxFreq: Int): Int {
        if (maxFreq - minFreq <= 0) {
            return 0
        }
        return if (maxFreq >= 0) (currentFreq - minFreq) * 100 / (maxFreq - minFreq) else 0
    }

}
