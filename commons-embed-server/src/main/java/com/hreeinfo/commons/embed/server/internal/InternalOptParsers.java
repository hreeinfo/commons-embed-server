package com.hreeinfo.commons.embed.server.internal;

import com.hreeinfo.commons.embed.server.BaseEmbedServer;
import com.hreeinfo.commons.embed.server.EmbedServer;
import joptsimple.OptionSet;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/3/24 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public final class InternalOptParsers {
    private InternalOptParsers() {
    }

    public static final String optString(OptionSet osts, String name, String defaultValue) {
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

    public static final int optInteger(OptionSet osts, String name, int defaultValue) {
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
