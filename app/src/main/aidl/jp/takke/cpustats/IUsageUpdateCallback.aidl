package jp.takke.cpustats;

oneway interface IUsageUpdateCallback {

    void updateUsage(in int[] cpuUsages, in int currentFreq, in int minFreq, in int maxFreq);
}