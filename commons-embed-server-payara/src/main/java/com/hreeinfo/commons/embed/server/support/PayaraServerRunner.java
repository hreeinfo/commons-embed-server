package com.hreeinfo.commons.embed.server.support;


import org.glassfish.embeddable.*;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class PayaraServerRunner {

    private static final Logger LOGGER = Logger.getLogger(PayaraServerRunner.class.getName());

    // Usage: 'PayaraServerRunner [port] [webbappdir] [classesdir] [resourcesdir] [LogLevel] [name] [workdir]'
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        Level logLevel = Level.parse(args[4]);
        String workdir = args[6];

        LOGGER.log(Level.CONFIG, "Configuring logger log levels to "+logLevel);

        Logger.getLogger("").getHandlers()[0].setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system.tools.deployment").setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system").setLevel(logLevel);

        LOGGER.log(Level.INFO, "Starting Payara web server...");

        try {

            BootstrapProperties bootstrap = new BootstrapProperties();

            GlassFishRuntime runtime = GlassFishRuntime.bootstrap(bootstrap,
                    PayaraServerRunner.class.getClass().getClassLoader());

            GlassFishProperties glassfishProperties = new GlassFishProperties();
            glassfishProperties.setPort("http-listener", port);
            LOGGER.log(Level.INFO, "Running on port "+port);

            GlassFish glassfish = runtime.newGlassFish(glassfishProperties);
            glassfish.start();

            Deployer deployer = glassfish.getDeployer();

            File work = new File(workdir);
            File explodedWar = new File(work, "war");

            deployer.deploy(explodedWar, "--contextroot=");

        } catch (Exception ex){
            LOGGER.log(Level.SEVERE, "Failed to start Payara server", ex);
            throw ex;
        }
    }

}
