package com.fongmi.android.tv.player.impl;

import android.content.Context;
import android.view.Surface;

import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.bean.Video;
import com.fongmi.android.tv.player.base.IPlayer;
import com.fongmi.android.tv.player.base.PlayerListener;

import java.util.ArrayList;
import java.util.List;

import is.xyz.mpv.MPVLib;

public class MpvPlayer implements IPlayer, MPVLib.EventObserver {

    private final MPVLib mpv;
    private PlayerListener listener;
    private Surface surface;
    private boolean released;

    public MpvPlayer(Context context) {
        this.mpv = new MPVLib();
        mpv.create(context);
        mpv.setEventObserver(this);
        released = false;
        initConfig();
    }

    private void initConfig() {
        // 渲染与硬解
        mpv.setOption("vo", "android");
        mpv.setOption("gpu-context", "android");
        mpv.setOption("hwdec", "auto");
        mpv.setOption("hwdec-codecs", "all");

        // 网络与缓存
        mpv.setOption("cache", "yes");
        mpv.setOption("cache-size", "153600");
        mpv.setOption("cache-secs", "30");
        mpv.setOption("demuxer-readahead-secs", "60");
        mpv.setOption("network-timeout", "20");

        // 音视频同步
        mpv.setOption("video-sync", "audio");
        mpv.setOption("correct-pts", "yes");

        // 倍速不变调
        mpv.setOption("scaletempo", "yes");
        mpv.setOption("audio-pitch-correction", "yes");

        // 字幕
        mpv.setOption("sub-auto", "yes");
        mpv.setOption("sub-pos", "95");
    }

    @Override
    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        if (surface != null) mpv.setSurface(surface);
        else mpv.setSurface(null);
    }

    @Override
    public void setDataSource(String path) {
        mpv.command(new String[]{"loadfile", path});
    }

    @Override
    public void prepare() {}

    @Override
    public void start() {
        mpv.setProperty("pause", false);
    }

    @Override
    public void pause() {
        mpv.setProperty("pause", true);
    }

    @Override
    public void stop() {
        mpv.command(new String[]{"stop"});
    }

    @Override
    public void release() {
        released = true;
        mpv.destroy();
    }

    @Override
    public void seekTo(long ms) {
        mpv.setProperty("time-pos", ms / 1000.0);
    }

    @Override
    public long getCurrentPosition() {
        return (long) (mpv.getPropertyNumber("time-pos") * 1000);
    }

    @Override
    public long getDuration() {
        return (long) (mpv.getPropertyNumber("duration") * 1000);
    }

    @Override
    public boolean isPlaying() {
        if (released) return false;
        return mpv.getPropertyBoolean("playtime-remaining") && !mpv.getPropertyBoolean("pause");
    }

    @Override
    public void setSpeed(float speed) {
        mpv.setProperty("speed", speed);
    }

    @Override
    public float getSpeed() {
        return (float) mpv.getPropertyNumber("speed");
    }

    @Override
    public void setVideo(Video video) {}

    @Override
    public List<Track> getTracks(int type) {
        return new ArrayList<>();
    }

    @Override
    public void setTrack(int type, int index) {
        try {
            if (type == Track.TYPE_AUDIO) {
                mpv.setProperty("aid", index);
            } else if (type == Track.TYPE_SUBTITLE) {
                mpv.setProperty("sid", index);
            } else if (type == Track.TYPE_VIDEO) {
                mpv.setProperty("vid", index);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void addSubtitle(String path) {
        try {
            mpv.command(new String[]{"sub-add", path, "select"});
        } catch (Exception ignored) {}
    }

    @Override
    public void setSubVisible(boolean visible) {
        mpv.setProperty("sub-visibility", visible ? "yes" : "no");
    }

    @Override
    public void onEvent(int event) {
        if (listener == null || released) return;
        switch (event) {
            case MPVLib.EVENT_START_FILE:
                listener.onPrepared();
                break;
            case MPVLib.EVENT_PLAYBACK_RESTART:
            case MPVLib.EVENT_BUFFERING:
                listener.onInfo();
                break;
            case MPVLib.EVENT_END_FILE:
                listener.onCompletion();
                break;
            case MPVLib.EVENT_ERROR:
                listener.onError(-100, "MPV 播放错误");
                break;
        }
    }
}
