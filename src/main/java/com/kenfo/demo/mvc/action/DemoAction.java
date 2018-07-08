package com.kenfo.demo.mvc.action;

import com.kenfo.demo.service.IDemoService;
import com.kenfo.mvcframework.annotation.KFAutowired;
import com.kenfo.mvcframework.annotation.KFController;
import com.kenfo.mvcframework.annotation.KFRequestMapping;
import com.kenfo.mvcframework.annotation.KFRequestParam;

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
@KFController
@KFRequestMapping("/web")
public class DemoAction {

    @KFAutowired
    IDemoService iDemoService;

    @KFRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @KFRequestParam("name") String name){
        String result = iDemoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
