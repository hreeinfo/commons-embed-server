package com.hreeinfo.commons.embed.server.internal;

import com.hreeinfo.commons.embed.server.EmbedServer;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/27 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class InternalLifeCycleListener implements EmbedServer.LifeCycleListener {
    private final List<Runnable> startingCmds = new ArrayList<>();
    private final List<Runnable> startedCmds = new ArrayList<>();
    private final List<Runnable> failureCmds = new ArrayList<>();
    private final List<Runnable> stoppingCmds = new ArrayList<>();
    private final List<Runnable> stoppedCmds = new ArrayList<>();

    public void add(String type, Runnable cmd) {
        if (type == null || cmd == null) return;
        type = type.toUpperCase();

        switch (type) {
            case "STARTING":
                this.startingCmds.add(cmd);
                break;
            case "STARTED":
                this.startedCmds.add(cmd);
                break;
            case "FAILURE":
                this.failureCmds.add(cmd);
                break;
            case "STOPPING":
                this.stoppingCmds.add(cmd);
                break;
            case "STOPPED":
                this.stoppedCmds.add(cmd);
                break;
            case "ONSTARTING":
                this.startingCmds.add(cmd);
                break;
            case "ONSTARTED":
                this.startedCmds.add(cmd);
                break;
            case "ONFAILURE":
                this.failureCmds.add(cmd);
                break;
            case "ONSTOPPING":
                this.stoppingCmds.add(cmd);
            case "ONSTOPPED":
                this.stoppedCmds.add(cmd);
            default:
                break;
        }
    }

    @Override
    public void onStarting(EmbedServer server) {
        this.startingCmds.forEach(Runnable::run);
    }

    @Override
    public void onStarted(EmbedServer server) {
        this.startedCmds.forEach(Runnable::run);
    }

    @Override
    public void onFailure(EmbedServer server, Throwable cause) {
        this.failureCmds.forEach(Runnable::run);
    }

    @Override
    public void onStopping(EmbedServer server) {
        this.stoppingCmds.forEach(Runnable::run);
    }

    @Override
    public void onStopped(EmbedServer server) {
        this.stoppedCmds.forEach(Runnable::run);
    }
}
