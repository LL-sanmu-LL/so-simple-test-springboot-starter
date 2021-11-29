package com.zj;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        Class superclass = clazz.getSuperclass();
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
        for (Method method : bean.getClass().getDeclaredMethods()) {
            try {
                if (methodName.equals(method.getName())) {
                    result = InterfaceParametersUtils.getInterfaceInputJsonString(bean.getClass().getName(), method.getName());
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        Class realBeanClass = AopUtils.getTargetClass(bean);
        Object result = null;

        try {
            // 每个接口均存在InvokeParamVo参数，加入到参数列表
            // 1、单个参数{}，构造成[]
            // 2/多参数[]，直接将InvokeParamVo加在第一个参数

            boolean isInvoked = false;
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
                // cglib动态代理
                for (Method method : realBeanClass.getDeclaredMethods()) {
                    if (!methodList.contains(method)) {
                        methodList.add(method);
                    }
                }

                for (Method method : methodList) {
                    try {
                        if (methodName.equals(method.getName())) {
                            isInvoked = true;
                            result = invokeMethod(bean, method, param);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (isInvoked) {
                        break;
                    }
                }
            } else {
                // 非接口实现类
                for (Method method : realBeanClass.getDeclaredMethods()) {
                    try {
                        if (methodName.equals(method.getName())) {
                            isInvoked = true;
                            result = invokeMethod(bean, method, param);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (isInvoked) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Object invokeMethod(Object bean, Method targetMethod, String inputParams) throws Exception {
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