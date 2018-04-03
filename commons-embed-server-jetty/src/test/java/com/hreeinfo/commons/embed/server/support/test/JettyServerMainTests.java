package com.hreeinfo.commons.embed.server.support.test;

import com.hreeinfo.commons.embed.server.support.EmbedJettyServer;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/4/3 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class JettyServerMainTests {
    private static String[] buildArgs() {
        List<String> args = new ArrayList<>();

        args.add("--port=8888");
        args.add("--context=/app");
        args.add("--webapp=" + JettyServerBuilderTests.getTestWebappPath());
        args.add("--loglevel=INFO");
        args.add("--jetty_configurations=*");
        args.add("--jetty_configurations=ss");
        args.add("--lockfile=/tmp/app-embed-server.lock");
        args.add("--resourcesdir=" + JettyServerBuilderTests.getTestResourcePath());
        for (String s : JettyServerBuilderTests.getTestClassPaths()) args.add("--classpath=" + s);

        return args.toArray(new String[]{});
    }

    public static void main(String[] args) {
        EmbedJettyServer.main(buildArgs());
    }
}
