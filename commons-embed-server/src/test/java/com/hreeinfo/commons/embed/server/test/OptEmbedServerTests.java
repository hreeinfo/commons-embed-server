package com.hreeinfo.commons.embed.server.test;

import com.hreeinfo.commons.embed.server.EmbedServer.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/4/3 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class OptEmbedServerTests {
    private static String[] buildArgs() {
        List<String> args = new ArrayList<>();

        args.add("--port=8888");
        args.add("--context=/app");
        args.add("--webapp=/path/webapp");
        args.add("--workingdir=/path/workingdir");
        args.add("--lockfile=/path/lockfile");
        args.add("--classesdir=/dir1/file1:/dir1/file2:/dir2/file2:/dir2/file2:");
        args.add("--resourcesdir=/dir1/file1:/dir1/file2");
        args.add("--resourcesdir=/dir1/file");
        args.add("--configfile=/path/configfile");
        args.add("--loglevel=INFO");
        args.add("--classpath=/dir1/jar1:/dir1/jar2");
        args.add("--option=aa:bb");
        args.add("--option=cc:");
        args.add("--option=bb");
        args.add("--option=dd:bb");
        args.add("--option=ss:bb");
        args.add("--option=ee:bb");
        args.add("--argsfile=somefile");

        return args.toArray(new String[]{});
    }

    public static void main(String[] args) {
        Builder builder = Builder.builder();

        builder.opts(buildArgs());

        System.out.println(builder.toString());
    }
}
