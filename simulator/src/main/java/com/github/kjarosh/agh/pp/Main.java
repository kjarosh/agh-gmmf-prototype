package com.github.kjarosh.agh.pp;

import com.github.kjarosh.agh.pp.cli.Cmd;
import com.github.kjarosh.agh.pp.test.Tester;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kamil Jarosz
 */
public class Main {
    public static void main(String[] args) {
        if (System.getenv("TESTED_ZONE") != null) {
            String testedZone = System.getenv("TESTED_ZONE");
            List<String> allZones = Arrays.asList(System.getenv("ALL_ZONES").split(","));
            Tester.main(testedZone, allZones);
            return;
        }

        if (System.getenv("CLIENT_MODE") != null) {
            Cmd.main(args);
            return;
        }

        SpringApp.main(args);
    }
}
