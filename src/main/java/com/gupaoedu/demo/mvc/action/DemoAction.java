package com.gupaoedu.demo.mvc.action;

import com.gupaoedu.demo.service.IDemoService;
import com.gupaoedu.mvcframework.annotation.GPAutowired;
import com.gupaoedu.mvcframework.annotation.GPController;
import com.gupaoedu.mvcframework.annotation.GPRequestMapping;
import com.gupaoedu.mvcframework.annotation.GPRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author kenfo
 * @version V1.0
 * @Package com.gupaoedu.demo.mvc.action
 * @Description: TODO
 * @date 2018/2/11 下午1:45
 */
@GPController
@GPRequestMapping("/web")
public class DemoAction {

    @GPAutowired
    IDemoService iDemoService;

    @GPRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @GPRequestParam("name") String name){
        String result = iDemoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
