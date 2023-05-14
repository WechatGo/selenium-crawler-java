package com.harperdog.seleniumcrawlerjava.utils;

import org.springframework.cglib.beans.BeanCopier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BeanCopierUtils {

    // 创建一个map来存储BeanCopier
    private static final Map<String, BeanCopier> beanCopierMap = new HashMap<>();

    /**
     * 深拷贝,我们可以直接传实例化的拷贝对象和被实例化的拷贝对象进行深拷贝
     *
     * @param str
     * @param target
     */
    public static void copy(Object str, Object target) {
        if (str == null) {
            return;
        }
        // 用来判断空指针异常
        Objects.requireNonNull(target);
        String key = getKey(str, target);
        BeanCopier beanCopier;
        // 判断键是否存在，不存在就将BeanCopier插入到map里，存在就直接获取
        if (!beanCopierMap.containsKey(key)) {
            beanCopier = BeanCopier.create(str.getClass(), target.getClass(), false);
            beanCopierMap.put(key, beanCopier);
        } else {
            beanCopier = beanCopierMap.get(key);
        }
        beanCopier.copy(str, target, null);
    }

    /**
     * 深拷贝
     *
     * @param str
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T copy(Object str, Class<T> tClass) {
        if (str == null) {
            return null;
        }
        // 用来判断空指针异常
        Objects.requireNonNull(tClass);
        T dest = null;
        try {
            dest = tClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        copy(str, dest);
        return dest;
    }

    /**
     * List深拷贝
     *
     * @param strList
     * @param tClass
     * @param <T>
     * @param <S>
     * @return
     */
    public static <T, S> List<T> copyList(List<S> strList, Class<T> tClass) {
        // 判断空指针异常
        Objects.requireNonNull(tClass);
        return strList.stream().map(src -> copy(src, tClass)).collect(Collectors.toList());

    }

    /**
     * 获取Map Key
     *
     * @param str
     * @param target
     * @return
     */
    private static String getKey(Object str, Object target) {
        return str.getClass().getName() + target.getClass().getName();
    }
}


