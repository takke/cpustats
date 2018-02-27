package jp.takke.cpustats;

import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class CpuInfoCollector {

    // core count cache
    private static int sLastCpuCoreCount = -1;

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or "availableProcessors" if failed to get result
     */
    // from http://stackoverflow.com/questions/7962155/how-can-you-detect-a-dual-core-cpu-on-an-android-device-from-code
    public static int calcCpuCoreCount() {

        if (sLastCpuCoreCount >= 1) {
            // キャッシュさせる
            return sLastCpuCoreCount;
        }

        try {
            // Get directory containing CPU info
            final File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            final File[] files = dir.listFiles(new FileFilter() {

                public boolean accept(File pathname) {
                    //Check if filename is "cpu", followed by a single digit number
                    if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                        return true;
                    }
                    return false;
                }
            });

            // Return the number of cores (virtual CPU devices)
            sLastCpuCoreCount = files.length;

        } catch(Exception e) {
            sLastCpuCoreCount = Runtime.getRuntime().availableProcessors();
        }

        return sLastCpuCoreCount;
    }

    /**
     * 現在のCPUクロックを取得する
     *
     * @return 384000 のような数値(取得エラー時は0)
     */
    private static int takeCurrentCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/scaling_cur_freq");
    }

    /**
     * 最小CPUクロックを取得する
     *
     * @return 384000 のような数値(取得エラー時は0)
     */
    private static int takeMinCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/cpuinfo_min_freq");
    }

    /**
     * 最大CPUクロックを取得する
     *
     * @return 384000 のような数値(取得エラー時は0)
     */
    private static int takeMaxCpuFreq(int coreIndex) {
        return readIntegerFile("/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/cpuinfo_max_freq");
    }

    /**
     * 全コアのCPUクロックを取得する
     */
    public static void takeAllCoreFreqs(AllCoreFrequencyInfo fi) {
        final int n = calcCpuCoreCount();

        for (int i = 0; i < n; i++) {
            fi.freqs[i] = takeCurrentCpuFreq(i);
            fi.minFreqs[i] = takeMinCpuFreq(i);
            fi.maxFreqs[i] = takeMaxCpuFreq(i);
        }
    }

    private static int readIntegerFile(String filePath) {

        try {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath)), 1000);
            final String line = reader.readLine();
            reader.close();

            return Integer.parseInt(line);
        } catch (Exception e) {
            MyLog.e(e);
            return 0;
        }
    }

    /**
     * /proc/stat から各コアの CPU 値を取得する
     *
     * @return 各コアの CPU 値のリスト(エラー時は要素数0)
     */
    @Nullable
    public static ArrayList<OneCpuInfo> takeCpuUsageSnapshot() {

        // [0] が全体、[1]以降が個別CPU
        final ArrayList<OneCpuInfo> result = new ArrayList<>();

        try {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream("/proc/stat")), C.READ_BUFFER_SIZE);
//          final RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");

//          MyLog.i("load:-----");

            String line;
            while ((line=reader.readLine()) != null) {
                if (!line.startsWith("cpu")) {
                    break;
                }
//              MyLog.i(" load:" + load);

                //     user     nice    system  idle    iowait  irq     softirq     steal
                //cpu  48200 4601 35693 979258 5095 1 855 0 0 0
                //cpu0 26847 1924 25608 212324 2212 1 782 0 0 0
                //cpu1 8371 1003 4180 254096 1026 0 50 0 0 0
                //cpu2 8450 983 3916 252872 1304 0 9 0 0 0
                //cpu3 4532 691 1989 259966 553 0 14 0 0 0

                final String[] tokens = line.split(" +");
                final OneCpuInfo oci = new OneCpuInfo();
                oci.idle = Long.parseLong(tokens[4]);
                oci.total = Long.parseLong(tokens[1])
                        + Long.parseLong(tokens[2])
                        + Long.parseLong(tokens[3])
                        + oci.idle
                        + Long.parseLong(tokens[5])
                        + Long.parseLong(tokens[6])
                        + Long.parseLong(tokens[7]);
                result.add(oci);
            }

            reader.close();
        } catch (Exception ex) {
            MyLog.e(ex);
            return null;
        }

        return result;
    }
}
