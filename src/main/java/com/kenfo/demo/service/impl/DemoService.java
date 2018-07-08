package com.kenfo.demo.service.impl;

import com.kenfo.demo.service.IDemoService;
import com.kenfo.mvcframework.annotation.KFService;

/**
 * @author kenfo
 * @version V1.0
 * @Package com.gupaoedu.demo.service.impl
 * @Description: TODO
 * @date 2018/2/11 下午1:48
 */
@KFService
public class DemoService implements IDemoService {
    public String get(String name) {
        return "my name is " + name;
    }
}
