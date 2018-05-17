package com.hreeinfo.commons.embed.server.internal;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
import joptsimple.OptionSet;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>版权所属：xingxiuyi </p>
 */
public final class InternalOptParsers {
    private InternalOptParsers() {
    }

    /**
     * 返回对象 如果多次定义以最后定义的一个为准
     *
     * @param osts
     * @param name
     * @return
     */
    public static final Object optObject(OptionSet osts, String name) {
        if (osts == null || StringUtils.isBlank(name)) return null;
        try {
            if (osts.has(name)) {
                List<?> values = osts.valuesOf(name);
                if (values != null && values.size() > 0) return values.get(values.size() - 1);
            }
        } catch (Throwable e) {
        }

        return null;
    }

    public static final String optString(OptionSet osts, String name, String defaultValue) {
        if (osts == null) return defaultValue;
        Object o = optObject(osts, name);
        if (o == null) return defaultValue;
        String s = o.toString();
        if (StringUtils.isBlank(s)) return defaultValue;
        return s;
    }

    public static final int optInteger(OptionSet osts, String name, int defaultValue) {
        if (osts == null) return defaultValue;
        Object o = optObject(osts, name);
        if (o == null) return defaultValue;

        int i = 0;
        if (o instanceof Number) i = ((Number) o).intValue();

        return (i == 0) ? defaultValue : i;
    }

    public static final List<String> optString(OptionSet osts, String name) {
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

    public static final void opt(OptionSet osts, String name, Consumer<Object> consumer) {
        if (osts == null || consumer == null || name == null) return;
        if (osts.has(name)) {
            List<?> objects = osts.valuesOf(name);
            if (objects != null) for (Object o : objects) {
                consumer.accept(o);
            }
        }
    }

    public static void optAppendPathValues(Object o, List<String> dirs) {
        if (dirs == null || o == null) return;

        String s = o.toString();

        if (StringUtils.isBlank(s)) return;
        else if (StringUtils.contains(s, ":")) {
            String[] sp = StringUtils.split(s, ":");
            if (sp != null) for (String p : sp) {
                if (StringUtils.isBlank(p)) continue;
                dirs.add(StringUtils.trim(p));
            }
        } else dirs.add(StringUtils.trim(s));
    }

    public static void optAppendMaValues(Object o, Map<String, String> opts) {
        if (opts == null || o == null) return;

        String s = o.toString();

        if (StringUtils.isNotBlank(s) && StringUtils.contains(s, ":")) {
            List<String> pslist = new ArrayList<>();
            if (StringUtils.contains(s, ",")) {
                String[] psp = StringUtils.split(s, ",");
                if (psp != null) pslist.addAll(Arrays.asList(psp));
            } else pslist.add(s);

            for (String ps : pslist) {
                String[] sp = StringUtils.split(ps, ":");

                if (sp.length > 0 && StringUtils.isNotBlank(sp[0])) {
                    String k = StringUtils.trim(sp[0]);
                    if (sp.length > 1) opts.put(k, StringUtils.trim(sp[1]));
                    else opts.put(k, null);
                }
            }
        }
    }

    public String toParams(EmbedServer es) {
        StringBuilder sb = new StringBuilder();

        if (!(es instanceof BaseEmbedServer)) return sb.toString();

        BaseEmbedServer server = (BaseEmbedServer) es;

        if (StringUtils.isNotBlank(server.getType())) sb.append("--type=").append(server.getType()).append(" ");
        if (server.getPort() > 0) sb.append("--port=").append(server.getPort()).append(" ");
        if (StringUtils.isNotBlank(server.getContext()))
            sb.append("--context=").append(server.getContext()).append(" ");
        if (StringUtils.isNotBlank(server.getWebapp()))
            sb.append("--webappdir=").append(server.getWebapp()).append(" ");
        for (String s : server.getClassesdirs()) {
            if (StringUtils.isNotBlank(s)) sb.append("--classesdir=").append(s).append(" ");
        }
        for (String s : server.getResourcesdirs()) {
            if (StringUtils.isNotBlank(s)) sb.append("--resourcesdir=").append(s).append(" ");
        }
        if (StringUtils.isNotBlank(server.getConfigfile()))
            sb.append("--configfile=").append(server.getConfigfile()).append(" ");
        if (StringUtils.isNotBlank(server.getLoglevel()))
            sb.append("--loglevel=").append(server.getLoglevel()).append(" ");

        // options 额外配置选项无法作为其他参数附加

        return sb.toString();
    }
}
