package com.github.kjarosh.agh.pp.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Kamil Jarosz
 */
public class StringList extends ArrayList<String> {
    public StringList(int initialCapacity) {
        super(initialCapacity);
    }

    public StringList() {
    }

    public StringList(Collection<? extends String> c) {
        super(c);
    }
}
