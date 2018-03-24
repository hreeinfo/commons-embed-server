package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/24 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class EmbedPayaraServer extends BaseEmbedServer {
    private static final Logger LOG = Logger.getLogger(EmbedPayaraServer.class.getName());
    public static final String TYPE_NAME = "PAYARA";

    private volatile PayaraMicro micro;
    private volatile PayaraMicroRuntime runtime;
    private volatile Thread payaraThread;
    private volatile Thread awaitThread;
    private volatile boolean stopAwait = false;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    protected boolean isUseThreadContextLoader() {
        return true;
    }

    protected PayaraMicroRuntime initPayara(PayaraMicro initMicro, ClassLoader loader) throws RuntimeException {
        try {
            initMicro.setHttpPort(this.getPort());
            return initMicro.bootStrap();
        } catch (Throwable e) {
            LOG.severe("无法创建实例 " + e.getMessage());
            throw new IllegalStateException("无法创建实例 " + e.getMessage(), e);
        }
    }

    @Override
    protected void doServerInit(ClassLoader loader, Consumer<EmbedServer> config) throws RuntimeException {
        if (this.micro == null) {
            PayaraMicro initMicro = PayaraMicro.getInstance();

            PayaraMicroRuntime initRuntime = this.initPayara(initMicro, loader);

            this.micro = initMicro;
            this.runtime = initRuntime;

            if (config != null) config.accept(this);
        }
    }

    @Override
    protected void doServerStart() throws RuntimeException {
        if (this.micro == null || this.runtime == null) throw new IllegalStateException("Payara实例未初始化");

        if (this.getListeners() != null) this.getListeners().forEach(e -> e.onStarting(this));

        ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();

        this.payaraThread = new Thread(() -> {
            if (ctxLoader != null) Thread.currentThread().setContextClassLoader(ctxLoader);

            try {
                this.runtime.deploy(this.getType(), this.getFullContextPath(false), new File(this.getWebapp()));
            } catch (Throwable e) {
                LOG.severe("无法创建实例 " + e.getMessage());
                throw new IllegalStateException("无法创建实例 " + e.getMessage(), e);
            }
        });

        this.payaraThread.run();

        if (this.getListeners() != null) this.getListeners().forEach(e -> e.onStarted(this));
    }

    @Override
    protected void doServerWait() throws RuntimeException {
        try {
            this.stopAwait = false;
            this.awaitThread = Thread.currentThread();
            while (!this.stopAwait) {
                try {
                    Thread.sleep(5000);
                } catch (Throwable ignored) {
                }
            }
        } finally {
            this.awaitThread = null;
            this.stopAwait = true;
        }
    }

    @Override
    protected void doServerStop() throws RuntimeException {
        if (this.getListeners() != null) this.getListeners().forEach(e -> e.onStopping(this));

        try {
            this.stopAwait = true;
            this.runtime.shutdown();
        } catch (Throwable e) {
            LOG.severe("无法停止实例 " + e.getMessage());
        } finally {
            try {
                this.micro.shutdown();
                if (this.payaraThread != null && this.payaraThread.isAlive()) {
                    Thread.sleep(5000);
                    if (this.payaraThread.isAlive()) this.payaraThread.interrupt();
                }
            } catch (Throwable ignored) {
            }
        }

        if (this.getListeners() != null) this.getListeners().forEach(e -> e.onStopped(this));
    }
}
