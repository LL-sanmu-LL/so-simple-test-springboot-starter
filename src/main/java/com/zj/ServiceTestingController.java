package com.zj;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RequestMapping({"**/testing"})
public class ServiceTestingController {
    /**
     * 静态页面
     */
    @RequestMapping("/index")
    public void index(HttpServletResponse response) {
        response.setContentType("text/html");
        StringBuilder content = new StringBuilder();
        InputStream resourceAsStream = getClass().getResourceAsStream("/webapp/index.html");
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
            bufferedReader.close();
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(content);
        } catch (IOException e) {
            try {
                response.getWriter().print("Html file read error");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * 获取所有服务
     */
    @RequestMapping("/services")
    @ResponseBody
    public ServiceTestingResponse<List<String>> getServices() {
        String[] beans = ServiceTestingContext.getBeans();
        List<String> services = new ArrayList<>();
        for (String bean : beans) {
            if (bean.contains(".")) {
                continue;
            }

            if (ServiceTestingConfigure.filters.length == 0) {
                services.add(bean);
            } else {
                for (String filter : ServiceTestingConfigure.filters) {
                    if ((bean.toLowerCase().contains(filter.toLowerCase())) && !services.contains(bean)) {
                        services.add(bean);
                        break;
                    }
                }
            }
        }
        Collections.sort(services);

        return ServiceTestingResponse.success(services);
    }

    /**
     * 获取指定类的所有方法
     */
    @RequestMapping("/methods")
    @ResponseBody
    public ServiceTestingResponse<List<String>> getMethods(@RequestBody JSONObject request) {
        String serviceName = request.getString("serviceName");
        Object bean = ServiceTestingContext.getBean(serviceName);
        Class clazz;
        if (AopUtils.isJdkDynamicProxy(bean)) {
            InvocationHandler handler = Proxy.getInvocationHandler(bean);
            AdvisedSupport advised = (AdvisedSupport) new DirectFieldAccessor(handler)
                    .getPropertyValue("advised");
            clazz = advised.getTargetClass();
        } else {
            clazz = AopUtils.getTargetClass(bean);
        }
        List<String> ms = new ArrayList<>();
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodName = method.getName();
            if (!ms.contains(methodName) && !methodName.contains("$")) {
                ms.add(method.getName());
            }
        }
        // 父类接口方法
        Class<?> superclass = clazz.getSuperclass();
        Method[] superDeclaredMethods = null;
        if (superclass != null) {
            superDeclaredMethods = superclass.getDeclaredMethods();
        }
        if (superDeclaredMethods != null) {
            for (Method superDeclaredMethod : superDeclaredMethods) {
                String methodName = superDeclaredMethod.getName();
                if (!ms.contains(superDeclaredMethod) && !methodName.contains("$")) {
                    ms.add(superDeclaredMethod.getName());
                }
            }
        }
        Collections.sort(ms);
        return ServiceTestingResponse.success(ms);
    }

    /**
     * 加载指定方法的参数
     */
    @RequestMapping("/load")
    @ResponseBody
    public String loadParam(@RequestBody JSONObject request) {
        String serviceName = request.getString("serviceName");
        String methodName = request.getString("methodName");
        Object bean = ServiceTestingContext.getBean(serviceName);
        Object result = null;
        Class<?> clazz = bean.getClass();
        if(bean instanceof TargetClassAware){
            clazz = ((TargetClassAware) bean).getTargetClass();
        }
        if(clazz != null){
            for (Method method : clazz.getDeclaredMethods()) {
                try {
                    if (methodName.equals(method.getName())) {
                        result = InterfaceParametersUtils.getInterfaceInputJsonString(clazz.getName(), method.getName());
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        ServiceTestingResponse<Object> success = ServiceTestingResponse.success(result);
        return  JSON.toJSONString(success, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullBooleanAsFalse,
                SerializerFeature.WriteDateUseDateFormat, SerializerFeature.PrettyFormat);
    }
    /**
     * 调用指定方法
     */
    @RequestMapping("/request")
    @ResponseBody
    public Object request(@RequestBody JSONObject request) {
        String serviceName = request.getString("serviceName");
        String methodName = request.getString("methodName");
        String param = request.getString("param");
        Object bean = ServiceTestingContext.getBean(serviceName);
        // 获取Bean的真实类，这样保证JSON.parseObject按照类型对泛型，反序列化不出问题
        Class<?> realBeanClass = AopUtils.getTargetClass(bean);
        Object result = null;

        try {
            if (AopUtils.isCglibProxy(bean)){//cglib动态代理
                Method method = Arrays.stream(realBeanClass.getDeclaredMethods()).filter(v -> methodName.equals(v.getName())).findFirst().orElse(null);
                try {
                    return invokeMethod(bean, method, param);

                } catch (Exception e) {
                    log.error("invokeMethod failed", e);
                }
            }
            if(AopUtils.isJdkDynamicProxy(bean)){//jdk动态代理
                Proxy proxyBean = (Proxy)bean;
                InvocationHandler handler = (InvocationHandler) new DirectFieldAccessor(proxyBean)
                        .getPropertyValue("h");
                Class<?> clazz = ((TargetClassAware) bean).getTargetClass();
                if(clazz == null){
                    log.error("clazz is not exist");
                    return result;
                }
                Method method = Arrays.stream(clazz.getDeclaredMethods()).filter(v -> methodName.equals(v.getName())).findFirst().orElse(null);
                return invokeProxyMethod(handler, bean, method, param);
            }
            // 必须用getInterfaces，这样才能取到方法参数的泛型
            if (bean.getClass().isInterface()) {
                // 获取接口或类的所有方法
                List<Method> methodList = new ArrayList<>();
                for (Class<?> beanInterface : realBeanClass.getInterfaces()) {
                    for (Method method : beanInterface.getMethods()) {
                        if (!methodList.contains(method)) {
                            methodList.add(method);
                        }
                    }
                }
                for (Method method : realBeanClass.getDeclaredMethods()) {
                    if (!methodList.contains(method)) {
                        methodList.add(method);
                    }
                }

                Method method = methodList.stream().filter(v -> methodName.equals(v.getName())).findFirst().orElse(null);
                try {
                    return invokeMethod(bean, method, param);
                } catch (Exception e) {
                    log.error("invokeMethod failed", e);
                }
            } else {
                // 非接口实现类
                Method method = Arrays.stream(realBeanClass.getDeclaredMethods()).filter(v -> methodName.equals(v.getName())).findFirst().orElse(null);
                try {
                    return invokeMethod(bean, method, param);

                } catch (Exception e) {
                    log.error("invokeMethod failed", e);
                }
            }
        } catch (Throwable e) {
            log.error("invokeMethod failed", e);
        }

        return result;
    }

    public Object invokeMethod(Object bean, Method targetMethod, String inputParams) throws Exception {
        if(targetMethod == null){
            log.error("targetMethod is null");
            return null;
        }
        Type[] types = targetMethod.getGenericParameterTypes();
        List<String> paramList = processParams(targetMethod, inputParams);
        List<Object> params = new ArrayList<>();
        // 参数数量必须相同
        if (types.length == paramList.size()) {
            for (int i = 0; i < types.length; i++) {
                String paramJson = paramList.get(i);
                Object paramObject;
                try {
                    paramObject = types[i] == String.class ? paramJson : JSON.parseObject(paramJson, types[i]);
                } catch (Exception e) {
                    paramObject = paramList.get(i);
                }
                params.add(paramObject);
            }
            return targetMethod.invoke(bean, params.toArray());
        }
        return null;
    }

    public Object invokeProxyMethod(InvocationHandler handler, Object bean, Method targetMethod, String inputParams) throws Throwable {
        if(targetMethod == null){
            log.error("targetMethod is null");
            return null;
        }
        Type[] types = targetMethod.getGenericParameterTypes();
        List<String> paramList = processParams(targetMethod, inputParams);
        List<Object> params = new ArrayList<>();
        // 参数数量必须相同
        if (types.length == paramList.size()) {
            for (int i = 0; i < types.length; i++) {
                String paramJson = paramList.get(i);
                Object paramObject;
                try {
                    paramObject = types[i] == String.class ? paramJson : JSON.parseObject(paramJson, types[i]);
                } catch (Exception e) {
                    paramObject = paramList.get(i);
                }
                params.add(paramObject);
            }
            return handler.invoke(bean, targetMethod, params.toArray());
        }
        return null;
    }

    private List<String> processParams(Method method, String param) {
        List<String> paramList = new ArrayList<>();
        Type[] types = method.getGenericParameterTypes();
        if (types.length == 1) {
            paramList.add(param);
        }
        // 超过2个参数，将JSON参数转为List<String>，后续再逐个转为对应的对象
        if (types.length > 1) {
            List<String> tmepList = JSON.parseArray(param, String.class);
            paramList.addAll(tmepList);
        }

        return paramList;
    }
}