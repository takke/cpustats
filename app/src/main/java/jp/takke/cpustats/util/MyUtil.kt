package jp.takke.cpustats.util

import jp.takke.cpustats.AllCoreFrequencyInfo
import jp.takke.cpustats.MyLog
import jp.takke.cpustats.OneCpuInfo

object MyUtil {

  /**
   * CPU使用率の算出
   *
   * @param currentInfo
   * @param lastInfo
   * @return CPU使用率の配列(要素数は必ず1以上、[0]は全CPU、[1]以降は各コア)、または null
   */
  fun calcCpuUsages(
    currentInfo: ArrayList<OneCpuInfo>?,
    lastInfo: ArrayList<OneCpuInfo>?
  ): IntArray? {

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
   * 数値+単位の文字列を「左から右」の向きに固定する
   *
   * アラビア語等の RTL 言語では、"1.1 GHz" や "(400 MHz - 2.2 GHz)" のような文字列が
   * Bidi アルゴリズムにより "GHz 1.1" のように並べ替えられてしまうため、
   * Unicode の方向分離文字(LRI U+2066 ... PDI U+2069)で囲んで防ぐ。
   * LTR 言語では表示に影響しない。
   */
  fun ltr(s: String): String = "\u2066$s\u2069"

  /**
   * バッテリー温度の表示用整形
   *
   * @param temperature 温度(0.1℃単位)
   * @param fahrenheit true なら華氏に変換して表示する
   * @return "32.5°C" または "90.5°F" のような文字列
   */
  fun formatTemperature(temperature: Int, fahrenheit: Boolean = false): String {
    val value = if (fahrenheit) celsiusToFahrenheit10(temperature) else temperature
    val unit = if (fahrenheit) "°F" else "°C"
    val sign = if (value < 0) "-" else ""
    val t = if (value < 0) -value else value
    return "$sign${t / 10}.${t % 10}$unit"
  }

  /**
   * 摂氏(0.1℃単位)を華氏(0.1°F単位)に変換する(小数第1位で四捨五入)
   */
  fun celsiusToFahrenheit10(celsius10: Int): Int =
    Math.round(celsius10 * 9 / 5.0).toInt() + 320

  /**
   * メモリサイズの表示用整形
   *
   * @param sizeMb サイズ[MB]
   * @return "800 MB" または "3.2 GB" のような文字列
   */
  fun formatMemSize(sizeMb: Int): String {
    if (sizeMb < 1024) {
      return "$sizeMb MB"
    }
    val a = sizeMb / 1024
    val b = sizeMb * 10 / 1024 % 10
    return "$a.$b GB"
  }

  /**
   * CPU クラスタ構成の表示用整形(big.LITTLE 対応)
   *
   * cpuinfo_max_freq が同じコアを 1 クラスタとしてグループ化する
   *
   * @param maxFreqs 各コアの最大周波数(KHz)。0 は取得不可として無視する
   * @return "4×1.8 GHz + 4×2.8 GHz" のような文字列(周波数昇順)。取得不可時は空文字
   */
  fun formatCpuClusters(maxFreqs: IntArray): String {

    val groups = maxFreqs.filter { it > 0 }.groupBy { it }.toSortedMap()
    if (groups.isEmpty()) {
      return ""
    }
    return groups.entries.joinToString(" + ") { (freq, cores) ->
      "${cores.size}×${formatFreq(freq)}"
    }
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
