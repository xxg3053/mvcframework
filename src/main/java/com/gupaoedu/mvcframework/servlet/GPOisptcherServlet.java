package com.gupaoedu.mvcframework.servlet;

import com.gupaoedu.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kenfo
 * @version V1.0
 * @Package com.gupaoedu.mvcframework.servlet
 * @Description: TODO
 * @date 2018/2/11 下午1:28
 */
public class GPOisptcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Object> ioc = new HashMap<String, Object>();

    //private Map<String, Method> handlerMapping = new HashMap<String, Method>();
    private List<Handler> handlerMapping = new ArrayList<Handler>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception,Detail:" + Arrays.toString(e.getStackTrace()));
        }

    }

    /**
     *
     //1. 加载配置文件applaction.properties, 代替applaction.xml
     //2. 扫描到所有相关的类
     //3. 把扫描到的所有的类实例化，放到IOC容器中（我们自己时间一个IOC容器,说白了是个map）
     //4. 检查注入，只要加了@GPAutowired注解的字段，不管它是私有的还是公有的，还是受保护的都要给他强制赋值
     //5. 获取用户的请求，根据所请求的url去找到对应的其method，通过反射机制去调用
     //handlerMapping 把这样一个关系存放到handleMapping中，说白了是个map
     //6. 等待请求，把反射调用的结果通过response写出到浏览器中
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1. 加载配置文件applaction.properties, 代替applaction.xml
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2. 扫描到所有相关的类,拿到基础包路径，递归扫描
        doScanner(properties.getProperty("scanPackage"));
        //3. 把扫描到的所有的类实例化，放到IOC容器中（我们自己时间一个IOC容器,说白了是个map）
        doInstance();
        //4. 检查注入，只要加了@GPAutowired注解的字段，不管它是私有的还是公有的，还是受保护的都要给他强制赋值
        doAutowired();
        //5. 获取用户的请求，根据所请求的url去找到对应的其method，通过反射机制去调用
             //handlerMapping 把这样一个关系存放到handleMapping中，说白了是个map
        initHandleMapping();
        //6. 等待请求，把反射调用的结果通过response写出到浏览器中

    }

    private void doLoadConfig(String path){
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void doScanner(String packageName){
        URL url = this.getClass().getClassLoader()
                .getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());

        for (File file : dir.listFiles()){
            if(file.isDirectory()){
                doScanner(packageName + "." + file.getName());
            }else{
                classNames.add(packageName + "." + file.getName().replace(".class", ""));
            }
        }

    }
    private void doInstance(){
        if(classNames.isEmpty()){
            return;
        }
        try{
            for(String className: classNames){
                Class<?> clazz = Class.forName(className);
                //不是所有类都要初始化
                if(clazz.isAnnotationPresent(GPController.class)){
                    //<bane id="" name="" class="">
                    String beanName = lowerFirst(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                }else if(clazz.isAnnotationPresent(GPService.class)){
                    //1. 如果取了名字，优先使用自己的名字进行匹配并注入
                    //2. 如果自己没起名字，默认首字母小写（发生在不是接口的情况）
                    //3. 如果注入的类型是接口的话，要自动找到其实现类的实例
                    GPService service = clazz.getAnnotation(GPService.class);
                    String beanName = service.value();//如果设了值则不为空
                    if("".equals(beanName.trim())){
                        ioc.put(beanName, clazz.newInstance());
                    }else {
                        beanName = lowerFirst(clazz.getSimpleName());
                        ioc.put(beanName, clazz.newInstance());
                    }
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class<?> i: interfaces){
                        ioc.put(i.getName(), clazz.newInstance());
                    }

                }else{
                    continue;
                }
            }
        }catch(Exception e){
             e.printStackTrace();
        }
    }
    private void doAutowired(){
        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry<String, Object> entry: ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field:fields){
                if(!field.isAnnotationPresent(GPAutowired.class)){
                    continue;
                }
                //如果注解加了自定义名字
                GPAutowired gpAutowired = field.getAnnotation(GPAutowired.class);
                String beanName = gpAutowired.value().trim();
                //通过声明接口注入
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                //强制赋值
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void initHandleMapping(){
        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry<String, Object> entry: ioc.entrySet()){
            //非常具有技术含量的地方
            //把所有的requestMapping扫描出来，读取它的值，跟method关联上，并且放到handleMapping中去
            Class<?> clazz = entry.getValue().getClass();
            //只跟GPcontroller有关
            if(!clazz.isAnnotationPresent(GPController.class)){
                continue;
            }
            String baseUrl = "";
            if(clazz.isAnnotationPresent(GPRequestMapping.class)){
                GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for(Method method:methods){
                if(!method.isAnnotationPresent(GPRequestMapping.class)){
                    continue;
                }
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                String mappingUrl = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");

                //handlerMapping.put(mappingUrl, method);
                //System.out.println("Mapping : " + mappingUrl + ", method: " + method);

                Pattern pattern = Pattern.compile(mappingUrl);
                handlerMapping.add(new Handler(pattern, entry.getValue(), method));
                System.out.println("Mapping " + mappingUrl + "," + method);
            }
        }
    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();

        String contextPath = req.getContextPath();
        url.replace(contextPath, "").replaceAll("/+", "/");
        System.out.println(url);
        /**
        if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 not found");
            return;
        }

        Method method = handlerMapping.get(url);
        System.out.println("成功获取到即将要调用的method:" + method);

        //第一个参数表示这个方式的实例
         **/

        Handler handler = getHandler(req);
        if(handler == null){
            resp.getWriter().write("404 not found");
            return;
        }
        //参数列表
        Class<?>[] paramTypes = handler.method.getParameterTypes();

        Object[] paramsValues = new Object[paramTypes.length];
        Map<String,String[]> params = req.getParameterMap();
        for(Map.Entry<String, String[]> param:params.entrySet()){
            String value = Arrays.toString(param.getValue())
                    .replaceAll("\\[|\\]", "")
                    .replaceAll(",\\s", ",");

            if(!handler.paramIndexMapping.containsKey(param.getKey())){
                continue;
            }
            int index = handler.paramIndexMapping.get(param.getKey());
            paramsValues[index] = convert(paramTypes[index], value);

        }
        int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
        paramsValues[reqIndex] = req;
        int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
        paramsValues[respIndex] = resp;

        handler.method.invoke(handler.controller, paramsValues);

    }

    private Handler getHandler(HttpServletRequest req) throws Exception{
        if(handlerMapping.isEmpty()){
            return null;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        for(Handler handler:handlerMapping){
            try{
                Matcher matcher = handler.pattern.matcher(url);
                if(!matcher.matches()){
                    continue;
                }
                return handler;
            }catch (Exception e){
                throw  e;
            }
        }
        return null;
    }

    private class Handler{

        protected Object controller; //保存方式对应的实例
        protected Method method;     //保存反射的方法
        protected Pattern pattern;   //记得spring的url支持正则
        protected Map<String, Integer> paramIndexMapping; //参数顺序

        protected Handler(Pattern pattern, Object controller, Method method){
            this.controller =  controller;
            this.method = method;
            this.pattern = pattern;

            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method){
            Annotation[][] pa = method.getParameterAnnotations();
            for(int i=0; i<pa.length; i++){
                for(Annotation a: pa[i]){
                    if(a instanceof GPRequestParam){
                        String paramName = ((GPRequestParam)a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            Class<?>[] paramsTypes = method.getParameterTypes();
            for(int i=0; i<paramsTypes.length; i++){
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){

                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }

    }

    private Object convert(Class<?> type, String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        return value;
    }
    /**
     * 首字母小写
     * @param str
     * @return
     */
    private String lowerFirst(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
