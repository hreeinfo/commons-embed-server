package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
import com.hreeinfo.commons.embed.server.internal.InternalFactory;
import com.hreeinfo.commons.embed.server.internal.InternalOptParsers;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanner;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class EmbedTomcatServer extends BaseEmbedServer {
    private static final Logger LOG = Logger.getLogger(EmbedTomcatServer.class.getName());

    public static final String TYPE_NAME = "TOMCAT";

    public static final String CONFIG_FILE = "META-INF/context.xml";

    public static final String DEFAULT_PROTOCOL_HANDLER_NAME = "org.apache.coyote.http11.Http11NioProtocol";
    public static final String DEFAULT_URI_ENCODING = "UTF-8";
    public static final int DEFAULT_CACHE_SIZE = 16 * 1024;

    public static final String OPTION_PROTOCOL = "protocol";
    public static final String OPTION_URI_ENCODING = "uri_encoding";
    public static final String OPTION_RELOADABLE = "reloadable";
    public static final String OPTION_CACHE_SIZE = "cache_size";
    public static final String OPTION_DEFAULT_WEB_XML = "default_web_xml";
    public static final String OPTION_SCAN_CLASSES_DIR = "classes_dir";
    public static final String OPTION_USERS = "tomcat_user"; // user.password.role1,role2:user1.password.role

    public static final String GLOBAL_CLASSPATH_CONF_ROOT = "META-INF/embed/tomcat";

    private volatile File baseDir;
    private volatile Tomcat tomcatServer;
    private volatile Connector tomcatConnector;
    private volatile Context tomcatContext;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    public Tomcat getTomcatServer() {
        return tomcatServer;
    }

    public Connector getTomcatConnector() {
        return tomcatConnector;
    }

    public Context getTomcatContext() {
        return tomcatContext;
    }


    @Override
    protected void doServerInit(ClassLoader loader, Consumer<EmbedServer> config) throws RuntimeException {
        final int serverPort = (this.getPort() <= 0) ? 8080 : this.getPort();
        this.baseDir = InternalFactory.inst().get("tomcat_basedir", File.class, () -> {
            File bdir = null;
            if (StringUtils.isNotBlank(this.getWorkingdir())) {
                bdir = new File(this.getWorkingdir());
            }

            if (bdir == null || !bdir.exists() || !bdir.isDirectory()) bdir = createTempDir();
            return bdir;
        });

        this.tomcatServer = InternalFactory.inst().get("tomcat_server", Tomcat.class,
                () -> this.createTomcat(this.baseDir.getAbsolutePath()));
        this.tomcatConnector = InternalFactory.inst().get("tomcat_connector_" + serverPort, Connector.class,
                () -> this.createTomcatConnector(this.tomcatServer, serverPort));

        this.tomcatContext = InternalFactory.inst().get("tomcat_context_" + this.getContext(), Context.class, () -> {
            Context initContext = this.createTomcatContext(this.tomcatServer);

            this.configTomcat(this.baseDir, this.tomcatServer, this.tomcatConnector, initContext, loader);

            return initContext;
        });

        if (config != null) config.accept(this);
    }

    @Override
    protected void doServerStart() throws RuntimeException {
        if (this.tomcatServer == null) throw new IllegalStateException("Tomcat Server 未初始化");

        try {
            this.tomcatServer.start();
        } catch (Exception e) {
            throw new IllegalStateException("启动服务发生错误", e);
        }
    }

    @Override
    protected void doServerWait(Supplier<Boolean> waitWhen) throws RuntimeException {
        if (this.tomcatServer == null) throw new IllegalStateException("Tomcat Server 未初始化");
        try {
            this.tomcatServer.getServer().await();
        } catch (Exception e) {
            throw new IllegalStateException("等待服务发生错误", e);
        }
    }

    @Override
    protected void doServerReload() throws RuntimeException {
        if (this.tomcatContext == null) throw new IllegalStateException("Tomcat Server 未初始化");
        try {
            this.tomcatContext.reload();
        } catch (Exception e) {
            throw new IllegalStateException("重载context发生错误 " + e.getMessage(), e);
        }
    }

    @Override
    protected void doServerStop() throws RuntimeException {
        if (this.tomcatServer == null) throw new IllegalStateException("Tomcat Server 未初始化");
        try {
            if (this.tomcatContext != null) {
                this.tomcatContext.stop();
                this.tomcatContext.destroy();
            }

            if (this.tomcatServer != null) this.tomcatServer.stop();
        } catch (Exception e) {
            throw new IllegalStateException("停止服务发生错误", e);
        } finally {
            try {
                if (this.tomcatServer != null) this.tomcatServer.destroy();
            } catch (Throwable ignored) {
            }

            this.tomcatServer = null;
            this.tomcatContext = null;
            this.tomcatConnector = null;
        }
    }

    protected Tomcat createTomcat(String initBaseDir) {
        Tomcat initTomcat = new Tomcat();
        initTomcat.setBaseDir(initBaseDir);
        return initTomcat;
    }

    protected Context createTomcatContext(Tomcat initTomcat) throws RuntimeException {
        if (initTomcat == null) throw new IllegalStateException("tomcatServer 未初始化");

        return initTomcat.addWebapp(null, getTomcatContextPath(this.getContext()), this.getWebapp());
    }

    protected Connector createTomcatConnector(Tomcat initTomcat, int serverPort) {
        String phc = this.option(OPTION_PROTOCOL);
        if (StringUtils.isBlank(phc)) phc = DEFAULT_PROTOCOL_HANDLER_NAME;

        String uriEncoding = this.option(OPTION_URI_ENCODING);
        if (StringUtils.isBlank(uriEncoding)) uriEncoding = DEFAULT_URI_ENCODING;

        Connector conn = new Connector(phc);
        conn.setPort(serverPort);
        conn.setURIEncoding(uriEncoding);

        if (initTomcat.getConnector() != null) initTomcat.getService().removeConnector(initTomcat.getConnector());
        initTomcat.getService().addConnector(conn);

        return conn;
    }

    /**
     * 配置对应参数
     *
     * @param initTomcat
     * @param initConnector
     * @param initContext
     */
    protected void configTomcat(File initBasedir, Tomcat initTomcat, Connector initConnector, Context initContext, ClassLoader loader) {
        // ############ Context 配置部分
        StandardRoot sr = new StandardRoot(initContext);
        initContext.setResources(sr);


        if (loader == null) loader = Thread.currentThread().getContextClassLoader();
        this.setupWebappLoader(new WebappLoader(loader), initContext);

        if (!this.isExistsWebXml()) this.setupClassesJarScanning(initContext);

        this.setupCacheSize(initContext);
        initContext.setReloadable(this.isReloadable());

        //TODO 需验证tomcat当前版本是否已经修复 JasperInitializer 问题
        //ctx.addServletContainerInitializer(new JasperInitializer(), null);


        this.processResources(initTomcat, initContext);


        // 设置默认webxml和context.xml
        if (initContext instanceof StandardContext) {
            StandardContext sctx = (StandardContext) initContext;
            String webDefaultxml = this.option(OPTION_DEFAULT_WEB_XML);

            LOG.info("开始配置 StandardContext");

            this.setupGlobalConfFile(initBasedir);

            if (StringUtils.isNotBlank(webDefaultxml)) sctx.setDefaultWebXml(webDefaultxml);
            else sctx.setDefaultWebXml(initBasedir.getAbsolutePath() + "/conf/web.xml");

            sctx.setDefaultContextXml("conf/context.xml");
        }


        // 配置 应用对应的 context.xml
        URL configFileURL = this.findTomcatConfigFile();
        if (configFileURL != null) initContext.setConfigFile(configFileURL);

        // ############ 其它配置
        initTomcat.enableNaming();
        this.processTomcatUsers(initTomcat);
        this.setupListeners(initTomcat);

    }

    /**
     * 配置 webapplication
     * <p>
     * 增加额外的资源文件 需确保最后一个资源目录的web.xml（如果存在的话）与最终生成的web.xml一致
     *
     * @param initTomcat
     * @param initContext
     */
    protected void processResources(Tomcat initTomcat, Context initContext) {
        if (this.getClassesdirs() != null)
            this.getClassesdirs().forEach(e -> addWebappClassPathResource(initContext, e));
        if (this.getResourcesdirs() != null)
            this.getResourcesdirs().forEach(e -> addWebappWebappResource(initContext, e));
        if (this.getJars() != null)
            this.getJars().forEach(e -> addWebappWebinfJar(initContext, e));
    }

    /**
     * 增加用户
     *
     * @param initTomcat
     */
    protected void processTomcatUsers(Tomcat initTomcat) {
        Map<String, String> users = this.options(OPTION_USERS);
        users.forEach((key, value) -> {
            if (StringUtils.isBlank(value)) return;
            String[] udef = StringUtils.split(value, ":");
            if (udef.length >= 3) {
                String user = StringUtils.trim(udef[0]);
                String pass = StringUtils.trim(udef[1]);
                String[] roles = StringUtils.split(udef[2], ",");

                if (StringUtils.isNotBlank(user)) {
                    initTomcat.addUser(user, pass);
                    if (roles != null) for (String r : roles) initTomcat.addRole(user, StringUtils.trim(r));
                }
            } else LOG.severe("用户定义非法 user:password:roles 设置的值为=" + value);
        });
    }

    private boolean isReloadable() {
        return (StringUtils.isNotBlank(this.option(OPTION_RELOADABLE))) && this.optionBoolean(OPTION_RELOADABLE);
    }

    private boolean isExistsWebXml() {
        String wad = this.getWebapp();
        if (StringUtils.isBlank(wad)) return false;
        try {
            File wxf = new File(wad, "WEB-INF/web.xml");
            return (wxf.exists() && wxf.isFile());
        } catch (Throwable e) {
        }
        return false;
    }


    /**
     * 当项目不存在 WEB-INF/classes 时，扫描目标的jar
     * <p>
     * TODO 此行为未进行验证
     *
     * @param initContext
     */
    @SuppressWarnings("PointlessNullCheck")
    private void setupClassesJarScanning(Context initContext) {
        JarScanner jsc = initContext.getJarScanner();
        if ((jsc != null) && (jsc instanceof StandardJarScanner)) {
            ((StandardJarScanner) jsc).setScanAllDirectories(true);
        }


        String wcpd = this.option(OPTION_SCAN_CLASSES_DIR);
        if (StringUtils.isNotBlank(wcpd)) {
            String[] wcps = StringUtils.split(wcpd, ";");
            if (wcps != null) for (String wc : wcps) {
                try {
                    if (StringUtils.isBlank(wc)) continue;

                    File cf = new File(wc);
                    if (cf.exists() && cf.isDirectory() && cf.canWrite()) {
                        File metaInfDir = new File(cf, "META-INF");
                        if (!metaInfDir.exists()) {
                            boolean success = metaInfDir.mkdir();
                            if (!success) LOG.warning("无法创建 " + wc + "/META-INF ");
                        }
                    }
                } catch (Throwable e) {
                    LOG.warning("处理目录 " + wc + " 出错");
                }
            }
        }
    }

    protected void setupWebappLoader(WebappLoader loader, Context initContext) {
        initContext.setLoader(loader);
    }

    protected void setupListeners(Tomcat initTomcat) {
        final List<LifeCycleListener> lns = new ArrayList<>();
        lns.add(new LogListener(LOG));
        lns.addAll(this.getListeners());

        final EmbedTomcatServer cur = this;

        initTomcat.getServer().addLifecycleListener(event -> {
            switch (event.getType()) {
                case Lifecycle.START_EVENT:
                    lns.forEach(e -> e.onStarting(cur));
                    break;
                case Lifecycle.AFTER_START_EVENT:
                    lns.forEach(e -> e.onStarted(cur));
                    break;
                case Lifecycle.STOP_EVENT:
                    lns.forEach(e -> e.onStopping(cur));
                    break;
                case Lifecycle.AFTER_STOP_EVENT:
                    lns.forEach(e -> e.onStopped(cur));
                    break;
                default:
                    break;
            }
        });
    }

    protected void setupGlobalConfFile(File initBasedir) {
        if (initBasedir == null || !initBasedir.exists()) return;

        try {
            File confDir = new File(initBasedir, "conf");
            FileUtils.forceMkdir(confDir);

            copyClassPathResourceFile(GLOBAL_CLASSPATH_CONF_ROOT + "/conf/web.xml", new File(confDir, "web.xml"));
            copyClassPathResourceFile(GLOBAL_CLASSPATH_CONF_ROOT + "/conf/context.xml", new File(confDir, "context.xml"));
            copyClassPathResourceFile(GLOBAL_CLASSPATH_CONF_ROOT + "/conf/logging.properties", new File(confDir, "logging.properties"));
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "无法处理 Global 配置文件 " + e.getMessage(), e);
        }
    }

    protected void setupCacheSize(Context initContext) {
        int cacheSize = DEFAULT_CACHE_SIZE;

        if (StringUtils.isNotBlank(this.option(OPTION_CACHE_SIZE))) cacheSize = this.optionInt(OPTION_CACHE_SIZE);

        if (cacheSize > 0) {
            initContext.getResources().setCacheMaxSize(cacheSize);
        } else if (cacheSize < 0) {
            initContext.getResources().setCacheMaxSize(DEFAULT_CACHE_SIZE);
            initContext.getResources().setCachingAllowed(false);
        }
    }

    private static void addTomcatResource(Context initContext, URL resource, String mount) {
        if (initContext == null || resource == null) return;
        if (mount == null) mount = "/";

        LOG.info("增加了资源 " + mount + " -> " + resource);

        initContext.getResources().createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, mount, resource, "/");
    }

    protected void addWebappClassPathResource(Context initContext, String resource) {
        if (resource != null) {
            try {
                URL rui = toURL(resource, true);
                if (rui != null) addTomcatResource(initContext, rui, "/WEB-INF/classes");
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "无法转换 URI -> " + resource, e);
            }
        }
    }

    protected void addWebappWebappResource(Context initContext, String resource) {
        if (resource != null) {
            try {
                URL rui = toURL(resource, true);
                if (rui != null) addTomcatResource(initContext, rui, "/");
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "无法转换 URI -> " + resource, e);
            }
        }
    }

    protected void addWebappWebinfJar(Context initContext, String resource) {
        if (resource != null) {
            try {
                URL rui = toURL(resource, true);
                if (rui != null) addTomcatResource(initContext, rui, "/WEB-INF/lib");
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "无法转换 URI -> " + resource, e);
            }
        }
    }


    protected URL findTomcatConfigFile() {
        try {
            if (StringUtils.isNotBlank(this.getConfigfile())) {
                LOG.info("Web 应用 配置文件 = " + this.getConfigfile());
                String cf = this.getConfigfile();
                if (StringUtils.contains(cf, ":")) {
                    return new URL(cf); // 完整URL形式
                } else {
                    File f = new File(cf);
                    if (f.exists() && f.isFile()) return f.toURI().toURL();
                }
            }

            File waf = new File(this.getWebapp());
            if (!waf.exists()) return null;
            if (waf.isFile()) { // war
                LOG.info("Web 应用WAR = " + waf.getCanonicalPath());

                JarFile war = new JarFile(waf.getAbsolutePath());
                JarEntry defaultConfigFileEntry = war.getJarEntry(CONFIG_FILE);

                if (defaultConfigFileEntry != null) {
                    URL cfurl = new URL("jar:" + waf.toURI().toString() + "!/" + CONFIG_FILE);

                    LOG.info("Web 应用配置文件 = " + cfurl.toString());
                    return cfurl;
                }
            } else if (waf.isDirectory()) { // webappdir
                LOG.info("Web 应用目录 = " + waf.getCanonicalPath());

                File cff = new File(waf, CONFIG_FILE);
                if (cff.exists() && cff.isFile()) {
                    LOG.info("Web 应用配置文件 = " + cff.toURI().toURL().toString());
                    return cff.toURI().toURL();
                }
            }

            // 查找classes_dir
            String wcpd = this.option(OPTION_SCAN_CLASSES_DIR);
            if (StringUtils.isNotBlank(wcpd)) {
                try {
                    File cf = new File(wcpd);
                    if (cf.exists() && cf.isDirectory()) {
                        File cff = new File(cf, CONFIG_FILE);
                        if (cff.exists()) return cff.toURI().toURL();
                    }
                } catch (Throwable e) {
                    LOG.warning("处理目录 " + wcpd + " 出错");
                }
            }

        } catch (Throwable e) {
            LOG.warning("查找 config.xml 文件错误");
        }

        return null;
    }

    private static String getTomcatContextPath(String contextPath) {
        contextPath = StringUtils.trim(contextPath);
        if (StringUtils.isBlank(contextPath) || StringUtils.equals(contextPath, "/")) return "";
        return (StringUtils.startsWith(contextPath, "/")) ? contextPath : ("/" + contextPath);
    }

    /**
     * 复制class下文件到目标文件中
     *
     * @param res
     * @param dist
     * @throws RuntimeException
     */
    private static void copyClassPathResourceFile(String res, File dist) throws RuntimeException {
        if (StringUtils.isBlank(res) || dist == null) throw new IllegalStateException("文件不允许为空");

        InputStream is = null;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            is = loader.getResourceAsStream(res);
            if (is == null) throw new IllegalStateException("无法找到类路径资源 " + res);

            FileUtils.forceMkdirParent(dist);
            if (dist.exists()) FileUtils.forceDelete(dist);
            if (!dist.createNewFile()) throw new IllegalStateException("无法创建文件 " + dist);

            FileUtils.copyInputStreamToFile(is, dist);
        } catch (Throwable e) {
            throw new IllegalStateException("无法复制指定资源 " + e.getMessage(), e);
        } finally {
            if (is != null) try {
                is.close();
            } catch (Throwable ignored) {
            }
        }
    }


    public static void main(String[] args) {
        if (args == null || args.length < 1) throw new IllegalArgumentException("参数配置错误");

        Builder builder = Builder.builder();
        builder.opts(
                optionParser -> {
                    // 识别jetty参数
                    optionParser.accepts(OPTION_PROTOCOL).withOptionalArg();
                    optionParser.accepts(OPTION_URI_ENCODING).withOptionalArg();
                    optionParser.accepts(OPTION_RELOADABLE).withOptionalArg();
                    optionParser.accepts(OPTION_CACHE_SIZE).withOptionalArg();
                    optionParser.accepts(OPTION_DEFAULT_WEB_XML).withOptionalArg();
                    optionParser.accepts(OPTION_SCAN_CLASSES_DIR).withOptionalArg();
                    optionParser.accepts(OPTION_USERS).withOptionalArg();
                }, optionSet -> {
                    // 处理jetty参数
                    String oPROTOCOL = InternalOptParsers.optString(optionSet, OPTION_PROTOCOL, "");
                    if (StringUtils.isNotBlank(oPROTOCOL)) builder.option(OPTION_PROTOCOL, oPROTOCOL);

                    String oURI_ENCODING = InternalOptParsers.optString(optionSet, OPTION_URI_ENCODING, "");
                    if (StringUtils.isNotBlank(oURI_ENCODING)) builder.option(OPTION_URI_ENCODING, oURI_ENCODING);

                    String oRELOADABLE = InternalOptParsers.optString(optionSet, OPTION_RELOADABLE, "");
                    if (StringUtils.isNotBlank(oRELOADABLE)) builder.option(OPTION_RELOADABLE, oRELOADABLE);

                    String oCACHE_SIZE = InternalOptParsers.optString(optionSet, OPTION_CACHE_SIZE, "");
                    if (StringUtils.isNotBlank(oCACHE_SIZE)) builder.option(OPTION_CACHE_SIZE, oCACHE_SIZE);

                    String oDWEB_XML = InternalOptParsers.optString(optionSet, OPTION_DEFAULT_WEB_XML, "");
                    if (StringUtils.isNotBlank(oDWEB_XML)) builder.option(OPTION_DEFAULT_WEB_XML, oDWEB_XML);

                    List<String> oSC_DIR = InternalOptParsers.optString(optionSet, OPTION_SCAN_CLASSES_DIR);
                    if (oSC_DIR != null) oSC_DIR.forEach(s -> builder.option(OPTION_SCAN_CLASSES_DIR, s));

                    List<String> oUSERS = InternalOptParsers.optString(optionSet, OPTION_USERS);
                    if (oUSERS != null) oUSERS.forEach(s -> builder.option(OPTION_USERS, s));

                },
                args);

        EmbedTomcatServer es = builder.build(EmbedTomcatServer.class, server -> {
            LOG.info("EmbedTomcatServer 已载入");
            // 增加其他初始化配置
        });

        if (es == null) throw new IllegalStateException("无法载入 EmbedTomcatServer");

        boolean failed = false;
        try {
            es.start(Thread.currentThread().getContextClassLoader(), false, false);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "EmbedTomcatServer 运行错误" + e.getMessage(), e);
            failed = true;
        } finally {
            es.stop();
        }

        System.exit(failed ? 2 : 0);
    }
}
