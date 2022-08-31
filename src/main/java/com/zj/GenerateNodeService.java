package com.zj;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

public class GenerateNodeService {

    public static Object generateNode(Class clz) throws Exception {
        Map<String, Method> methodMap = getClassMethod(clz);
        Object obj = null;
        if (isBaseType(clz)) {
            if (clz != Integer.class && clz != Integer.TYPE) {
                if (clz != Long.class && clz != Long.TYPE) {
                    if (clz != Boolean.class && clz != Boolean.TYPE) {
                        if (clz == BigDecimal.class) {
                            obj = new BigDecimal(0);
                        } else if (clz == String.class) {
                            obj = "";
                        } else {
                            obj = clz.newInstance();
                        }
                    } else {
                        obj = false;
                    }
                } else {
                    obj = 0L;
                }
            } else {
                obj = 0;
            }

            return obj;
        } else {
            try {
                if(clz.isInterface()){
                    return obj;
                }
                if(clz.isEnum()){
                    return obj;
                }
                obj = clz.newInstance();
                List<Field> fields = getClassField(clz);
                Iterator var4 = fields.iterator();

                while(var4.hasNext()) {
                    Field field = (Field)var4.next();
                    if (!field.getName().equals("serialVersionUID") && !isBaseType(field.getType())) {
                        Object fieldObj = null;
                        Class genericKeyClz;
                        Object key;
                        if (field.getType() == List.class) {
                            genericKeyClz = getGenericType(field, 0);
                            fieldObj = new ArrayList();
                            List listObj = (List)fieldObj;
                            key = generateNode(genericKeyClz);
                            listObj.add(key);
                        } else if (field.getType() == Map.class) {
                            fieldObj = new HashMap();
                            genericKeyClz = getGenericType(field, 0);
                            Class genericValueClz = getGenericType(field, 1);
                            key = generateNode(genericKeyClz);
                            Object value = generateNode(genericValueClz);
                            Map mapObj = (Map)fieldObj;
                            mapObj.put(key, value);
                        } else {
                            fieldObj = generateNode(field.getType());
                        }

                        String methodName = "set" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1, field.getName().length());
                        Method method = (Method)methodMap.get(methodName);
                        method.invoke(obj, fieldObj);
                    }
                }
            } catch (Exception var12) {
                var12.printStackTrace();
            }

            return obj;
        }
    }

    private static Class getGenericType(Field f, int postion) throws Exception {
        Type genericType = f.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)genericType;
            Class genericClazz = null;

            try {
                genericClazz = (Class)pt.getActualTypeArguments()[postion];
            } catch (Exception var7) {
                ParameterizedType pt1 = (ParameterizedType)pt.getActualTypeArguments()[postion];
                genericClazz = (Class)pt1.getActualTypeArguments()[0];
            }

            Class clz = Class.forName(genericClazz.getName());
            return clz;
        } else {
            return null;
        }
    }

    private static boolean isBaseType(Class clz) {
        return clz == Integer.class || clz == Integer.TYPE || clz == String.class || clz == Boolean.class || clz == Boolean.TYPE || clz == Long.class || clz == Long.TYPE || clz == BigDecimal.class || clz == Date.class;
    }

    private static List<Field> getClassField(Class clz) {
        List<Field> declaredFields = Arrays.asList(clz.getDeclaredFields());
        List<Field> arrayFields = new ArrayList(declaredFields);
        Class superclass = clz.getSuperclass();
        if (superclass != null) {
            arrayFields.addAll(getClassField(superclass));
        }

        return arrayFields;
    }

    private static Map<String, Method> getClassMethod(Class clz) {
        Map<String, Method> methodMap = new HashMap();
        Method[] var2 = clz.getDeclaredMethods();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Method method = var2[var4];
            methodMap.put(method.getName(), method);
        }

        Class superclass = clz.getSuperclass();
        if (superclass != null) {
            mergeMap(methodMap, getClassMethod(superclass));
        }

        return methodMap;
    }

    private static <K, V> Map<K, V> mergeMap(Map<K, V> resultMap, Map<K, V> addMap) {
        if (null != addMap && addMap.size() > 0) {
            Iterator var2 = addMap.entrySet().iterator();

            while(var2.hasNext()) {
                Entry<K, V> entry = (Entry)var2.next();
                resultMap.put(entry.getKey(), entry.getValue());
            }
        }

        return resultMap;
    }
}
