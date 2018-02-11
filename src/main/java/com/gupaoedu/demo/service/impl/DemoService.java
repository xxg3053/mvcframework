package com.gupaoedu.demo.service.impl;

import com.gupaoedu.demo.service.IDemoService;
import com.gupaoedu.mvcframework.annotation.GPService;

/**
 * @author kenfo
 * @version V1.0
 * @Package com.gupaoedu.demo.service.impl
 * @Description: TODO
 * @date 2018/2/11 下午1:48
 */
@GPService
public class DemoService implements IDemoService {
    public String get(String name) {
        return "my name is " + name;
    }
}
