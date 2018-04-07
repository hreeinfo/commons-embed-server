package com.hreeinfo.commons.embed.server.internal;

import com.hreeinfo.commons.embed.server.EmbedServer;

import java.util.concurrent.Future;

/**
 * 当未找到可用实例时 使用本实例作为服务对象返回值 此服务仅打印警告信息
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/23 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class InternalNullServer implements EmbedServer {
    @Override
    public String getType() {
        return "";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void setServerContextLoader(ClassLoader classLoader) {
    }

    @Override
    public void start() throws RuntimeException {

    }

    @Override
    public void start(boolean daemon) throws RuntimeException {

    }

    @Override
    public void start(ClassLoader parentLoader, boolean daemon) throws RuntimeException {

    }

    @Override
    public Future<?> start(ClassLoader parentLoader, boolean daemon, boolean daemonThread) throws RuntimeException {
        return null;
    }

    @Override
    public void reload() {

    }

    @Override
    public void stop() {

    }
}
