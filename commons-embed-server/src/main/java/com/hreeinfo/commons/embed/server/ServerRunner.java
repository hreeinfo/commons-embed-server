package com.hreeinfo.commons.embed.server;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public interface ServerRunner {
    public void start() throws RuntimeException;

    public void stop();
}
