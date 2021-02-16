package com.github.kjarosh.agh.pp.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author Kamil Jarosz
 */
public class RandomUtils {

    public static <X> X randomElement(Random random, Collection<? extends X> collection) {
        if (collection instanceof List) {
            return randomElementList(random, (List<? extends X>) collection);
        } else {
            return randomElementCollection(random, collection);
        }
    }

    public static <X> X randomElementList(Random random, List<? extends X> list) {
        return list.get(random.nextInt(list.size()));
    }

    public static <X> X randomElementCollection(Random random, Collection<? extends X> collection) {
        int ix = random.nextInt(collection.size());
        Iterator<? extends X> it = collection.iterator();
        while (ix-- > 0) {
            it.next();
        }
        return it.next();
    }
}
