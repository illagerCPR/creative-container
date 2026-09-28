/*
 * PinIn 1.6.0 - vendored copy.
 * Copyright (c) 2019 Juntong Liu, released under the MIT license (see LICENSE-PinIn.txt).
 * Original project: https://github.com/Towdium/PinIn
 * Vendored into Creative Container on 2026-09-28 as PinIn 1.6.0; only the package was renamed
 * (me.towdium.pinin -> io.github.illagercpr.creativecontainer.pinyin), the code is untouched.
 */

package io.github.illagercpr.creativecontainer.pinyin.elements;

import io.github.illagercpr.creativecontainer.pinyin.utils.IndexSet;

public class Char implements Element {
    protected char ch;
    public static final Pinyin[] NONE = new Pinyin[0];

    Pinyin[] pinyin;

    public Char(char ch, Pinyin[] pinyin) {
        this.ch = ch;
        this.pinyin = pinyin;
    }

    @Override
    public IndexSet match(String str, int start, boolean partial) {
        IndexSet ret = (str.charAt(start) == ch ? IndexSet.ONE : IndexSet.NONE).copy();
        for (Element p : pinyin) ret.merge(p.match(str, start, partial));
        return ret;
    }

    public char get() {
        return ch;
    }

    public Pinyin[] pinyins() {
        return pinyin;
    }

    public static class Dummy extends Char {
        public Dummy() {
            super('\0', NONE);
        }

        public void set(char ch) {
            this.ch = ch;
        }
    }
}