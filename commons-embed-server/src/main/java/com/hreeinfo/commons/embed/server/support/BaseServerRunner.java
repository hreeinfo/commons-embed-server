package com.hreeinfo.commons.embed.server.support;

import com.hreeinfo.commons.embed.server.ServerRunner;
import com.hreeinfo.commons.embed.server.ServerRunnerOpt;

/**
 * 基础类 封装了各个实现的基本功能
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/19 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public abstract class BaseServerRunner implements ServerRunner {
    protected final ServerRunnerOpt opt;

    public BaseServerRunner(ServerRunnerOpt opt) {
        this.opt = opt;
    }
}
