@file:Suppress("FunctionName")

package jp.takke.cpustats

import java.util.*

internal object CpuNotificationDataDistributor {

    fun distributeNotificationData(cpuUsages: IntArray, coreDistributionMode: Int): Array<CpuNotificationData> {

        // cpuUsages の index=0 は「全CPU使用率の平均」
        val coreCount = cpuUsages.size - 1

        return when (coreDistributionMode) {
            C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED -> distributeNotificationData_1IconUnsorted(coreCount, cpuUsages)

            C.CORE_DISTRIBUTION_MODE_1ICON_SORTED -> distributeNotificationData_1IconSorted(coreCount, cpuUsages)

            C.CORE_DISTRIBUTION_MODE_2ICONS -> distributeNotificationData_2Icons(coreCount, cpuUsages)
            else -> distributeNotificationData_2Icons(coreCount, cpuUsages)
        }
    }

    private fun distributeNotificationData_1IconUnsorted(coreCount: Int, cpuUsages: IntArray): Array<CpuNotificationData> {

        if (coreCount <= 4) {
            // 4コア以下
            val data = arrayOf(CpuNotificationData())

            // icon1
            data[0].cpuUsages = cpuUsages
            data[0].cpuUsages = cpuUsages
            data[0].coreNoStart = 1
            data[0].coreNoEnd = coreCount

            return data
        }

        run {
            // 5コア以上
            val data = arrayOf(CpuNotificationData())

            // icon1
            data[0].cpuUsages = IntArray(1 + 4)
            System.arraycopy(cpuUsages, 0, data[0].cpuUsages!!, 0, 5)

            data[0].coreNoStart = 1
            data[0].coreNoEnd = 4

            return data
        }
    }

    private fun distributeNotificationData_1IconSorted(coreCount: Int, cpuUsagesIn: IntArray): Array<CpuNotificationData> {

        val data = arrayOf(CpuNotificationData())

        // ソートして破壊されるのを防ぐためにコピーしておく
        val cpuUsages = IntArray(cpuUsagesIn.size)
        System.arraycopy(cpuUsagesIn, 0, cpuUsages, 0, cpuUsagesIn.size)

        // icon1
        data[0].cpuUsages = IntArray(1 + min(coreCount, 4))
        data[0].cpuUsages!![0] = cpuUsages[0]

        // index=0 は無視してソートする
        Arrays.sort(cpuUsages, 1, cpuUsages.size)

        // 降順に選択する
        for (i in 0 until min(coreCount, 4)) {
            data[0].cpuUsages!![i + 1] = cpuUsages[cpuUsages.size - i - 1]
        }

        data[0].coreNoStart = 1
        data[0].coreNoEnd = min(coreCount, 4)

        return data
    }

    private fun min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    private fun distributeNotificationData_2Icons(coreCount: Int, cpuUsages: IntArray): Array<CpuNotificationData> {

        if (coreCount <= 4) {
            // 4コア以下
            val data = arrayOf(CpuNotificationData())

            // icon1
            data[0].cpuUsages = cpuUsages
            data[0].coreNoStart = 1
            data[0].coreNoEnd = coreCount

            return data
        }

        if (coreCount == 6) {
            // 6コアなので3つずつに分割する
            val data = arrayOf(CpuNotificationData(), CpuNotificationData())

            // icon1
            data[0].cpuUsages = IntArray(3 + 1)
            data[0].coreNoStart = 1
            data[0].coreNoEnd = 3
            System.arraycopy(cpuUsages, 0, data[0].cpuUsages!!, 0, 4)

            // icon2
            data[1].cpuUsages = IntArray(3 + 1)
            data[1].coreNoStart = 4
            data[1].coreNoEnd = 6
            // icon2のindex=0 も「全CPU使用率の平均」とする
            data[1].cpuUsages!![0] = cpuUsages[0]
            System.arraycopy(cpuUsages, 4, data[1].cpuUsages!!, 1, 3)

            return data
        }

        run {
            // 4コア以上(6コアを除く)

            // 2つに分割する
            val data = arrayOf(CpuNotificationData(), CpuNotificationData())

            // icon1
            data[0].cpuUsages = IntArray(5)
            data[0].coreNoStart = 1
            data[0].coreNoEnd = 4
            System.arraycopy(cpuUsages, 0, data[0].cpuUsages!!, 0, 5)

            // icon2
            data[1].cpuUsages = IntArray(1 + coreCount - 4)
            data[1].coreNoStart = 5
            data[1].coreNoEnd = coreCount
            // icon2のindex=0 も「全CPU使用率の平均」とする
            data[1].cpuUsages!![0] = cpuUsages[0]
            System.arraycopy(cpuUsages, 5, data[1].cpuUsages!!, 1, coreCount - 4)

            return data
        }
    }
}
