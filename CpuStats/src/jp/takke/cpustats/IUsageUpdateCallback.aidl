package jp.takke.cpustats;

oneway interface IUsageUpdateCallback {

	void updateUsage(in int[] cpuUsages, in int currentFreq);
}