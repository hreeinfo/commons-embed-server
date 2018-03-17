package com.hreeinfo.commons.embed.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 静态工具方法 根据指定变量调用对应服务器实现并启动服务器
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class Servers {
    private static final Logger LOG = Logger.getLogger(Servers.class.getName());
    private static final Map<String, String> RUNNER_TYPES = new ConcurrentHashMap<>();

    static {
        RUNNER_TYPES.put("TOMCAT", "com.hreeinfo.commons.embed.server.support.TomcatServerRunner");
        RUNNER_TYPES.put("JETTY", "com.hreeinfo.commons.embed.server.support.JettyServerRunner");
        RUNNER_TYPES.put("PAYARA", "com.hreeinfo.commons.embed.server.support.PayaraServerRunner");
    }

    /**
     * 获取目标实例
     *
     * @param opt
     * @return
     */
    public static ServerRunner server(ServerRunnerOpt opt) {
        if (opt == null) return null;
        try {
            String type = opt.getType();
            if (StringUtils.isBlank(type)) type = "TOMCAT";
            String typeName = RUNNER_TYPES.get(type.toUpperCase());

            if (StringUtils.isBlank(typeName)) throw new IllegalArgumentException("未找到目标：" + type);
            Class<?> typeCls = Class.forName(typeName);

            if (typeCls == null) throw new IllegalArgumentException("未找到目标类：" + typeName);

            ServerRunner sr = (ServerRunner) ConstructorUtils.invokeConstructor(typeCls, opt);

            if (sr == null) throw new IllegalArgumentException("无法创建目标实例：：" + type);

            return sr;
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, e.getMessage());
        }
        return null;
    }

    /**
     * 获取目标实例
     *
     * @param args
     * @return
     */
    public static ServerRunner server(String[] args) {
        return server(ServerRunnerOpt.loadFromOpts(args));
    }

    /**
     * 根据命令行参数 直接运行
     *
     * @param args
     */
    public static void main(String[] args) {
        ServerRunner runner = null;
        try {
            runner = server(args);
            if (runner == null) throw new IllegalArgumentException("无法创建应用实例");

            runner.start();
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, e.getMessage());
        } finally {
            try {
                if (runner != null) runner.stop();
            } catch (Throwable e1) {
                LOG.log(Level.SEVERE, "退出错误 " + e1.getMessage());
            }
        }
    }
}
