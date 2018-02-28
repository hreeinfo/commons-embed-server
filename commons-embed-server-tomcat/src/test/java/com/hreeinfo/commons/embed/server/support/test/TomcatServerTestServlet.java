package com.hreeinfo.commons.embed.server.support.test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>创建作者：xingxiuyi </p>
 * <p>创建日期：2018/2/28 </p>
 * <p>版权所属：xingxiuyi </p>
 */
public class TomcatServerTestServlet  extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("hello tomcat");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("hello tomcat");
    }
}
