package jp.takke.cpustats

import jp.takke.cpustats.CpuNotificationDataDistributor.distributeNotificationData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class CpuNotificationDataDistributorTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        ShadowLog.stream = System.out
    }

    @Test
    fun distributeNotificationData_2icons() {

        // core1
        run {
            val cpuUsages = intArrayOf(30, 30)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(1)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(30, 30))
        }

        // core2
        run {
            val cpuUsages = intArrayOf(60, 30, 90)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(2)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(60, 30, 90))
        }

        // core3
        run {
            val cpuUsages = intArrayOf(50, 30, 90, 30)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(3)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(50, 30, 90, 30))
        }

        // core4
        run {
            val cpuUsages = intArrayOf(40, 30, 90, 30, 10)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 30, 90, 30, 10))
        }

        // core5
        run {
            val cpuUsages = intArrayOf(40, 30, 90, 30, 10, 40)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(2)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 30, 90, 30, 10))
            assertThat(data[1].coreNoStart).isEqualTo(5)
            assertThat(data[1].coreNoEnd).isEqualTo(5)
            assertThat(data[1].cpuUsages).isEqualTo(intArrayOf(40, 40))
        }

        // core6
        run {
            val cpuUsages = intArrayOf(40, 10, 20, 30, 70, 50, 60)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(2)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(3)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 10, 20, 30))
            assertThat(data[1].coreNoStart).isEqualTo(4)
            assertThat(data[1].coreNoEnd).isEqualTo(6)
            assertThat(data[1].cpuUsages).isEqualTo(intArrayOf(40, 70, 50, 60))
        }

        // core7
        run {
            val cpuUsages = intArrayOf(40, 10, 20, 30, 70, 50, 60, 40)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(2)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 10, 20, 30, 70))
            assertThat(data[1].coreNoStart).isEqualTo(5)
            assertThat(data[1].coreNoEnd).isEqualTo(7)
            assertThat(data[1].cpuUsages).isEqualTo(intArrayOf(40, 50, 60, 40))
        }

        // core8
        run {
            val cpuUsages = intArrayOf(45, 10, 30, 50, 20, 40, 80, 70, 60)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(2)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(45, 10, 30, 50, 20))
            assertThat(data[1].coreNoStart).isEqualTo(5)
            assertThat(data[1].coreNoEnd).isEqualTo(8)
            assertThat(data[1].cpuUsages).isEqualTo(intArrayOf(45, 40, 80, 70, 60))
        }

        // core9
        run {
            val cpuUsages = intArrayOf(45, 10, 30, 50, 20, 40, 80, 70, 60, 90)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS)
            assertThat(data.size).isEqualTo(2)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(45, 10, 30, 50, 20))
            assertThat(data[1].coreNoStart).isEqualTo(5)
            assertThat(data[1].coreNoEnd).isEqualTo(9)
            assertThat(data[1].cpuUsages).isEqualTo(intArrayOf(45, 40, 80, 70, 60, 90))
        }
    }

    @Test
    fun distributeNotificationData_1icon_unsorted() {

        // core1
        run {
            val cpuUsages = intArrayOf(30, 30)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(1)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(30, 30))
        }

        // core2
        run {
            val cpuUsages = intArrayOf(60, 30, 90)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(2)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(60, 30, 90))
        }

        // core3
        run {
            val cpuUsages = intArrayOf(50, 30, 90, 30)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(3)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(50, 30, 90, 30))
        }

        // core4
        run {
            val cpuUsages = intArrayOf(40, 30, 90, 30, 10)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 30, 90, 30, 10))
        }

        // core5
        run {
            val cpuUsages = intArrayOf(40, 30, 90, 30, 10, 40)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 30, 90, 30, 10))
        }

        // core6
        run {
            val cpuUsages = intArrayOf(40, 10, 20, 30, 70, 50, 60)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 10, 20, 30, 70))
        }

        // core7
        run {
            val cpuUsages = intArrayOf(40, 10, 20, 30, 70, 50, 60, 40)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 10, 20, 30, 70))
        }

        // core8
        run {
            val cpuUsages = intArrayOf(45, 10, 30, 50, 20, 40, 80, 70, 60)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(45, 10, 30, 50, 20))
        }

        // core9
        run {
            val cpuUsages = intArrayOf(45, 10, 30, 50, 20, 40, 80, 70, 60, 90)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(45, 10, 30, 50, 20))
        }
    }

    @Test
    fun distributeNotificationData_1icon_sorted() {

        // core1
        run {
            val cpuUsages = intArrayOf(30, 30)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(1)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(30, 30))
        }

        // core2
        run {
            val cpuUsages = intArrayOf(60, 30, 90)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(2)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(60, 90, 30))
        }

        // core3
        run {
            val cpuUsages = intArrayOf(50, 30, 90, 20)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(3)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(50, 90, 30, 20))
        }

        // core4
        run {
            val cpuUsages = intArrayOf(40, 20, 90, 30, 10)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 90, 30, 20, 10))
        }

        // core5
        run {
            val cpuUsages = intArrayOf(40, 30, 90, 10, 5, 40)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 90, 40, 30, 10))
        }

        // core6
        run {
            val cpuUsages = intArrayOf(40, 10, 20, 30, 70, 50, 60)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 70, 60, 50, 30))
        }

        // core7
        run {
            val cpuUsages = intArrayOf(40, 10, 20, 30, 70, 50, 60, 40)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(40, 70, 60, 50, 40))
        }

        // core8
        run {
            val cpuUsages = intArrayOf(45, 10, 30, 50, 20, 40, 80, 70, 60)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(45, 80, 70, 60, 50))
        }

        // core9
        run {
            val cpuUsages = intArrayOf(45, 10, 30, 50, 20, 40, 80, 70, 60, 90)
            val data = distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            assertThat(data.size).isEqualTo(1)
            assertThat(data[0].coreNoStart).isEqualTo(1)
            assertThat(data[0].coreNoEnd).isEqualTo(4)
            assertThat(data[0].cpuUsages).isEqualTo(intArrayOf(45, 90, 80, 70, 60))
        }
    }
}