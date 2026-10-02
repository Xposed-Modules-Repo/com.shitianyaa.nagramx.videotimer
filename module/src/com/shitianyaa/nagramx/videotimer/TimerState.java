package com.shitianyaa.nagramx.videotimer;

/**
 * 当前定时状态，供面板与菜单渲染选中态与副标题。
 */
final class TimerState {
    final boolean active;
    final int mode;
    final int remainingMinutes;
    final int selectedMinutes;

    TimerState(boolean active, int mode, int remainingMinutes, int selectedMinutes) {
        this.active = active;
        this.mode = mode;
        this.remainingMinutes = remainingMinutes;
        this.selectedMinutes = selectedMinutes;
    }
}
