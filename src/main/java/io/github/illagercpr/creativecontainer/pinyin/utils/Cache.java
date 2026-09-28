/*
 * PinIn 1.6.0 - vendored copy.
 * Copyright (c) 2020-2023 Juntong Liu (Towdium), released under the MIT license.
 * Original project: https://github.com/Towdium/PinIn
 * Vendored into Creative Container on 2026-09-28; only the package was renamed
 * (me.towdium.pinin -> io.github.illagercpr.creativecontainer.pinyin), the code is untouched.
 */

package io.github.illagercpr.creativecontainer.pinyin.utils;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Author: Towdium
 * Date: 04/03/19
 */
public class Cache<K, V> {
    HashMap<K, V> data = new HashMap<>();
    Function<K, V> generator;

    public Cache(Function<K, V> generator) {
        this.generator = generator;
    }

    public V get(K key) {
        V ret = data.get(key);
        if (ret == null) {
            ret = generator.apply(key);
            if (ret != null) data.put(key, ret);
        }
        return ret;
    }

    public void foreach(BiConsumer<K, V> c) {
        data.forEach(c);
    }

    public void clear() {
        data.clear();
    }
}