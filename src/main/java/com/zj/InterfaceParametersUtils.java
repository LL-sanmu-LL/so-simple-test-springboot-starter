package com.zj;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.zj.GenerateNodeService.generateNode;


@Slf4j
public class InterfaceParametersUtils {

    /**
     * 根据解决类名和方法名，获取接口参数
     */
    public static List<Object> getInterfaceInputJsonString(String interfaceName, String methodName) {
        Method[] methods;
        List<Object> jsonInput = new ArrayList<>();
        try {
            methods = Class.forName(interfaceName).getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    jsonInput.add(getMethodsParameterJson(method));
                }
            }
        } catch (Exception e) {
            log.error(String.format("获取%s Bean %s方法 参数列表失败！", interfaceName, methodName), e);
        }
        return jsonInput;
    }


    public static Object getMethodsParameterJson(Method method) {
        Type[] ts = method.getGenericParameterTypes();
        List<Object> list = new ArrayList<>();
        for (Type t : ts) {
            list.add((getInstance((Class<?>) t)));
        }

        return list.size() > 1 ? list : list.get(0);

    }

    private static Object getInstance(Class<?> clazz){
        try {

            return  generateNode(clazz);
        }catch (Exception e){
            return null;
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object createInstance(Type type) {
        Object obj = createSimpleObject(type);
        if (type instanceof ParameterizedType) {
            Type[] ts = ((ParameterizedType) type).getActualTypeArguments();
            if (ts.length == 1) {
                Type trueType = ts[0];
                ((List) obj).add(createInstance(trueType));
            } else if (ts.length == 2) {
                Type keyType = ts[0];
                Type valType = ts[1];
                Object key = createInstance(keyType);
                Object val = createInstance(valType);
                ((Map) obj).put(key, val);
            }
        } else {
            try {
                obj = ((Class) type).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return obj;
    }

    private static Object createSimpleObject(Type clazz) {
        Object o = null;
        // 这里对性能要求不高，只要能生成一个对象即可
        try {
            o = JSON.parseObject("0", clazz);
            if (o instanceof String) {
                return "";
            }
        } catch (Exception e) {
            try {
                o = JSON.parseObject("[]", clazz);
            } catch (Exception ee) {
                try {
                    o = JSON.parseObject("{}", clazz);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
        return o;
    }
}
