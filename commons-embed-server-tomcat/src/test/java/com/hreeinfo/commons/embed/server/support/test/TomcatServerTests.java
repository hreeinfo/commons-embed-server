package com.hreeinfo.commons.embed.server.support.test;

import com.hreeinfo.commons.embed.server.EmbedServer;
import com.hreeinfo.commons.embed.server.support.EmbedTomcatServer;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 测试tomcat server
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/28 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class TomcatServerTests {
    private static List<String> getTestClassPaths() {
        String[] acps = {System.getProperty("user.dir") + "/commons-embed-server-tomcat/src/test/java"};
        return Arrays.asList(acps);
    }

    private static String getTestResourcePath() {
        try {
            String path = System.getProperty("user.dir") + "/commons-embed-server-tomcat/src/test/resources";
            if (new File(path).exists()) return path;

            path = System.getProperty("user.dir") + "/src/test/resources";
            if (new File(path).exists()) return path;

        } catch (Throwable e) {
        }

        throw new IllegalArgumentException("未找到项目 resources 目录");
    }

    private static String getTestWebappPath() {
        try {
            String path = System.getProperty("user.dir") + "/commons-embed-server-tomcat/src/test/web";
            if (new File(path).exists()) return path;

            path = System.getProperty("user.dir") + "/src/test/web";
            if (new File(path).exists()) return path;

        } catch (Throwable e) {
        }

        throw new IllegalArgumentException("未找到项目 resources 目录");
    }

    public static void main(String[] args) {
        List<String> cps = getTestClassPaths();

        EmbedServer server = EmbedServer.Builder.builder()
                .port(8080).context("/aa").loglevel("INFO")
                .webapp(getTestWebappPath())
                .classesdir(cps.toArray(new String[cps.size()]))
                .resourcedir(getTestResourcePath())
                .build(EmbedTomcatServer.class, e -> System.out.println(e.getClass() + " 配置完成"));

        server.start();
    }
}
