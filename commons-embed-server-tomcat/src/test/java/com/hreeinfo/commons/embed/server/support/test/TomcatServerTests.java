package com.hreeinfo.commons.embed.server.support.test;

import com.hreeinfo.commons.embed.server.ServerRunner;
import com.hreeinfo.commons.embed.server.ServerRunnerOpt;
import com.hreeinfo.commons.embed.server.Servers;
import com.hreeinfo.commons.embed.server.support.TomcatServerRunner;
import org.apache.commons.lang3.StringUtils;

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
        String[] acps = StringUtils.split(System.getProperty("java.class.path"), ":");
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
        ServerRunnerOpt opt = new ServerRunnerOpt();
        opt.type("TOMCAT").context("/aa").port(8080)
                .webappdir(getTestWebappPath())
                .classesdir()
                .resourcesdir(getTestResourcePath() + "/addtion")
                .option("cacheSize", "100000");

        System.out.println("命令行参数：" + opt.toParams());

        final ServerRunner runner = Servers.server(StringUtils.split(opt.toParams(), " "));

        try {
            runner.start();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            runner.stop();
        }
    }
}
