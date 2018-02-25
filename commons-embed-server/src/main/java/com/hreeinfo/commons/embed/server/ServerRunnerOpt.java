package com.hreeinfo.commons.embed.server;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * ServerRunner 载入参数
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/24 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class ServerRunnerOpt implements Serializable {
    private String type = "TOMCAT";// 默认 tomcat jetty payara 忽略大小写
    private int port = 8080;
    private String context = "";
    private String webappdir = "";
    private List<String> classesdirs = new ArrayList<>();
    private List<String> resourcesdirs = new ArrayList<>();
    private String configfile = "";
    private String workdir = "";
    private String loglevel = "DEBUG";
    private Map<String, String> options = new LinkedHashMap<>();

    /**
     * 根据命令行参数构建配置对象
     *
     * @param args
     * @return
     */
    public static final ServerRunnerOpt loadFromOpts(String[] args) {
        OptionParser parser = new OptionParser();
        parser.accepts("type").withOptionalArg();
        parser.accepts("port").withOptionalArg().ofType(Integer.class);
        parser.accepts("context").withOptionalArg();
        parser.accepts("webappdir").withOptionalArg();
        parser.accepts("classesdir").withOptionalArg();
        parser.accepts("resourcesdir").withOptionalArg();
        parser.accepts("configfile").withOptionalArg();
        parser.accepts("workdir").withOptionalArg();
        parser.accepts("loglevel").withOptionalArg();


        ServerRunnerOpt sro = new ServerRunnerOpt();

        OptionSet osts = parser.parse((args != null) ? args : new String[]{});
        if (osts == null) return sro;

        sro.type = StringUtils.upperCase(optString(osts, "type", "TOMCAT"));
        sro.port = optInteger(osts, "port", 8080);
        sro.context = optString(osts, "context", "");
        sro.webappdir = optString(osts, "webappdir", "");
        sro.classesdirs.addAll(optString(osts, "classesdir"));
        sro.resourcesdirs.addAll(optString(osts, "resourcesdir"));
        sro.configfile = optString(osts, "configfile", "");
        sro.workdir = optString(osts, "workdir", "");
        sro.loglevel = StringUtils.upperCase(optString(osts, "loglevel", "INFO"));

        return sro;
    }

    private static final String optString(OptionSet osts, String name, String defaultValue) {
        if (osts == null) return defaultValue;
        if (osts.has(name)) {
            Object o = osts.valueOf(name);
            if (o == null) return defaultValue;
            String s = o.toString();
            if (StringUtils.isBlank(s)) return defaultValue;
            return s;
        }
        return defaultValue;
    }

    private static final int optInteger(OptionSet osts, String name, int defaultValue) {
        if (osts == null) return defaultValue;
        if (osts.has(name)) {
            Object o = osts.valueOf(name);
            if (o == null) return defaultValue;

            int i = 0;
            if (o instanceof Number) i = ((Number) o).intValue();

            return (i == 0) ? defaultValue : i;
        }
        return defaultValue;
    }

    private static final List<String> optString(OptionSet osts, String name) {
        List<String> list = new ArrayList<>();
        if (osts == null) return list;
        if (osts.has(name)) {
            List<?> objects = osts.valuesOf(name);
            if (objects != null) for (Object o : objects) {
                if (o == null) continue;
                String s = o.toString();
                if (StringUtils.isBlank(s)) continue;
                list.add(StringUtils.trim(s));
            }
        }
        return list;
    }

    public String toParams() {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(this.type)) sb.append("--type=").append(this.type).append(" ");
        if (this.port > 0) sb.append("--port=").append(this.port).append(" ");
        if (StringUtils.isNotBlank(this.context)) sb.append("--context=").append(this.context).append(" ");
        if (StringUtils.isNotBlank(this.webappdir)) sb.append("--webappdir=").append(this.webappdir).append(" ");
        for (String s : this.classesdirs) {
            if (StringUtils.isNotBlank(s)) sb.append("--classesdir=").append(s).append(" ");
        }
        for (String s : this.classesdirs) {
            if (StringUtils.isNotBlank(s)) sb.append("--resourcesdir=").append(s).append(" ");
        }
        if (StringUtils.isNotBlank(this.configfile)) sb.append("--configfile=").append(this.configfile).append(" ");
        if (StringUtils.isNotBlank(this.workdir)) sb.append("--workdir=").append(this.workdir).append(" ");
        if (StringUtils.isNotBlank(this.loglevel)) sb.append("--loglevel=").append(this.loglevel).append(" ");

        // options 额外配置选项无法作为其他参数附加

        return sb.toString();
    }

    // BUILDER 方法
    public ServerRunnerOpt type(String type) {
        this.type = type;
        return this;
    }

    public ServerRunnerOpt port(int port) {
        this.port = port;
        return this;
    }

    public ServerRunnerOpt context(String context) {
        this.context = context;
        return this;
    }

    public ServerRunnerOpt webappdir(String webappdir) {
        this.webappdir = webappdir;
        return this;
    }

    public ServerRunnerOpt classesdir(String... classesdirs) {
        if (this.classesdirs == null) this.classesdirs = new ArrayList<>();
        if (classesdirs != null) this.classesdirs.addAll(Arrays.asList(classesdirs));
        return this;
    }

    public ServerRunnerOpt resourcesdir(String... resourcesdirs) {
        if (this.resourcesdirs == null) this.resourcesdirs = new ArrayList<>();
        if (resourcesdirs != null) this.resourcesdirs.addAll(Arrays.asList(resourcesdirs));
        return this;
    }

    public ServerRunnerOpt configfile(String configfile) {
        this.configfile = configfile;
        return this;
    }

    public ServerRunnerOpt workdir(String workdir) {
        this.workdir = workdir;
        return this;
    }

    public ServerRunnerOpt loglevel(String loglevel) {
        this.loglevel = loglevel;
        return this;
    }

    public ServerRunnerOpt option(String name, String value) {
        if (this.options == null) this.options = new LinkedHashMap<>();
        if (name == null) name = "";
        if (value == null) value = "";

        this.options.put(name, value);

        return this;
    }

    public ServerRunnerOpt option(Map<String, String> opts) {
        if (this.options == null) this.options = new LinkedHashMap<>();
        if (opts != null) this.options.putAll(opts);

        return this;
    }


    // GETTER / SETTER 方法
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getWebappdir() {
        return webappdir;
    }

    public void setWebappdir(String webappdir) {
        this.webappdir = webappdir;
    }

    public List<String> getClassesdirs() {
        return classesdirs;
    }

    public void setClassesdirs(List<String> classesdirs) {
        this.classesdirs = classesdirs;
    }

    public List<String> getResourcesdirs() {
        return resourcesdirs;
    }

    public void setResourcesdirs(List<String> resourcesdirs) {
        this.resourcesdirs = resourcesdirs;
    }

    public String getConfigfile() {
        return configfile;
    }

    public void setConfigfile(String configfile) {
        this.configfile = configfile;
    }

    public String getWorkdir() {
        return workdir;
    }

    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }

    public String getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "ServerRunnerOpt{" +
                "type='" + type + '\'' +
                ", port=" + port +
                ", context='" + context + '\'' +
                ", webappdir='" + webappdir + '\'' +
                ", classesdirs=" + classesdirs +
                ", resourcesdirs=" + resourcesdirs +
                ", configfile='" + configfile + '\'' +
                ", workdir='" + workdir + '\'' +
                ", loglevel='" + loglevel + '\'' +
                ", options=" + options +
                '}';
    }
}
