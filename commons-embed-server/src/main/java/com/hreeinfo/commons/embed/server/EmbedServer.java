package com.hreeinfo.commons.embed.server;

import com.hreeinfo.commons.embed.server.internal.InternalLifeCycleListener;
import com.hreeinfo.commons.embed.server.internal.InternalNullServer;
import com.hreeinfo.commons.embed.server.internal.InternalOptParsers;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.File;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/23 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public interface EmbedServer {
    public String getType();

    public boolean isRunning();


    public void setServerContextLoader(ClassLoader classLoader);

    /**
     * 启动服务（前台等待）
     */
    public void start() throws RuntimeException;

    /**
     * 启动服务
     *
     * @param daemon 是否以后台方式启动 已后台方式启动时会立即返回 服务在独立线程中执行
     */
    public void start(boolean daemon) throws RuntimeException;

    /**
     * 启动服务
     *
     * @param parentLoader
     */
    public void start(ClassLoader parentLoader, boolean daemon) throws RuntimeException;

    /**
     * 启动服务 其中服务运行在独立的守护线程中
     * <p>
     * 当 daemonThread=true 返回 Future 以便操作服务状态
     *
     * @param parentLoader
     * @param daemon
     * @param daemonThread
     * @return
     * @throws RuntimeException
     */
    public Future<?> start(ClassLoader parentLoader, boolean daemon, boolean daemonThread) throws RuntimeException;

    /**
     * 停止服务 停止服务前指定给定操作
     */
    public void stop();

    public static final class Builder {
        private static final Logger LOG = Logger.getLogger(Builder.class.getName());
        private int port = 8080;
        private String context = "";
        private String webapp = "";
        private int war = 0;
        private String workingdir = "";
        private String lockfile = "";
        private final List<String> classesdirs = new ArrayList<>();
        private final List<String> resourcesdirs = new ArrayList<>();
        private String configfile = "";
        private String loglevel = "INFO";
        private final Map<String, String> options = new LinkedHashMap<>();
        private final List<LifeCycleListener> listeners = new ArrayList<>();
        private Consumer<EmbedServer> config;

        private final List<String> serverClasspaths = new ArrayList<>();

        private final InternalLifeCycleListener innerListener = new InternalLifeCycleListener();

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder webapp(String webapp) {
            this.webapp = webapp;
            this.war = 0;
            return this;
        }

        public Builder webapp(String webapp, boolean war) {
            this.webapp = webapp;
            this.war = (war) ? 1 : -1;
            return this;
        }

        public Builder workingdir(String workingdir) {
            this.workingdir = workingdir;
            return this;
        }

        public Builder configfile(String configfile) {
            this.configfile = configfile;
            return this;
        }

        public Builder lockfile(String lockfile) {
            this.lockfile = lockfile;
            return this;
        }

        public Builder loglevel(String loglevel) {
            this.loglevel = loglevel;
            return this;
        }

        public Builder classesdir(String... dirs) {
            if (dirs != null) this.classesdirs.addAll(Arrays.asList(dirs));
            return this;
        }

        public Builder resourcedir(String... dirs) {
            if (dirs != null) this.resourcesdirs.addAll(Arrays.asList(dirs));
            return this;
        }

        public Builder listener(LifeCycleListener... lns) {
            if (lns != null) this.listeners.addAll(Arrays.asList(lns));
            return this;
        }

        public Builder listener(String type, Runnable runnable) {
            this.innerListener.add(type, runnable);
            return this;
        }

        public Builder option(String name, String value) {
            if (name != null) this.options.put(name, value);
            return this;
        }

        public Builder options(Map<String, String> opts) {
            if (opts != null) this.options.putAll(opts);
            return this;
        }

        public Builder serverClasspath(String... cps) {
            if (cps != null) this.serverClasspaths.addAll(Arrays.asList(cps));
            return this;
        }

        private void setFields(BaseEmbedServer embedServer, ClassLoader loader) {
            if (embedServer == null) return;


            embedServer.setPort(this.port);
            embedServer.setContext(this.context);
            embedServer.setWebapp(this.webapp);
            embedServer.setWar(this.detectWAR());
            embedServer.setWorkingdir(this.workingdir);
            embedServer.setLockfile(this.lockfile);
            embedServer.setConfigfile(this.configfile);
            embedServer.setLoglevel(this.loglevel);
            embedServer.getClassesdirs().addAll(this.classesdirs);
            embedServer.getResourcesdirs().addAll(this.resourcesdirs);
            embedServer.getOptions().putAll(this.options);

            embedServer.setConfig(this.config);

            embedServer.getListeners().add(this.innerListener);
            embedServer.getListeners().addAll(this.listeners);

            // 此处使用独立的 classloader 以附加 serverClasspaths 定义
            embedServer.setServerContextLoader(loader);
        }

        /**
         * 检测当前的webapp是否为war 只有未指定war值时才进行此判断
         *
         * @return
         */
        private boolean detectWAR() {
            if (this.war > 0) return true;
            else if (this.war < 0) return false;
            else {
                if (StringUtils.isBlank(this.webapp)) return false;
                File wf = new File(this.webapp);
                if (wf.exists() && wf.isFile()) return true;
                else return false;
            }
        }


        /**
         * 构建目标服务对象 如果类型有误（type无效）则返回默认服务对象（此服务什么也不处理仅打印警告）
         *
         * @param type
         * @return 返回总不为空
         * @throws RuntimeException
         */
        public EmbedServer build(String type) throws RuntimeException {
            ClassLoader loader = BaseEmbedServer.createEmbedServerContextLoader(this.serverClasspaths, null);
            BaseEmbedServer fes = null;
            if (StringUtils.isNotBlank(type)) {
                final ServiceLoader<EmbedServer> serverLoader = ServiceLoader.load(EmbedServer.class, loader);

                for (EmbedServer es : serverLoader) {
                    if (es != null
                            && StringUtils.equalsIgnoreCase(type, es.getType())
                            && (es instanceof BaseEmbedServer)) {
                        fes = (BaseEmbedServer) es;
                        break;
                    }
                }
            }

            if (fes == null) return new InternalNullServer();

            this.setFields(fes, loader);

            return fes;
        }


        /**
         * 按类型构建目标服务对象 无法初始化会抛出异常 类型无效则返回空
         *
         * @param serverType
         * @param <T>
         * @return
         * @throws RuntimeException
         */
        public <T extends EmbedServer> T build(Class<T> serverType) throws RuntimeException {
            return build(serverType, null);
        }

        /**
         * 按类型构建目标服务对象 无法初始化会抛出异常 类型无效则返回空
         * <p>
         * 其中可对目标服务进行额外配置，通过configs进行管理
         *
         * @param serverType
         * @param config
         * @param <T>
         * @return
         * @throws RuntimeException
         */
        @SuppressWarnings("unchecked")
        public <T extends EmbedServer> T build(Class<T> serverType, Consumer<T> config) throws RuntimeException {
            ClassLoader loader = BaseEmbedServer.createEmbedServerContextLoader(this.serverClasspaths, null);

            T fes = null;
            try {
                fes = serverType.getConstructor().newInstance();
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "无法构建Server实例 -> " + serverType, e);
            }
            if (fes == null) throw new IllegalArgumentException("无法构建 " + serverType + " 实例");

            if (config != null) {
                this.config = (Consumer<EmbedServer>) config;
            }

            if (fes instanceof BaseEmbedServer) this.setFields((BaseEmbedServer) fes, loader);

            return fes;
        }

        public Builder opts(String[] args) {
            return opts(null, null, args);
        }

        /**
         * 根据命令行参数构建配置对象
         *
         * @param parserConsumer
         * @param optionSetConsumer
         * @param args
         * @return
         */
        public Builder opts(Consumer<OptionParser> parserConsumer, Consumer<OptionSet> optionSetConsumer, String[] args) {
            OptionParser parser = new OptionParser();
            parser.accepts("port").withOptionalArg().ofType(Integer.class);
            parser.accepts("context").withOptionalArg();
            parser.accepts("webapp").withOptionalArg();
            parser.accepts("workingdir").withOptionalArg();
            parser.accepts("lockfile").withOptionalArg();
            parser.accepts("classesdir").withOptionalArg();
            parser.accepts("resourcesdir").withOptionalArg();
            parser.accepts("configfile").withOptionalArg();
            parser.accepts("loglevel").withOptionalArg();
            parser.accepts("classpath").withOptionalArg();
            parser.accepts("serverClasspath").withOptionalArg();
            parser.accepts("option").withOptionalArg();

            if (parserConsumer != null) parserConsumer.accept(parser);

            OptionSet osts = parser.parse((args != null) ? args : new String[]{});
            if (osts == null) return this;

            this.port = InternalOptParsers.optInteger(osts, "port", 8080);
            this.context = InternalOptParsers.optString(osts, "context", "");
            this.webapp = InternalOptParsers.optString(osts, "webapp", "");
            this.workingdir = InternalOptParsers.optString(osts, "workingdir", "");
            this.lockfile = InternalOptParsers.optString(osts, "lockfile", "");
            InternalOptParsers.opt(osts, "classesdir", o -> InternalOptParsers.optAppendPathValues(o, this.classesdirs));
            InternalOptParsers.opt(osts, "resourcesdir", o -> InternalOptParsers.optAppendPathValues(o, this.resourcesdirs));
            this.configfile = InternalOptParsers.optString(osts, "configfile", "");
            this.loglevel = StringUtils.upperCase(InternalOptParsers.optString(osts, "loglevel", "INFO"));
            InternalOptParsers.opt(osts, "classpath", o -> InternalOptParsers.optAppendPathValues(o, this.serverClasspaths));
            InternalOptParsers.opt(osts, "serverClasspath", o -> InternalOptParsers.optAppendPathValues(o, this.serverClasspaths));

            InternalOptParsers.opt(osts, "option", o -> InternalOptParsers.optAppendMaValues(o, this.options));

            if (optionSetConsumer != null) optionSetConsumer.accept(osts);

            return this;
        }


        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                    .append("port", port)
                    .append("context", context)
                    .append("webapp", webapp)
                    .append("war", war)
                    .append("workingdir", workingdir)
                    .append("lockfile", lockfile)
                    .append("classesdirs", classesdirs)
                    .append("resourcesdirs", resourcesdirs)
                    .append("configfile", configfile)
                    .append("loglevel", loglevel)
                    .append("options", options)
                    .append("listeners", listeners)
                    .append("config", config)
                    .append("serverClasspaths", serverClasspaths)
                    .append("innerListener", innerListener)
                    .toString();
        }
    }

    public interface LifeCycleListener extends EventListener {
        public void onStarting(EmbedServer server);

        public void onStarted(EmbedServer server);

        public void onFailure(EmbedServer server, Throwable cause);

        public void onStopping(EmbedServer server);

        public void onStopped(EmbedServer server);
    }
}
