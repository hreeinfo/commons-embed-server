package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
import com.hreeinfo.commons.embed.server.internal.InternalFactory;
import com.hreeinfo.commons.embed.server.internal.InternalOptParsers;
import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
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
    public ClassLoader getServerContextLoader() {
        ClassLoader ocl = super.getServerContextLoader();

        if (this.getClassesdirs() == null || this.getClassesdirs().isEmpty()) return ocl;// 没有额外的类路径

        if (ocl == null) ocl = Thread.currentThread().getContextClassLoader();

        return createEmbedServerContextLoader(this.getClassesdirs(), ocl);
    }

    protected PayaraMicroRuntime initPayara(int serverPort, PayaraMicro initMicro, ClassLoader loader) throws RuntimeException {
        try {
            initMicro.setHttpPort(serverPort);
            return initMicro.bootStrap();
        } catch (Throwable e) {
            LOG.severe("无法创建实例 " + e.getMessage());
            throw new IllegalStateException("无法创建实例 " + e.getMessage(), e);
        }
    }

    @Override
    protected void doServerInit(ClassLoader loader, Consumer<EmbedServer> config) throws RuntimeException {
        final int serverPort = (this.getPort() <= 0) ? 8080 : this.getPort();

        this.micro = InternalFactory.inst().get("payara_micro", PayaraMicro.class, PayaraMicro::getInstance);
        this.runtime = InternalFactory.inst().get("payara_runtime_" + serverPort, PayaraMicroRuntime.class,
                () -> this.initPayara(serverPort, this.micro, loader));

        if (config != null) config.accept(this);
    }

    @Override
    protected void doServerStart() throws RuntimeException {
        if (this.micro == null || this.runtime == null) throw new IllegalStateException("Payara实例未初始化");

        if (this.getListeners() != null) this.getListeners().forEach(e -> e.onStarting(this));

        try {
            this.runtime.deploy(this.getType(), this.getFullContextPath(false), new File(this.getWebapp()));
        } catch (Throwable e) {
            LOG.severe("无法创建实例 " + e.getMessage());
            throw new IllegalStateException("无法创建实例 " + e.getMessage(), e);
        }

        if (this.getListeners() != null) this.getListeners().forEach(e -> e.onStarted(this));
    }

    @Override
    protected void doServerWait(Supplier<Boolean> waitWhen) throws RuntimeException {
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


    public static void main(String[] args) {
        if (args == null || args.length < 1) throw new IllegalArgumentException("参数配置错误");

        Builder builder = Builder.builder();
        builder.opts(
                optionParser -> {
                    // 识别额外参数参数
                }, optionSet -> {
                    // 处理识别额外参数参数
                },
                args);

        EmbedPayaraServer es = builder.build(EmbedPayaraServer.class, server -> {
            LOG.info("EmbedJettyServer 已载入");
            // 增加其他初始化配置
        });

        if (es == null) throw new IllegalStateException("无法载入 EmbedPayaraServer");

        boolean failed = false;
        try {
            es.start(Thread.currentThread().getContextClassLoader(), false, false);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "EmbedPayaraServer 运行错误" + e.getMessage(), e);
            failed = true;
        } finally {
            es.stop();
        }

        System.exit(failed ? 2 : 0);
    }
}
