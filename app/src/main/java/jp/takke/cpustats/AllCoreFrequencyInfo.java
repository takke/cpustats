package jp.takke.cpustats;

public class AllCoreFrequencyInfo {

    public final int[] freqs;
    public final int[] minFreqs;
    public final int[] maxFreqs;

    public AllCoreFrequencyInfo(int coreCount) {
        this.freqs = new int[coreCount];
        this.minFreqs = new int[coreCount];
        this.maxFreqs = new int[coreCount];
    }
}
