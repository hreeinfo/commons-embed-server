package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Jetty Server 实现
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/23 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class EmbedJettyServer extends BaseEmbedServer {
    private static final Logger LOG = Logger.getLogger(EmbedJettyServer.class.getName());

    public static final String TYPE_NAME = "JETTY";

    /**
     * 值为true时，使用jetty内置的classpath来配置classpathdirs，而不是使用当前线程的类路径
     */
    public static final String OPTION_CLASSPATH_JETTY = "jetty_classpaths";

    /**
     * 重写jetty所需的 configurations 每个configuration为完整类名，多个值之间用,分隔
     * <p>
     * 如果以包含*，仅增加额外的配置，出现*的位置用默认实例填充
     * <p>
     * 否则以当前定义值重写整体configurations
     */
    public static final String OPTION_CONFIGURATIONS = "jetty_configurations";

    private volatile WebAppContext handler;
    private volatile Server server;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    public WebAppContext getHandler() {
        return handler;
    }

    public Server getServer() {
        return server;
    }

    protected void initServerHandler(Server initServer, WebAppContext initHandler, ClassLoader loader) {
        if (initHandler == null || initServer == null) return;

        initServer.setHandler(initHandler);


        try {
            if (System.getProperty("org.eclipse.jetty.LEVEL") == null || System.getProperty("org.eclipse.jetty.LEVEL").equals("")) {
                System.setProperty("org.eclipse.jetty.LEVEL", (StringUtils.isBlank(this.getLoglevel()) ? "INFO" : this.getLoglevel()));
            }

            Properties logprops = new Properties();
            logprops.put("org.eclipse.jetty.LEVEL", (StringUtils.isBlank(this.getLoglevel()) ? "INFO" : this.getLoglevel()));

            Log.setLog(new StdErrLog(null, logprops));

            this.configHandler(initHandler, loader);
            this.configServer(initServer);
        } catch (Throwable e) {
            throw new IllegalStateException("无法创建 Server : " + e.getMessage(), e);
        }
    }

    /**
     * 默认配置
     */
    protected List<Configuration> createDefaultJettyConfigurations() {
        final WebXmlConfiguration webXmlConfiguration = new WebXmlConfiguration();
        final WebInfConfiguration webInfConfiguration = new WebInfConfiguration();
        final PlusConfiguration plusConfiguration = new PlusConfiguration();
        final MetaInfConfiguration metaInfConfiguration = new MetaInfConfiguration();
        final FragmentConfiguration fragmentConfiguration = new FragmentConfiguration();
        final EnvConfiguration envConfiguration = new EnvConfiguration();
        final AnnotationConfiguration annotationConfiguration = new AnnotationConfiguration();
        final JettyWebXmlConfiguration jettyWebXmlConfiguration = new JettyWebXmlConfiguration();

        return Arrays.asList(new Configuration[]{
                webXmlConfiguration,
                webInfConfiguration,
                plusConfiguration,
                metaInfConfiguration,
                fragmentConfiguration,
                envConfiguration,
                annotationConfiguration,
                jettyWebXmlConfiguration
        });
    }

    /**
     * 创建 Configurations
     *
     * @return
     */
    protected final Configuration[] createJettyConfigurations() {
        List<Configuration> configurations = new ArrayList<>();

        String cfgstr = StringUtils.trim(this.option(OPTION_CONFIGURATIONS));

        if (StringUtils.isBlank(cfgstr) || StringUtils.equals(cfgstr, "*")) {
            configurations.addAll(this.createDefaultJettyConfigurations());
        } else {
            String[] cs = StringUtils.split(cfgstr, ",");
            if (cs != null) for (String c : cs) {
                c = StringUtils.trim(c);
                if (StringUtils.isBlank(c)) continue; // 为空忽略

                if (StringUtils.equals(c, "*")) {
                    configurations.addAll(this.createDefaultJettyConfigurations()); // * 表示默认
                } else try {
                    Class<?> ct = Class.forName(c);
                    if (ct == null) continue;

                    Object cto = ct.newInstance();
                    if (cto != null && (cto instanceof Configuration)) configurations.add((Configuration) cto);
                    else throw new IllegalStateException("Configuration类型无法兼容此对象");
                } catch (Throwable e) {
                    LOG.severe("无法实例化类 " + c + " - " + e.getMessage());
                }
            }
        }

        return configurations.toArray(new Configuration[configurations.size()]);
    }

    /**
     * TODO 需要实现configfile机制
     * @param initHandler
     * @param loader
     * @throws Exception
     */
    private void configHandler(WebAppContext initHandler, ClassLoader loader) throws Exception {
        initHandler.setContextPath(this.getFullContextPath(true));
        if (StringUtils.isNotBlank(this.getWorkingdir())) initHandler.setTempDirectory(new File(this.getWorkingdir()));

        Resource baseResource = Resource.newResource(this.getWebapp());

        if (this.getResourcesdirs().isEmpty()) {
            initHandler.setBaseResource(baseResource);
        } else {
            List<Resource> allrs = new ArrayList<>();
            allrs.add(baseResource);

            for (String r : this.getResourcesdirs()) {
                allrs.add(Resource.newResource(r));
            }

            ResourceCollection baseRCS = new ResourceCollection(allrs.toArray(new Resource[allrs.size()]));

            initHandler.setBaseResource(baseRCS);
        }

        initHandler.setParentLoaderPriority(true);
        List<String> allECPs = new ArrayList<>();

        // TODO 需要统一 classpath webapp resource  载入顺序

        if (!this.isUseThreadContextLoader()) allECPs.addAll(this.getClassesdirs()); // 此时使用jetty内置的classpath解析机制

        File webappFile = new File(this.getWebapp());
        if (webappFile.exists()) {
            if (this.isWar()) initHandler.setWar(this.getWebapp());
            else allECPs.add(this.getWebapp());
        } else throw new IllegalStateException("webapp 不存在");

        allECPs.addAll(this.getResourcesdirs());

        initHandler.setExtraClasspath(String.join(";", allECPs));

        initHandler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/build/classes/.*");

        initHandler.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");

        initHandler.setConfigurations(this.createJettyConfigurations());

        if (loader == null) loader = EmbedJettyServer.class.getClassLoader();

        initHandler.setClassLoader(new WebAppClassLoader(loader, initHandler));
    }

    private void configServer(Server initServer) {
        final List<LifeCycleListener> lns = new ArrayList<>();
        lns.add(new LogListener(LOG));
        lns.addAll(this.getListeners());

        final EmbedJettyServer cur = this;
        initServer.addLifeCycleListener(new LifeCycle.Listener() {
            @Override
            public void lifeCycleStarting(LifeCycle event) {
                lns.forEach(e -> e.onStarting(cur));
            }

            @Override
            public void lifeCycleStarted(LifeCycle event) {
                lns.forEach(e -> e.onStarted(cur));
            }

            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) {
                lns.forEach(e -> e.onFailure(cur, cause));
            }

            @Override
            public void lifeCycleStopping(LifeCycle event) {
                lns.forEach(e -> e.onStopping(cur));
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                lns.forEach(e -> e.onStopped(cur));
            }
        });
    }

    @Override
    protected boolean isUseThreadContextLoader() {
        return !this.optionBoolean(OPTION_CLASSPATH_JETTY);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doServerInit(ClassLoader loader, Consumer<EmbedServer> config) throws RuntimeException {
        if (this.server == null) {
            Server initServer = new Server((this.getPort() <= 0) ? 8080 : this.getPort());
            WebAppContext initHandler = new WebAppContext();

            this.initServerHandler(initServer, initHandler, loader);

            this.server = initServer;
            this.handler = initHandler;
            if (config != null) config.accept(this);
        }
    }

    @Override
    protected void doServerStart() throws RuntimeException {
        if (this.server == null) throw new IllegalStateException("Jetty Server 未初始化");

        try {
            this.server.start();
        } catch (Exception e) {
            throw new IllegalStateException("启动服务发生错误", e);
        }
    }

    @Override
    protected void doServerWait() throws RuntimeException {
        if (this.server == null) throw new IllegalStateException("Jetty Server 未初始化");
        try {
            this.server.join();
        } catch (Exception e) {
            throw new IllegalStateException("等待服务发生错误", e);
        }
    }

    @Override
    protected void doServerStop() throws RuntimeException {
        if (this.server == null) throw new IllegalStateException("Jetty Server 未初始化");
        try {
            this.server.stop();
        } catch (Exception e) {
            throw new IllegalStateException("停止服务发生错误", e);
        }
    }
}
