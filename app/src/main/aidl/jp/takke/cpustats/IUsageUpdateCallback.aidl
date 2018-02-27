package jp.takke.cpustats;

oneway interface IUsageUpdateCallback {

    void updateUsage(in int[] cpuUsages,
                     in int[] freqs, in int[] minFreqs, in int[] maxFreqs);
}