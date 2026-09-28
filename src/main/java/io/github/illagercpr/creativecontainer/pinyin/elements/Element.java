/*
 * PinIn 1.6.0 - vendored copy.
 * Copyright (c) 2019 Juntong Liu, released under the MIT license (see LICENSE-PinIn.txt).
 * Original project: https://github.com/Towdium/PinIn
 * Vendored into Creative Container on 2026-09-28 as PinIn 1.6.0; only the package was renamed
 * (me.towdium.pinin -> io.github.illagercpr.creativecontainer.pinyin), the code is untouched.
 */

package io.github.illagercpr.creativecontainer.pinyin.elements;

import io.github.illagercpr.creativecontainer.pinyin.utils.IndexSet;

/**
 * Author: Towdium
 * Date: 21/04/19
 */
public interface Element {
    IndexSet match(String str, int start, boolean partial);
}