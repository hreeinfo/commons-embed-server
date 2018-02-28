package com.hreeinfo.commons.embed.server.support;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Realm;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.jasper.servlet.JasperInitializer;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/27 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class TomcatServerImpl implements TomcatServer {
    private static final Logger LOG = Logger.getLogger(TomcatServerImpl.class.getName());
    private static final int TEMP_DIR_ATTEMPTS = 10000;
    private final Tomcat tomcat;
    private Context context;
    private Connector connector;
    private final Lock statLock = new ReentrantLock();
    private final AtomicBoolean stopped = new AtomicBoolean(true);
    private String home;

    public TomcatServerImpl() {
        this.tomcat = new Tomcat();
        this.stopped.set(true);
    }

    @Override
    public Tomcat getEmbedded() {
        return this.tomcat;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public Connector getConnector() {
        return this.connector;
    }

    @Override
    public void createContext(String contextPath, String webAppPath) {
        if (StringUtils.isBlank(this.home)) this.home = createTempDir().getAbsolutePath();

        if (StringUtils.isNotBlank(webAppPath)) {
            this.context = tomcat.addWebapp(null, getFullContextPath(contextPath), webAppPath);
        } else {
            this.context = tomcat.addContext(getFullContextPath(contextPath), this.home);
        }

        StandardRoot sr = new StandardRoot(this.context);
        this.context.setResources(sr);

        this.context.setReloadable(true); // 默认 reloadable
        this.context.addServletContainerInitializer(new JasperInitializer(), null);
    }

    @Override
    public void createLoader(ClassLoader classLoader) {
        WebappLoader wl = new WebappLoader(classLoader);
        this.context.setLoader(wl);
    }

    @Override
    public void setHome(String home) {
        this.home = home;
    }

    @Override
    public void setRealm(Realm realm) {
        this.tomcat.getEngine().setRealm(realm);
    }

    @Override
    public void configureContainer() {
        this.tomcat.enableNaming();
    }

    @Override
    public void configureHttpConnector(int port, String uriEncoding, String protocolHandlerClassName) {
        String phc = StringUtils.isBlank(protocolHandlerClassName) ? DEFAULT_PROTOCOL_HANDLER_NAME : protocolHandlerClassName;
        Connector conn = new Connector(phc);
        conn.setPort(port);
        conn.setURIEncoding(uriEncoding);

        this.connector = conn;

        if (tomcat.getConnector() != null) this.tomcat.getService().removeConnector(this.tomcat.getConnector());
        this.tomcat.getService().addConnector(this.connector);

    }

    @Override
    public void configureDefaultWebXml(File webDefaultXml) {
        if (webDefaultXml != null && webDefaultXml.exists()) {
            if (this.context instanceof StandardContext) {
                ((StandardContext) this.context).setDefaultWebXml(webDefaultXml.getAbsolutePath());
            } else LOG.warning("未设置 webDefaultXml 原因：context 不是 StandardContext");
        }
    }

    @Override
    public void configureUser(String username, String password, String... roles) {
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            this.tomcat.addUser(username, password);
        }

        if (roles != null) for (String r : roles) this.tomcat.addRole(username, r);
    }

    @Override
    public void setConfigFile(URL configFile) {
        if (configFile != null) this.context.setConfigFile(configFile);
    }

    @Override
    public void setCacheSize(int cacheSize) {
        if (cacheSize > 0) {
            context.getResources().setCacheMaxSize(cacheSize);
        } else if (cacheSize < 0) {
            context.getResources().setCacheMaxSize(16 * 1024);
            context.getResources().setCachingAllowed(false);
        }
    }

    @Override
    public void setReloadable(boolean reloadable) {
        this.context.setReloadable(false);
    }

    @Override
    public void addWebappClassPathResource(File resource) {
        if (resource != null) {
            try {
                URL rui = resource.toURI().toURL();
                this.context.getResources().createWebResourceSet(ResourceSetType.PRE, "/WEB-INF/classes", rui, "/");
            } catch (Throwable e) {
                LOG.warning("无法转换 URI -> " + resource);
            }
        }
    }

    @Override
    public void addWebappWebappResource(String webAppMountPoint, File resource) {
        if (StringUtils.isBlank(webAppMountPoint)) webAppMountPoint = "/";
        if (resource != null && resource.exists()) {
            try {
                URL rui = resource.toURI().toURL();
                this.context.getResources().createWebResourceSet(ResourceSetType.PRE, webAppMountPoint, rui, "/");
            } catch (Throwable e) {
                LOG.warning("无法转换 URI -> " + resource);
            }
        }
    }

    @Override
    public void addLifecycleListener(LifecycleListener lifecycleListener) {
        if (lifecycleListener != null) this.tomcat.getServer().addLifecycleListener(lifecycleListener);
    }

    @Override
    public void start() {
        try {
            this.statLock.lock();
            this.tomcat.start();
            this.stopped.set(false);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Tomcat 启动过程发生错误", e);
            this.stopped.set(true);
            try {
                statLock.unlock();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void stop() {
        try {
            this.tomcat.stop();
            this.statLock.unlock();
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Tomcat 停止过程发生错误", e);
        } finally {
            this.stopped.set(true);
        }
    }

    @Override
    public void waitTomcat() {
        for (int i = 0; i < 60; i++) { // 确保启动后等待
            if (!this.stopped.get()) break;
            try {
                Thread.sleep(500L);
            } catch (Throwable e) {
            }
        }

        if (this.stopped.get()) throw new IllegalStateException("Tomcat 未启动");

        try {
            this.tomcat.getServer().await();
        } catch (Throwable e) {
        } finally {
            this.stop();
        }
    }

    @Override
    public boolean isStopped() {
        return this.stopped.get();
    }

    private static String getFullContextPath(String contextPath) {
        contextPath = StringUtils.trim(contextPath);
        if (StringUtils.isBlank(contextPath) || StringUtils.equals(contextPath, "/")) return "";
        return (StringUtils.startsWith(contextPath, "/")) ? contextPath : ("/" + contextPath);
    }

    public static File createTempDir() {
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
}
