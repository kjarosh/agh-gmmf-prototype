package com.github.kjarosh.agh.pp.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author Kamil Jarosz
 */
public class Assert {
    public static void assertTrue(boolean a) {
        if (!a) {
            failTest("Expected " + a + " to be true");
        } else {
            passTest();
        }
    }

    public static void assertFalse(boolean a) {
        if (a) {
            failTest("Expected " + a + " to be false");
        } else {
            passTest();
        }
    }

    public static void assertEqual(Object a, Object b) {
        if (!Objects.equals(a, b)) {
            failTest("Expected " + a + " to equal " + b);
        } else {
            passTest();
        }
    }

    public static <X> void assertEqualSet(Collection<X> a, Collection<X> b) {
        HashSet<X> setA = new HashSet<>(a);
        HashSet<X> setB = new HashSet<>(b);
        if (!Objects.equals(setA, setB)) {
            failTest("Expected set " + a + " to equal " + b);
        } else {
            passTest();
        }
    }

    private static void passTest() {
        String src = new Throwable().getStackTrace()[2].toString();
        System.out.println("Passed (" + src + ")");
    }

    private static void failTest(String message) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!");
        System.out.println("! Failed: " + message);
        System.out.println("!!!!!!!!!!!!!!!!!!!!");
        new Throwable().printStackTrace(System.out);
    }
}
