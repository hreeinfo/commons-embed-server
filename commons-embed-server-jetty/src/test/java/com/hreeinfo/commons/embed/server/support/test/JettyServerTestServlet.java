package com.hreeinfo.commons.embed.server.support.test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/25 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class JettyServerTestServlet extends HttpServlet{
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("尝试重新初始化 servlet");
    }

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("尝试重新初始化 servlet");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("hello jetty");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("hello jetty");
    }
}
