package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.ServerRunnerOpt;
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
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Jetty 服务
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class JettyServerRunner extends BaseServerRunner {
    private static final Logger LOG = Logger.getLogger(JettyServerRunner.class.getName());
    public static final String SERVER_STARTING_TOKEN = "Jetty starting";
    public static final String SERVER_STARTED_TOKEN = "Jetty started";
    public static final String SERVER_STOPPING_TOKEN = "Jetty stopping";
    public static final String SERVER_STOPPED_TOKEN = "Jetty stopped";
    public static final String SERVER_FAILED_TOKEN = "Jetty error";

    private final Object lock = new Object();
    private final WebAppContext handler;
    private final Server server;

    public JettyServerRunner(ServerRunnerOpt opt) {
        super(opt);

        LOG.info("启动参数为：" + this.opt);

        this.server = new Server((this.opt.getPort() <= 0) ? 8080 : this.opt.getPort());
        this.handler = new WebAppContext();

        //this.handler.setServer(this.server);

        this.server.setHandler(this.handler);


        try {
            if (System.getProperty("org.eclipse.jetty.LEVEL") == null || System.getProperty("org.eclipse.jetty.LEVEL").equals("")) {
                System.setProperty("org.eclipse.jetty.LEVEL", (StringUtils.isBlank(this.opt.getLoglevel()) ? "INFO" : this.opt.getLoglevel()));
            }

            Properties logprops = new Properties();
            logprops.put("org.eclipse.jetty.LEVEL", (StringUtils.isBlank(this.opt.getLoglevel()) ? "INFO" : this.opt.getLoglevel()));

            Log.setLog(new StdErrLog(null, logprops));

            this.configHandler();
            this.configServer();
        } catch (Throwable e) {
            throw new IllegalStateException("无法创建 Server : " + e.getMessage(), e);
        }
    }

    private void configHandler() throws Exception {
        if (StringUtils.isBlank(this.opt.getContext())) this.handler.setContextPath("/");
        else if (StringUtils.startsWith(this.opt.getContext(), "/")) this.handler.setContextPath(this.opt.getContext());
        else this.handler.setContextPath("/" + this.opt.getContext());

        Resource baseResource = Resource.newResource(this.opt.getWebappdir());

        if (this.opt.getResourcesdirs().isEmpty()) {
            this.handler.setBaseResource(baseResource);
        } else {
            List<Resource> allrs = new ArrayList<>();
            allrs.add(baseResource);

            for (String r : this.opt.getResourcesdirs()) {
                allrs.add(Resource.newResource(r));
            }

            ResourceCollection baseRCS = new ResourceCollection(allrs.toArray(new Resource[allrs.size()]));

            this.handler.setBaseResource(baseRCS);
        }

        this.handler.setParentLoaderPriority(true);
        List<String> allECPs = new ArrayList<>();

        allECPs.addAll(this.opt.getClassesdirs());
        allECPs.addAll(this.opt.getResourcesdirs());
        if (new File(this.opt.getWebappdir()).exists()) allECPs.add(this.opt.getWebappdir());

        this.handler.setExtraClasspath(String.join(";", allECPs));

        this.handler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/build/classes/.*");

        this.handler.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");

        final WebXmlConfiguration webXmlConfiguration = new WebXmlConfiguration();
        final WebInfConfiguration webInfConfiguration = new WebInfConfiguration();
        final PlusConfiguration plusConfiguration = new PlusConfiguration();
        final MetaInfConfiguration metaInfConfiguration = new MetaInfConfiguration();
        final FragmentConfiguration fragmentConfiguration = new FragmentConfiguration();
        final EnvConfiguration envConfiguration = new EnvConfiguration();
        final AnnotationConfiguration annotationConfiguration = new AnnotationConfiguration();
        final JettyWebXmlConfiguration jettyWebXmlConfiguration = new JettyWebXmlConfiguration();

        // TODO 额外的配置

        this.handler.setConfigurations(new Configuration[]{
                webXmlConfiguration,
                webInfConfiguration,
                plusConfiguration,
                metaInfConfiguration,
                fragmentConfiguration,
                envConfiguration,
                annotationConfiguration,
                jettyWebXmlConfiguration
        });

        this.handler.setClassLoader(new WebAppClassLoader(JettyServerRunner.class.getClassLoader(), this.handler));
    }

    private void configServer() {
        this.server.addLifeCycleListener(new LifeCycle.Listener() {
            @Override
            public void lifeCycleStarting(LifeCycle event) {
                LOG.info(SERVER_STARTING_TOKEN);
            }

            @Override
            public void lifeCycleStarted(LifeCycle event) {
                LOG.info(SERVER_STARTED_TOKEN);
            }

            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) {
                LOG.info(SERVER_FAILED_TOKEN);
            }

            @Override
            public void lifeCycleStopping(LifeCycle event) {
                LOG.info(SERVER_STOPPING_TOKEN);
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                LOG.info(SERVER_STOPPED_TOKEN);
            }
        });

    }

    @Override
    public void start() throws RuntimeException {
        synchronized (this.lock) {
            try {
                this.server.start();
                this.server.join();
            } catch (Throwable e) {
                throw new IllegalStateException("Server 服务启动错误", e);
            }
        }
    }

    @Override
    public void stop() {
        try {
            this.server.stop();
        } catch (Throwable e) {
            LOG.warning("无法停止实例" + e.getMessage());
        }
        LOG.info("已停止服务");
    }
}
