package com.hreeinfo.commons.embed.server.support.test;

import com.hreeinfo.commons.embed.server.EmbedServer;
import com.hreeinfo.commons.embed.server.support.EmbedJettyServer;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class JettyServerBuilderTests {
    static List<String> getTestClassPaths() {
        String[] acps = StringUtils.split(System.getProperty("java.class.path"), ":");
        return Arrays.asList(acps);
    }

    static String getTestResourcePath() {
        try {
            String path = System.getProperty("user.dir") + "/commons-embed-server-jetty/src/test/resources";
            if (new File(path).exists()) return path;

            path = System.getProperty("user.dir") + "/src/test/resources";
            if (new File(path).exists()) return path;

        } catch (Throwable e) {
        }

        throw new IllegalArgumentException("未找到项目 resources 目录");
    }

    static String getTestWebappPath() {
        try {
            String path = System.getProperty("user.dir") + "/commons-embed-server-jetty/src/test/web";
            if (new File(path).exists()) return path;

            path = System.getProperty("user.dir") + "/src/test/web";
            if (new File(path).exists()) return path;

        } catch (Throwable e) {
        }

        throw new IllegalArgumentException("未找到项目 resources 目录");
    }

    public static void main(String[] args) throws Exception {
        List<String> cps = getTestClassPaths();

        EmbedServer server = EmbedServer.Builder.builder()
                .port(8080).context("/app").loglevel("INFO")
                .webapp(getTestWebappPath())
                .reloadLockfile("/tmp/jetty-embed-reload-test.lock")
                .classesdir(cps.toArray(new String[cps.size()]))
                .resourcedir(getTestResourcePath())
                .build(EmbedJettyServer.class, e -> System.out.println(e.getClass() + " 配置完成"));

        server.start();
    }
}
