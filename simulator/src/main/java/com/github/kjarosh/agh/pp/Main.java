package com.github.kjarosh.agh.pp;

import com.github.kjarosh.agh.pp.cli.Cmd;
import com.github.kjarosh.agh.pp.test.Tester;

/**
 * @author Kamil Jarosz
 */
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            Cmd.main(args);
            return;
        }

        switch (args[0]) {
            case "server":
                SpringApp.main(args);
                break;
            case "test":
                Tester.main(args);
                break;
            default:
                Cmd.main(args);
                return;
        }
    }
}
