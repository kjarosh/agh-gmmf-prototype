package com.github.kjarosh.agh.pp.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kamil Jarosz
 */
public class Assert {
    public static final Stats statistics = new Stats();
    public static boolean showPassed = true;

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
        assertEqual(a, b, null);
    }

    public static void assertEqual(Object a, Object b, String when) {
        if (!Objects.equals(a, b)) {
            failTest("Expected " + a + " to equal " + b +
                    (when != null ? " when " + when : ""));
        } else {
            passTest();
        }
    }

    public static <X> void assertEqualSet(Collection<X> a, Collection<X> b) {
        assertEqualSet(a, b, null);
    }

    public static <X> void assertEqualSet(Collection<X> a, Collection<X> b, String when) {
        HashSet<X> setA = new HashSet<>(a);
        HashSet<X> setB = new HashSet<>(b);
        if (!Objects.equals(setA, setB)) {
            failTest("Expected set " + a + " to equal " + b +
                    (when != null ? " when " + when : ""));
        } else {
            passTest();
        }
    }

    private static void passTest() {
        statistics.passed.incrementAndGet();
        if (showPassed) {
            String src = new Throwable().getStackTrace()[2].toString();
            System.out.println("Passed (" + src + ")");
        }
    }

    private static void failTest(String message) {
        statistics.failed.incrementAndGet();
        System.out.println("!!!!!!!!!!!!!!!!!!!!");
        System.out.println("! Failed: " + message);
        System.out.println("!!!!!!!!!!!!!!!!!!!!");
        new Throwable().printStackTrace(System.out);
    }

    public static class Stats {
        private final AtomicInteger failed;
        private final AtomicInteger passed;

        public Stats() {
            this(0, 0);
        }

        public Stats(int failed, int passed) {
            this.failed = new AtomicInteger(failed);
            this.passed = new AtomicInteger(passed);
        }

        public Stats reset() {
            Stats oldStats = new Stats(failed.get(), passed.get());

            failed.set(0);
            passed.set(0);

            return oldStats;
        }

        public Stats reduce(Stats other) {
            return new Stats(failed() + other.failed(), passed() + other.passed());
        }

        public int failed() {
            return failed.get();
        }

        public int passed() {
            return passed.get();
        }

        @Override
        public String toString() {
            return failed.get() + "/" + (failed.get() + passed.get()) + " failed";
        }
    }
}
