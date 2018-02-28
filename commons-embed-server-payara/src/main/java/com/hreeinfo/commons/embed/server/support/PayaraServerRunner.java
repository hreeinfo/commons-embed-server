package com.hreeinfo.commons.embed.server.support;


import java.io.File;
import java.util.logging.Logger;

import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class PayaraServerRunner {
    private static final Logger LOGGER = Logger.getLogger(PayaraServerRunner.class.getName());

    // TODO 待完成此实现
    public static void main(String[] args) throws Exception {
        File path = new File("commons-embed-server-payara/src/test/web");

        PayaraMicro micro = PayaraMicro.getInstance();

        micro.setHttpPort(8080);

        PayaraMicroRuntime runtime = micro.bootStrap();

        LOGGER.info("开始部署目标应用");

        runtime.deploy("aaaaaa", "", path);
    }
}
