package com.hreeinfo.commons.embed.server;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/23 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public abstract class BaseEmbedServer implements EmbedServer {
    private static final Logger LOG = Logger.getLogger(BaseEmbedServer.class.getName());
    private static final int TEMP_DIR_ATTEMPTS = 10000;
    private final AtomicBoolean running = new AtomicBoolean(Boolean.FALSE);
    private final Lock optLock = new ReentrantLock();

    private int port = 8080;
    private String context = "";
    private String webapp = "";
    private boolean war;
    private String workingdir = "";
    private final List<String> classesdirs = new ArrayList<>();
    private final List<String> resourcesdirs = new ArrayList<>();
    private String configfile = "";
    private String loglevel = "INFO";
    private final Map<String, String> options = new LinkedHashMap<>();
    private final List<LifeCycleListener> listeners = new ArrayList<>();
    private Consumer<EmbedServer> config;

    public int getPort() {
        return port;
    }

    public String getContext() {
        return context;
    }

    public String getWebapp() {
        return webapp;
    }

    public boolean isWar() {
        return war;
    }

    public String getWorkingdir() {
        return workingdir;
    }

    public List<String> getClassesdirs() {
        return classesdirs;
    }

    public List<String> getResourcesdirs() {
        return resourcesdirs;
    }

    public String getConfigfile() {
        return configfile;
    }

    public String getLoglevel() {
        return loglevel;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public List<LifeCycleListener> getListeners() {
        return listeners;
    }

    public Consumer<EmbedServer> getConfig() {
        return config;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setWebapp(String webapp) {
        this.webapp = webapp;
    }

    public void setWar(boolean war) {
        this.war = war;
    }

    public void setConfigfile(String configfile) {
        this.configfile = configfile;
    }

    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    public void setWorkingdir(String workingdir) {
        this.workingdir = workingdir;
    }

    public void setConfig(Consumer<EmbedServer> config) {
        this.config = config;
    }

    /**
     * 获取属性值
     *
     * @param key
     * @return
     */
    public String option(String key) {
        if (StringUtils.isBlank(key)) return null;
        return this.options.get(key);
    }

    /**
     * 获取以 keyPrefix 开头的配置项目
     *
     * @param keyPrefix
     * @return
     */
    public Map<String, String> options(String keyPrefix) {
        final Map<String, String> mes = new LinkedHashMap<>();
        if (StringUtils.isBlank(keyPrefix)) return mes;

        this.options.forEach((key, value) -> {
            if (StringUtils.equalsIgnoreCase(key, keyPrefix) || StringUtils.startsWithIgnoreCase(key, keyPrefix)) {
                mes.put(key, value);
            }
        });

        return mes;
    }

    public int optionInt(String key) {
        String v = this.option(key);
        if (v == null) return 0;

        try {
            return Integer.valueOf(v);
        } catch (Throwable e) {
        }

        return 0;
    }

    public boolean optionBoolean(String key) {
        String v = this.option(key);
        if (v == null) return false;

        return StringUtils.equalsIgnoreCase(v, "true") || StringUtils.equalsIgnoreCase(v, "t")
                || StringUtils.equalsIgnoreCase(v, "yes") || StringUtils.equalsIgnoreCase(v, "y")
                || StringUtils.equalsIgnoreCase(v, "1");
    }

    /**
     * 是否包含给定的属性和值，用于判断
     *
     * @param key
     * @param value
     */
    public boolean optionMatch(String key, String value) {
        if (StringUtils.isBlank(key)) return true;
        String ov = this.option(key);
        if (ov == null && value == null) return true;
        else return StringUtils.equalsIgnoreCase(key, value);
    }


    /**
     * 获取 contextPath
     *
     * @param sstart 是否以 / 开头
     * @return
     */
    protected String getFullContextPath(boolean sstart) {
        if (sstart) {
            if (StringUtils.isBlank(this.getContext())) return "/";
            else if (StringUtils.startsWith(this.getContext(), "/")) return this.getContext();
            else return "/" + this.getContext();
        } else {
            if (StringUtils.isBlank(this.getContext()) || StringUtils.equals(this.getContext(), "/")) return "";
            else if (StringUtils.startsWith(this.getContext(), "/"))
                return StringUtils.substringAfter(this.getContext(), "/");
            else return this.getContext();
        }
    }

    /**
     * 创建临时目录
     *
     * @return
     */
    protected static File createTempDir() {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create directory within "
                + TEMP_DIR_ATTEMPTS + " attempts (tried "
                + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }


    protected ClassLoader createClassLoader(List<String> classpathFiles, ClassLoader loader) {
        if (classpathFiles == null || classpathFiles.isEmpty()) return null;
        return new URLClassLoader(toURLArray(classpathFiles), loader);
    }

    protected URL[] toURLArray(List<String> files) {
        final List<URL> urls = new ArrayList<>();
        if (files != null) for (String f : files) {
            if (StringUtils.isBlank(f)) continue;
            try {
                URL u = null;

                if (StringUtils.contains(f, ":")) u = new URL(f);
                else u = new File(f).toURI().toURL();

                urls.add(u);
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "解析文件URL发生错误", e);
            }
        }

        return urls.toArray(new URL[urls.size()]);
    }

    protected boolean checkEmbedServer() {
        if (this.getPort() < 1) {
            LOG.severe("检测环境失败：端口号 " + this.getPort() + " 无效");
            return false;
        }

        if (StringUtils.isBlank(this.getWebapp()) || !(new File(this.getWebapp()).exists())) {
            LOG.severe("检测环境失败：WebApp 无效 " + this.getWebapp());
            return false;
        }

        return true;
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    public final void start() throws RuntimeException {
        this.start(null, false);
    }

    @Override
    public final void start(boolean daemon) throws RuntimeException {
        this.start(null, daemon);
    }

    @Override
    public final void start(ClassLoader parentLoader, boolean daemon) throws RuntimeException {
        if (!this.checkEmbedServer()) throw new IllegalArgumentException("EMBED SERVER 变量配置检测失败，请核对参数");

        try {
            this.optLock.lock();
            this.running.set(true);

            Thread serverThread = new Thread(() -> {
                if (isUseThreadContextLoader()) {
                    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

                    ClassLoader cl = this.createClassLoader(this.getClassesdirs(), originalClassLoader);
                    if (cl != null) Thread.currentThread().setContextClassLoader(cl);

                    this.doServerInit(cl, this.config);
                    this.doServerStart();
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                } else {
                    this.doServerInit(Thread.currentThread().getContextClassLoader(), this.config);
                    this.doServerStart();
                }
            });

            serverThread.run();

            if (!daemon) this.doServerWait();
        } catch (Throwable e) {
            this.running.set(false);
            throw new IllegalStateException(e);
        } finally {
            try {
                this.optLock.unlock();
            } catch (Throwable ignored) {
            }
            this.running.set(false);
        }
    }

    @Override
    public final void stop() {
        try {
            this.doServerStop();
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "停止服务错误", e);
        } finally {
            try {
                this.optLock.unlock();
            } catch (Throwable ignored) {
            }
            this.running.set(false);
        }
    }

    /**
     * 是否使用线程的 ContextLoader 配置 classpath
     * <p>
     * 如果此方法返回true 则使用线程 ContextLoader 配置classpath
     * <p>
     * 如果此方法返回false 则需服务器自行设置 classpath
     *
     * @return
     */
    protected abstract boolean isUseThreadContextLoader();

    protected abstract void doServerInit(ClassLoader loader, Consumer<EmbedServer> config) throws RuntimeException;

    protected abstract void doServerStart() throws RuntimeException;

    protected abstract void doServerWait() throws RuntimeException;

    protected abstract void doServerStop() throws RuntimeException;


    protected static class LogListener implements LifeCycleListener {
        private final Logger logger;

        public LogListener(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void onStarting(EmbedServer server) {
            if (this.logger == null || server == null) return;
            this.logger.log(Level.FINE, server.getType() + " STARTING");
        }

        @Override
        public void onStarted(EmbedServer server) {
            if (this.logger == null || server == null) return;
            this.logger.log(Level.INFO, server.getType() + " STARTED");
        }

        @Override
        public void onFailure(EmbedServer server, Throwable cause) {
            if (this.logger == null || server == null) return;
            if (cause == null) this.logger.log(Level.SEVERE, server.getType() + " FAILURE ");
            else this.logger.log(Level.SEVERE, server.getType() + " FAILURE: " + cause.getMessage(), cause);
        }

        @Override
        public void onStopping(EmbedServer server) {
            if (this.logger == null || server == null) return;
            this.logger.log(Level.FINE, server.getType() + " STOPPING");
        }

        @Override
        public void onStopped(EmbedServer server) {
            if (this.logger == null || server == null) return;
            this.logger.log(Level.INFO, server.getType() + " STOPPED");
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "type='" + getType() + '\'' +
                ", port=" + port +
                ", context='" + context + '\'' +
                ", webapp='" + webapp + '\'' +
                ", workingdir='" + workingdir + '\'' +
                ", classesdirs=" + classesdirs +
                ", resourcesdirs=" + resourcesdirs +
                ", configfile='" + configfile + '\'' +
                ", loglevel='" + loglevel + '\'' +
                ", options=" + options +
                '}';
    }
}
