package com.netflix.hystrix.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility to have 'intern' - like functionality, which holds single instance of wrapper for a given key
 */
public class InternMap<K, V> {

    /**
     * 映射
     */
    private final ConcurrentMap<K, V> storage = new ConcurrentHashMap<K, V>();

    /**
     * 值( value ) 构造方法
     */
    private final ValueConstructor<K, V> valueConstructor;

    public interface ValueConstructor<K, V> {
        V create(K key);
    }

    public InternMap(ValueConstructor<K, V> valueConstructor) {
        this.valueConstructor = valueConstructor;
    }

    /**
     * 获得值
     *
     * 1. 优先从 {@link #storage} 获取
     * 2. 若获取不到，使用 {@link #valueConstructor} 创建，并添加到 {@link #storage}
     * 3. 返回值
     *
     * @param key 标识
     * @return 值( value )
     */
    public V interned(K key) {
        V existingKey = storage.get(key);
        V newKey = null;
        if (existingKey == null) {
            newKey = valueConstructor.create(key);
            existingKey = storage.putIfAbsent(key, newKey);
        }
        return existingKey != null ? existingKey : newKey;
    }

    public int size() {
        return storage.size();
    }
}
