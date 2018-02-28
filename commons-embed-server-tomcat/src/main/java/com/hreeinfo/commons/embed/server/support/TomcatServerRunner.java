package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.ServerRunnerOpt;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanner;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 额外可以配置的选项
 * <p>
 * webDefaultXml -> 默认的web.xml
 * <p>
 * cacheSize -> 配置resource scan 过程的 cacheSize
 * <p>
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/26 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class TomcatServerRunner extends BaseServerRunner {
    private static final Logger LOG = Logger.getLogger(TomcatServerRunner.class.getName());
    public static final String CONFIG_FILE = "META-INF/context.xml";
    public static final int DEFAULT_CACHE_SIZE = 16 * 1024;

    private final TomcatServer server;

    public TomcatServerRunner(ServerRunnerOpt opt) {
        super(opt);
        this.server = new TomcatServerImpl();
        this.server.setHome(TomcatServerImpl.createTempDir().getAbsolutePath());
    }

    @Override
    public void start() throws RuntimeException {
        if (!this.server.isStopped()) throw new IllegalStateException("Tomcat Server 未停止，无法启动");

        try {
            Thread serverThread = new Thread(() -> {
                ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

                ClassLoader cl = createClassLoader(opt.getClassesdirs());
                if (cl != null) Thread.currentThread().setContextClassLoader(cl);

                configureServer();

                server.start();

                Thread.currentThread().setContextClassLoader(originalClassLoader);
            });

            serverThread.run();

            this.server.waitTomcat();
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Tomcat Server 启动过程发生错误", e);
        }
    }

    @Override
    public void stop() {
        try {
            this.server.stop();
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Tomcat Server 停止过程发生错误", e);
        }
    }

    protected void configureServer() {
        // 配置 container
        this.server.createContext(this.opt.getContext(), this.opt.getWebappdir());

        // 不存在webxml时，需要处理classesJarScanning
        if (!this.existsWebXml()) this.setupClassesJarScanning();

        // 配置 Loader
        this.server.createLoader(Thread.currentThread().getContextClassLoader());

        // 配置 webapplication
        // 增加额外的资源文件
        // TODO 目前所增加的文件比webappdir目录文件优先 所以对于新增目录存在 WEB-INF/web.xml 的情况考虑是否进行额外处理
        if (this.opt.getResourcesdirs() != null) for (String s : this.opt.getResourcesdirs()) {
            File sf = new File(s);
            if (sf.exists()) {
                LOG.info("增加了额外的资源文件 " + sf);
                this.server.addWebappWebappResource("/", sf);
            }
        }

        // 配置参数
        this.server.setReloadable(true);
        this.server.setCacheSize(this.getResourceCacheSize());

        // 设置默认webxml
        File dwxf = this.getWebDefaultXml();
        if (dwxf != null) this.server.configureDefaultWebXml(dwxf);

        // 配置 context.xml
        URL configFileURL = this.getConfigFile();
        if (configFileURL != null) this.server.setConfigFile(configFileURL);


        // TODO 配置其它额外的变量

        // TODO 增加默认的侦听事件

        // 配置 server
        this.server.configureContainer();
        this.server.configureHttpConnector(this.opt.getPort(), "UTF-8", TomcatServer.DEFAULT_PROTOCOL_HANDLER_NAME);
    }

    protected URL getConfigFile() {
        try {
            if (StringUtils.isNotBlank(this.opt.getConfigfile())) {
                LOG.info("Web 应用 配置文件 = " + this.opt.getConfigfile());
                if (StringUtils.contains(this.opt.getConfigfile(), ":")) {
                    return new URL(this.opt.getConfigfile()); // 完整URL形式
                } else {
                    File f = new File(this.opt.getConfigfile());
                    if (f.exists() && f.isFile()) return f.toURI().toURL();
                }
            }

            if (StringUtils.isBlank(this.opt.getWebappdir())) {
                File waf = new File(this.opt.getWebappdir());
                if (waf.exists()) {
                    if (waf.isFile()) { // war
                        LOG.info("Web 应用 WAR = " + waf.getCanonicalPath());

                        JarFile war = new JarFile(waf.getAbsolutePath());
                        JarEntry defaultConfigFileEntry = war.getJarEntry(CONFIG_FILE);

                        if (defaultConfigFileEntry != null) {
                            URL cfurl = new URL("jar:" + waf.toURI().toString() + "!/" + CONFIG_FILE);

                            LOG.info("Web 应用 配置文件 = " + cfurl.toString());
                            return cfurl;
                        }
                    } else if (waf.isDirectory()) { // webappdir
                        LOG.info("Web 应用 目录 = " + waf.getCanonicalPath());

                        File cff = new File(waf, CONFIG_FILE);
                        if (cff.exists() && cff.isFile()) {
                            LOG.info("Web 应用 配置文件 = " + cff.toURI().toURL().toString());
                            return cff.toURI().toURL();
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOG.warning("查找 config.xml 文件错误");
        }

        return null;
    }

    protected File getWebDefaultXml() {
        String wdx = this.opt.optionValue("webDefaultXml");
        if (StringUtils.isBlank(wdx)) return null;
        try {
            File f = new File(wdx);

        } catch (Throwable e) {
        }
        return null;
    }

    protected int getResourceCacheSize() {
        String optionValue = this.opt.getOptions().get("cacheSize");
        if (StringUtils.isBlank(optionValue)) return DEFAULT_CACHE_SIZE;

        try {
            return Integer.valueOf(StringUtils.trim(optionValue));
        } catch (Throwable e) {
        }

        return DEFAULT_CACHE_SIZE;
    }

    protected void setupClassesJarScanning() {
        if (this.server.getContext() != null) {
            JarScanner jsc = this.server.getContext().getJarScanner();
            if (jsc != null && (jsc instanceof StandardJarScanner))
                ((StandardJarScanner) jsc).setScanAllDirectories(true);

            for (String c : this.opt.getClassesdirs()) {
                if (StringUtils.isBlank(c)) continue;
                try {
                    File cf = new File(c);
                    if (cf.exists() && cf.isDirectory() && cf.canWrite()) {
                        File metaInfDir = new File(cf, "META-INF");
                        if (!metaInfDir.exists()) {
                            boolean success = metaInfDir.mkdir();
                            if (!success) LOG.warning("无法创建 " + c + "/META-INF");
                        }
                    }
                } catch (Throwable e) {
                    LOG.warning("处理目录 " + c + " 出错");
                }
            }
        } else throw new IllegalStateException("Tomcat Context为空");
    }

    protected boolean existsWebXml() {
        String wad = this.opt.getWebappdir();
        if (StringUtils.isBlank(wad)) return false;
        try {
            File wxf = new File(wad, "WEB-INF/web.xml");
            return (wxf.exists() && wxf.isFile());
        } catch (Throwable e) {
        }
        return false;
    }


    protected ClassLoader createClassLoader(Collection<String> tomcatClasspathFiles) {
        if (tomcatClasspathFiles == null || tomcatClasspathFiles.isEmpty()) return null;
        return new URLClassLoader(toURLArray(tomcatClasspathFiles), Thread.currentThread().getContextClassLoader());
    }

    protected URL[] toURLArray(Collection<String> files) {
        final List<URL> urls = new ArrayList<>();
        if (files != null) for (String f : files) {
            if (StringUtils.isBlank(f)) continue;
            try {
                URL u = null;

                if (StringUtils.contains(f, ":")) u = new URL(f);
                else u = new File(f).toURI().toURL();

                urls.add(u);
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "Tomcat Server 停止过程发生错误", e);
            }
        }

        return urls.toArray(new URL[urls.size()]);
    }
}
