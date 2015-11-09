package jp.takke.cpustats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class UsageUpdateServiceTest {

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Test
    public void distributeNotificationData_2icons() {

        // core1
        {
            final int[] cpuUsages = {30, 30};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(1));
            assertThat(data[0].cpuUsages, is(new int[]{30, 30}));
        }

        // core2
        {
            final int[] cpuUsages = {60, 30, 90};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(2));
            assertThat(data[0].cpuUsages, is(new int[]{60, 30, 90}));
        }

        // core3
        {
            final int[] cpuUsages = {50, 30, 90, 30};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(3));
            assertThat(data[0].cpuUsages, is(new int[]{50, 30, 90, 30}));
        }

        // core4
        {
            final int[] cpuUsages = {40, 30, 90, 30, 10};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 30, 90, 30, 10}));
        }

        // core5
        {
            final int[] cpuUsages = {40, 30, 90, 30, 10, 40};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(2));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 30, 90, 30, 10}));
            assertThat(data[1].coreNoStart, is(5));
            assertThat(data[1].coreNoEnd, is(5));
            assertThat(data[1].cpuUsages, is(new int[]{40, 40}));
        }

        // core6
        {
            final int[] cpuUsages = {40, 10, 20, 30, 70, 50, 60};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(2));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(3));
            assertThat(data[0].cpuUsages, is(new int[]{40, 10, 20, 30}));
            assertThat(data[1].coreNoStart, is(4));
            assertThat(data[1].coreNoEnd, is(6));
            assertThat(data[1].cpuUsages, is(new int[]{40, 70, 50, 60}));
        }

        // core7
        {
            final int[] cpuUsages = {40, 10, 20, 30, 70, 50, 60, 40};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(2));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 10, 20, 30, 70}));
            assertThat(data[1].coreNoStart, is(5));
            assertThat(data[1].coreNoEnd, is(7));
            assertThat(data[1].cpuUsages, is(new int[]{40, 50, 60, 40}));
        }

        // core8
        {
            final int[] cpuUsages = {45, 10, 30, 50, 20, 40, 80, 70, 60};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(2));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{45, 10, 30, 50, 20}));
            assertThat(data[1].coreNoStart, is(5));
            assertThat(data[1].coreNoEnd, is(8));
            assertThat(data[1].cpuUsages, is(new int[]{45, 40, 80, 70, 60}));
        }

        // core9
        {
            final int[] cpuUsages = {45, 10, 30, 50, 20, 40, 80, 70, 60, 90};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_2ICONS);

            assertThat(data.length, is(2));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{45, 10, 30, 50, 20}));
            assertThat(data[1].coreNoStart, is(5));
            assertThat(data[1].coreNoEnd, is(9));
            assertThat(data[1].cpuUsages, is(new int[]{45, 40, 80, 70, 60, 90}));
        }
    }

    @Test
    public void distributeNotificationData_1icon_unsorted() {

        // core1
        {
            final int[] cpuUsages = {30, 30};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(1));
            assertThat(data[0].cpuUsages, is(new int[]{30, 30}));
        }

        // core2
        {
            final int[] cpuUsages = {60, 30, 90};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(2));
            assertThat(data[0].cpuUsages, is(new int[]{60, 30, 90}));
        }

        // core3
        {
            final int[] cpuUsages = {50, 30, 90, 30};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(3));
            assertThat(data[0].cpuUsages, is(new int[]{50, 30, 90, 30}));
        }

        // core4
        {
            final int[] cpuUsages = {40, 30, 90, 30, 10};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 30, 90, 30, 10}));
        }

        // core5
        {
            final int[] cpuUsages = {40, 30, 90, 30, 10, 40};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 30, 90, 30, 10}));
        }

        // core6
        {
            final int[] cpuUsages = {40, 10, 20, 30, 70, 50, 60};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 10, 20, 30, 70}));
        }

        // core7
        {
            final int[] cpuUsages = {40, 10, 20, 30, 70, 50, 60, 40};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 10, 20, 30, 70}));
        }

        // core8
        {
            final int[] cpuUsages = {45, 10, 30, 50, 20, 40, 80, 70, 60};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{45, 10, 30, 50, 20}));
        }

        // core9
        {
            final int[] cpuUsages = {45, 10, 30, 50, 20, 40, 80, 70, 60, 90};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{45, 10, 30, 50, 20}));
        }
    }

    @Test
    public void distributeNotificationData_1icon_sorted() {

        // core1
        {
            final int[] cpuUsages = {30, 30};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(1));
            assertThat(data[0].cpuUsages, is(new int[]{30, 30}));
        }

        // core2
        {
            final int[] cpuUsages = {60, 30, 90};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(2));
            assertThat(data[0].cpuUsages, is(new int[]{60, 90, 30}));
        }

        // core3
        {
            final int[] cpuUsages = {50, 30, 90, 20};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(3));
            assertThat(data[0].cpuUsages, is(new int[]{50, 90, 30, 20}));
        }

        // core4
        {
            final int[] cpuUsages = {40, 20, 90, 30, 10};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 90, 30, 20, 10}));
        }

        // core5
        {
            final int[] cpuUsages = {40, 30, 90, 10, 5, 40};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 90, 40, 30, 10}));
        }

        // core6
        {
            final int[] cpuUsages = {40, 10, 20, 30, 70, 50, 60};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 70, 60, 50, 30}));
        }

        // core7
        {
            final int[] cpuUsages = {40, 10, 20, 30, 70, 50, 60, 40};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{40, 70, 60, 50, 40}));
        }

        // core8
        {
            final int[] cpuUsages = {45, 10, 30, 50, 20, 40, 80, 70, 60};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{45, 80, 70, 60, 50}));
        }

        // core9
        {
            final int[] cpuUsages = {45, 10, 30, 50, 20, 40, 80, 70, 60, 90};
            final CpuNotificationData[] data = UsageUpdateService.distributeNotificationData(cpuUsages, C.CORE_DISTRIBUTION_MODE_1ICON_SORTED);

            assertThat(data.length, is(1));
            assertThat(data[0].coreNoStart, is(1));
            assertThat(data[0].coreNoEnd, is(4));
            assertThat(data[0].cpuUsages, is(new int[]{45, 90, 80, 70, 60}));
        }
    }

}
