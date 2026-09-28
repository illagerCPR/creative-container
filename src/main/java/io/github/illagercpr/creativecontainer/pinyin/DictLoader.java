/*
 * PinIn 1.6.0 - vendored copy.
 * Copyright (c) 2019 Juntong Liu, released under the MIT license (see LICENSE-PinIn.txt).
 * Original project: https://github.com/Towdium/PinIn
 * Vendored into Creative Container on 2026-09-28 as PinIn 1.6.0; only the package was renamed
 * (me.towdium.pinin -> io.github.illagercpr.creativecontainer.pinyin), the code is untouched.
 */

package io.github.illagercpr.creativecontainer.pinyin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface DictLoader {
    void load(BiConsumer<Character, String[]> feed);

    class Default implements DictLoader {
        @Override
        public void load(BiConsumer<Character, String[]> feed) {
            InputStream is = PinIn.class.getResourceAsStream("data.txt");
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    char ch = line.charAt(0);
                    String[] records = line.substring(3).split(", ");
                    feed.accept(ch, records);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
