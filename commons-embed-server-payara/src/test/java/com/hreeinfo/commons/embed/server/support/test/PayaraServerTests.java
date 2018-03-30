package com.hreeinfo.commons.embed.server.support.test;

import com.hreeinfo.commons.embed.server.EmbedServer;
import com.hreeinfo.commons.embed.server.support.EmbedPayaraServer;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/24 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class PayaraServerTests {
    public static void main(String[] args) {
        EmbedServer server = EmbedServer.Builder.builder()
                .port(8080).context("/aa").loglevel("INFO")
                .webapp("commons-embed-server-payara/src/test/web")
                .build(EmbedPayaraServer.class, e -> System.out.println(e.getClass() + " 配置完成"));

        server.start();
    }
}
