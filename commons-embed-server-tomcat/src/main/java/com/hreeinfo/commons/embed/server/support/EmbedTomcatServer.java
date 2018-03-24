package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/23 </p>
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
    public static final String OPTION_WEBINF_CLASS_RESOURCES = "webinf_classes"; // 多个值用,分隔
    public static final String OPTION_USERS = "tomcat_user"; // user.password.role1,role2:user1.password.role

    public static final String GLOBAL_CLASSPATH_CONF_ROOT = "META-INF/embed/tomcat";

    private volatile Tomcat tomcat;
    private volatile Context tomcatContext;
    private volatile Connector tomcatConnector;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    protected boolean isUseThreadContextLoader() {
        return true;
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public Context getTomcatContext() {
        return tomcatContext;
    }

    public Connector getTomcatConnector() {
        return tomcatConnector;
    }

    @Override
    protected void doServerInit(ClassLoader loader, Consumer<EmbedServer> config) throws RuntimeException {
        if (this.tomcat == null) {
            File basedir = null;
            if (StringUtils.isNotBlank(this.getWorkingdir())) {
                basedir = new File(this.getWorkingdir());
            }

            if (basedir == null || !basedir.exists() || !basedir.isDirectory()) basedir = createTempDir();

            Tomcat initTomcat = this.createTomcat(basedir.getAbsolutePath());
            Context initContext = this.createTomcatContext(initTomcat);
            Connector initConnector = this.createTomcatConnector(initTomcat, initContext);

            this.configTomcat(basedir, initTomcat, initContext, initConnector, loader);

            this.tomcat = initTomcat;
            this.tomcatContext = initContext;
            this.tomcatConnector = initConnector;

            if (config != null) config.accept(this);
        }
    }

    @Override
    protected void doServerStart() throws RuntimeException {
        if (this.tomcat == null) throw new IllegalStateException("Tomcat Server 未初始化");

        try {
            this.tomcat.start();
        } catch (Exception e) {
            throw new IllegalStateException("启动服务发生错误", e);
        }
    }

    @Override
    protected void doServerWait() throws RuntimeException {
        if (this.tomcat == null) throw new IllegalStateException("Tomcat Server 未初始化");
        try {
            this.tomcat.getServer().await();
        } catch (Exception e) {
            throw new IllegalStateException("等待服务发生错误", e);
        }
    }

    @Override
    protected void doServerStop() throws RuntimeException {
        if (this.tomcat == null) throw new IllegalStateException("Tomcat Server 未初始化");
        try {
            this.tomcat.stop();
        } catch (Exception e) {
            throw new IllegalStateException("停止服务发生错误", e);
        }
    }

    protected Tomcat createTomcat(String baseDir) {
        Tomcat initTomcat = new Tomcat();
        initTomcat.setBaseDir(baseDir);
        return initTomcat;
    }

    protected Context createTomcatContext(Tomcat initTomcat) throws RuntimeException {
        if (initTomcat == null) throw new IllegalStateException("tomcat 未初始化");

        return initTomcat.addWebapp(null, getTomcatContextPath(this.getContext()), this.getWebapp());
    }

    protected Connector createTomcatConnector(Tomcat initTomcat, Context initContext) {
        String phc = this.option(OPTION_PROTOCOL);
        if (StringUtils.isBlank(phc)) phc = DEFAULT_PROTOCOL_HANDLER_NAME;

        String uriEncoding = this.option(OPTION_URI_ENCODING);
        if (StringUtils.isBlank(uriEncoding)) uriEncoding = DEFAULT_URI_ENCODING;

        Connector conn = new Connector(phc);
        conn.setPort(this.getPort());
        conn.setURIEncoding(uriEncoding);

        if (initTomcat.getConnector() != null) initTomcat.getService().removeConnector(initTomcat.getConnector());
        initTomcat.getService().addConnector(conn);

        return conn;
    }

    /**
     * 配置对应参数
     *
     * @param initTomcat
     * @param initContext
     * @param initConnector
     */
    protected void configTomcat(File initBasedir, Tomcat initTomcat, Context initContext, Connector initConnector, ClassLoader loader) {
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

            System.out.println("当前设置 " + sctx.getDefaultWebXml());
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
        if (this.getResourcesdirs() != null) for (String s : this.getResourcesdirs()) {
            File sf = new File(s);
            if (sf.exists()) {
                LOG.info("增加了额外的资源文件 " + sf);
                this.addWebappWebappResource(initContext, sf);
            }
        }

        String wcrs = this.option(OPTION_WEBINF_CLASS_RESOURCES);
        if (StringUtils.isNotBlank(wcrs)) {
            String[] wcr = StringUtils.split(wcrs, ",");
            if (wcr != null) for (String w : wcr) {
                File wf = new File(StringUtils.trim(w));
                if (wf.exists()) {
                    LOG.info("增加了额外的资源文件 " + wf);
                    this.addWebappClassPathResource(initContext, wf);
                }
            }
        }
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
        return (StringUtils.isBlank(this.option(OPTION_RELOADABLE))) || this.optionBoolean(OPTION_RELOADABLE);
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


    private void setupClassesJarScanning(Context initContext) {
        JarScanner jsc = initContext.getJarScanner();
        if (jsc != null && (jsc instanceof StandardJarScanner))
            ((StandardJarScanner) jsc).setScanAllDirectories(true);

        for (String c : this.getClassesdirs()) {
            if (StringUtils.isBlank(c)) continue;
            try {
                File cf = new File(c);
                if (cf.exists() && cf.isDirectory() && cf.canWrite()) {
                    File metaInfDir = new File(cf, "META-INF");
                    if (!metaInfDir.exists()) {
                        boolean success = metaInfDir.mkdir();
                        if (!success) LOG.warning("无法创建 " + c + "/META-INF ");
                    }
                }
            } catch (Throwable e) {
                LOG.warning("处理目录 " + c + " 出错");
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

            System.out.println("导出了文件： " + Arrays.toString(confDir.list()));
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

    protected void addWebappClassPathResource(Context initContext, File resource) {
        if (resource != null) {
            try {
                URL rui = resource.toURI().toURL();
                initContext.getResources().createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/WEB-INF/classes", rui, "/");
            } catch (Throwable e) {
                LOG.warning("无法转换 URI -> " + resource);
            }
        }
    }

    protected void addWebappWebappResource(Context initContext, File resource) {
        if (resource != null && resource.exists()) {
            try {
                URL rui = resource.toURI().toURL();
                initContext.getResources().createWebResourceSet(WebResourceRoot.ResourceSetType.PRE, "/", rui, "/");
            } catch (Throwable e) {
                LOG.warning("无法转换 URI -> " + resource);
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
}
