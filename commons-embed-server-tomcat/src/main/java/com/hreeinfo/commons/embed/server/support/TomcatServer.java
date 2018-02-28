package com.hreeinfo.commons.embed.server.support;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.net.URL;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/27 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public interface TomcatServer {
    public final String DEFAULT_PROTOCOL_HANDLER_NAME = "org.apache.coyote.http11.Http11NioProtocol";

    public Tomcat getEmbedded();

    public Context getContext();

    public Connector getConnector();

    public void setHome(String home);

    public void setRealm(Realm realm);

    public void createLoader(ClassLoader classLoader);

    public void createContext(String fullContextPath, String webAppPath);

    public void configureContainer();

    public void configureHttpConnector(int port, String uriEncoding, String protocolHandlerClassName);

    public void configureDefaultWebXml(File webDefaultXml);

    public void configureUser(String username, String password, String... roles);

    public void setConfigFile(URL configFile);

    public void setCacheSize(int cacheSize);

    public void setReloadable(boolean reloadable);

    public void addWebappClassPathResource(File resource);

    public void addWebappWebappResource(String webAppMountPoint, File resource);

    public void addLifecycleListener(LifecycleListener lifecycleListener);

    public void start();

    public void waitTomcat();

    public void stop();

    public boolean isStopped();
}
