package com.github.kjarosh.agh.pp;

import com.github.kjarosh.agh.pp.cli.Cmd;
import com.github.kjarosh.agh.pp.test.Tester;

/**
 * @author Kamil Jarosz
 */
public class Main {
    public static void main(String[] args) {
        if (System.getenv("TESTED_ZONE") != null) {
            Tester.main(System.getenv("TESTED_ZONE"));
            return;
        }

        if (System.getenv("CLIENT_MODE") != null) {
            Cmd.main(args);
            return;
        }

        SpringApp.main(args);
    }
}
