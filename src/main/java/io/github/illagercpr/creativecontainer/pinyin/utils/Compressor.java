/*
 * PinIn 1.6.0 - vendored copy.
 * Copyright (c) 2020-2023 Juntong Liu (Towdium), released under the MIT license.
 * Original project: https://github.com/Towdium/PinIn
 * Vendored into Creative Container on 2026-09-28; only the package was renamed
 * (me.towdium.pinin -> io.github.illagercpr.creativecontainer.pinyin), the code is untouched.
 */

package io.github.illagercpr.creativecontainer.pinyin.utils;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Compressor implements Accelerator.Provider {
    CharList chars = new CharArrayList();
    IntList strs = new IntArrayList();

    public IntList offsets() {
        return strs;
    }

    public int put(String s) {
        strs.add(chars.size());
        for (int i = 0; i < s.length(); i++)
            chars.add(s.charAt(i));
        chars.add('\0');
        return strs.getInt(strs.size() - 1);
    }

    @Override
    public boolean end(int i) {
        return chars.getChar(i) == '\0';
    }

    @Override
    public char get(int i) {
        return chars.getChar(i);
    }
}