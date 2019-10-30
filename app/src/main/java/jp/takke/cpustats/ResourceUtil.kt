package jp.takke.cpustats


object ResourceUtil {

    internal fun getIconIdForCpuUsage(cpuUsages: IntArray?): Int {

        if (cpuUsages == null || cpuUsages.isEmpty()) {
            return R.drawable.single000
        }

//        val coreCount = MyUtil.calcCpuCoreCount()
//      MyLog.i("core:" + coreCount)
        val coreCount = cpuUsages.size - 1

        // デバッグ用にコア数を変更する
//      val coreCount = 1

        return when (coreCount) {
            // シングルコア
            1 -> getIconIdForCpuUsageSingleMono(cpuUsages[0])

            // 2コア
            2 -> getIconIdForCpuUsageDual(cpuUsages)

            // 3コア(6コアの2分割など)
            3 -> QuadResourceUtil.getIconIdForCpuUsageTri(cpuUsages)

            // 4コア以上
            else -> QuadResourceUtil.getIconIdForCpuUsageQuad(cpuUsages)
        }
    }

    /**
     * 1コアアイコンの取得 カラー版(プレビュー画面用)
     */
    internal fun getIconIdForCpuUsageSingleColor(cpuUsage: Int): Int {

        when (cpuUsageToLevel5(cpuUsage)) {
            0 -> return R.drawable.color_single000
            1 -> return R.drawable.color_single020
            2 -> return R.drawable.color_single040
            3 -> return R.drawable.color_single060
            4 -> return R.drawable.color_single080
            5 -> return R.drawable.color_single100
        }
        return R.drawable.single000
    }

    /**
     * 1コアアイコンの取得 モノクロ版(通知アイコン用)
     */
    private fun getIconIdForCpuUsageSingleMono(cpuUsage: Int): Int {

        when (cpuUsageToLevel5(cpuUsage)) {
            0 -> return R.drawable.single000
            1 -> return R.drawable.single020
            2 -> return R.drawable.single040
            3 -> return R.drawable.single060
            4 -> return R.drawable.single080
            5 -> return R.drawable.single100
        }
        return R.drawable.single000
    }

    /**
     * CPU 使用率を「レベル」に変換する
     *
     * @param cpuUsage CPU使用率[0,100]
     * @return レベル値[0,5]
     */
    private fun cpuUsageToLevel5(cpuUsage: Int): Int {

        return when {
            cpuUsage < 5 -> 0
            cpuUsage < 20 -> 1
            cpuUsage < 40 -> 2
            cpuUsage < 60 -> 3
            cpuUsage < 80 -> 4
            else -> 5
        }
    }

    private fun getIconIdForCpuUsageDual(cpuUsages: IntArray): Int {
        val levelForCore1 = cpuUsageToLevel5(if (cpuUsages.size < 2) 0 else cpuUsages[1])
        val levelForCore2 = cpuUsageToLevel5(if (cpuUsages.size < 3) 0 else cpuUsages[2])
        when (levelForCore1) {
            0 -> when (levelForCore2) {
                0 -> return R.drawable.dual_0_0
                1 -> return R.drawable.dual_0_1
                2 -> return R.drawable.dual_0_2
                3 -> return R.drawable.dual_0_3
                4 -> return R.drawable.dual_0_4
                5 -> return R.drawable.dual_0_5
            }

            1 -> when (levelForCore2) {
                0 -> return R.drawable.dual_1_0
                1 -> return R.drawable.dual_1_1
                2 -> return R.drawable.dual_1_2
                3 -> return R.drawable.dual_1_3
                4 -> return R.drawable.dual_1_4
                5 -> return R.drawable.dual_1_5
            }

            2 -> when (levelForCore2) {
                0 -> return R.drawable.dual_2_0
                1 -> return R.drawable.dual_2_1
                2 -> return R.drawable.dual_2_2
                3 -> return R.drawable.dual_2_3
                4 -> return R.drawable.dual_2_4
                5 -> return R.drawable.dual_2_5
            }

            3 -> when (levelForCore2) {
                0 -> return R.drawable.dual_3_0
                1 -> return R.drawable.dual_3_1
                2 -> return R.drawable.dual_3_2
                3 -> return R.drawable.dual_3_3
                4 -> return R.drawable.dual_3_4
                5 -> return R.drawable.dual_3_5
            }

            4 -> when (levelForCore2) {
                0 -> return R.drawable.dual_4_0
                1 -> return R.drawable.dual_4_1
                2 -> return R.drawable.dual_4_2
                3 -> return R.drawable.dual_4_3
                4 -> return R.drawable.dual_4_4
                5 -> return R.drawable.dual_4_5
            }

            5 -> when (levelForCore2) {
                0 -> return R.drawable.dual_5_0
                1 -> return R.drawable.dual_5_1
                2 -> return R.drawable.dual_5_2
                3 -> return R.drawable.dual_5_3
                4 -> return R.drawable.dual_5_4
                5 -> return R.drawable.dual_5_5
            }
        }
        return R.drawable.dual_0_0
    }

    /**
     * CPUクロック周波数のアイコンを取得する
     *
     * @param currentFreq クロック周波数(KHz)
     * @return R.drawable.freq_01　～ R.drawable.freq_50 の値
     */
    fun getIconIdForCpuFreq(currentFreq: Int): Int {

        // 下記のように変換する
        // 300MHz =>  3
        // 1.5GHz => 15
        val freqAB = currentFreq / 1000 / 100

        if (freqAB < 1) {
            return R.drawable.freq_01
        }
        when (freqAB) {
            1 -> return R.drawable.freq_01
            2 -> return R.drawable.freq_02
            3 -> return R.drawable.freq_03
            4 -> return R.drawable.freq_04
            5 -> return R.drawable.freq_05
            6 -> return R.drawable.freq_06
            7 -> return R.drawable.freq_07
            8 -> return R.drawable.freq_08
            9 -> return R.drawable.freq_09
            10 -> return R.drawable.freq_10
            11 -> return R.drawable.freq_11
            12 -> return R.drawable.freq_12
            13 -> return R.drawable.freq_13
            14 -> return R.drawable.freq_14
            15 -> return R.drawable.freq_15
            16 -> return R.drawable.freq_16
            17 -> return R.drawable.freq_17
            18 -> return R.drawable.freq_18
            19 -> return R.drawable.freq_19
            20 -> return R.drawable.freq_20
            21 -> return R.drawable.freq_21
            22 -> return R.drawable.freq_22
            23 -> return R.drawable.freq_23
            24 -> return R.drawable.freq_24
            25 -> return R.drawable.freq_25
            26 -> return R.drawable.freq_26
            27 -> return R.drawable.freq_27
            28 -> return R.drawable.freq_28
            29 -> return R.drawable.freq_29
            30 -> return R.drawable.freq_30
            31 -> return R.drawable.freq_31
            32 -> return R.drawable.freq_32
            33 -> return R.drawable.freq_33
            34 -> return R.drawable.freq_34
            35 -> return R.drawable.freq_35
            36 -> return R.drawable.freq_36
            37 -> return R.drawable.freq_37
            38 -> return R.drawable.freq_38
            39 -> return R.drawable.freq_39
            40 -> return R.drawable.freq_40
            41 -> return R.drawable.freq_41
            42 -> return R.drawable.freq_42
            43 -> return R.drawable.freq_43
            44 -> return R.drawable.freq_44
            45 -> return R.drawable.freq_45
            46 -> return R.drawable.freq_46
            47 -> return R.drawable.freq_47
            48 -> return R.drawable.freq_48
            49 -> return R.drawable.freq_49
            50 -> return R.drawable.freq_50
            else -> return R.drawable.freq_50 // 5.0GHz over
        }
    }

    /**
     * CPU使用率から背景色を取得する
     */
    fun getBackgroundColor(clockPercent: Int): Int {
        return when {
            clockPercent >= 60 -> 0xff442222.toInt()
            clockPercent > 0 -> 0xff333333.toInt()
            else -> 0xff222222.toInt()
        }
    }

    /**
     * CPU使用率から通知アイコンの色を取得する
     */
    fun getNotificationIconColorFromUsage(cpuUsage: Int): Int {

//        MyLog.i("iconcolor $cpuUsage -> " + ResourceUtil.cpuUsageToLevel5(cpuUsage))

        return when (cpuUsageToLevel5(cpuUsage)) {
            0 -> 0xff000000.toInt()
            1 -> 0xff00c21c.toInt()
            2 -> 0xff85c200.toInt()
            3 -> 0xffc4bd00.toInt()
            4 -> 0xffcf7400.toInt()
            5 -> 0xffcf0000.toInt()
            else -> 0xff000000.toInt()
        }
    }

}
