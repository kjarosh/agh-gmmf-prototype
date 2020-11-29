package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.shell.jline2.PicocliJLineCompleter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * @author Kamil Jarosz
 */
public class Cmd {
    private static final ZoneClient client = new ZoneClient();
    private static ZoneId zone = null;

    static {
        LogbackUtils.loadLogbackCli();
    }

    private static boolean checkZone() {
        if (zone == null) {
            System.out.println("Error: Unknown zone");
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        try {
            ConsoleReader reader = new ConsoleReader();
            CliCommands commands = new CliCommands(reader);
            CommandLine cmd = new CommandLine(commands);
            reader.addCompleter(new PicocliJLineCompleter(cmd.getCommandSpec()));

            String line;
            while ((line = reader.readLine("> ")) != null) {
                ArgumentCompleter.ArgumentList list = new ArgumentCompleter.WhitespaceArgumentDelimiter()
                        .delimit(line, line.length());
                new CommandLine(commands).execute(list.getArguments());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Command(
            description = "Simulator client",
            subcommands = {
                    UseZoneCmd.class,
                    IsAdjacentCmd.class,
                    ListAdjacentCmd.class,
                    ListAdjacentRevCmd.class,
                    ClearScreen.class,
            })
    static class CliCommands implements Runnable {
        final ConsoleReader reader;
        final PrintWriter out;

        @Spec
        private CommandSpec spec;

        CliCommands(ConsoleReader reader) {
            this.reader = reader;
            out = new PrintWriter(reader.getOutput());
        }

        public void run() {
            out.println(spec.commandLine().getUsageMessage());
        }
    }

    @Command(name = "clear",
            mixinStandardHelpOptions = true,
            description = "Clears the screen",
            version = "1.0")
    static class ClearScreen implements Callable<Void> {
        @ParentCommand
        CliCommands parent;

        public Void call() throws IOException {
            parent.reader.clearScreen();
            return null;
        }
    }

    @Command(name = "use", description = "Use the given zone when performing queries")
    static class UseZoneCmd implements Callable<Void> {
        @Option(names = {"-z", "--zone"})
        private String zone;

        public Void call() {
            Cmd.zone = new ZoneId(zone);
            return null;
        }
    }

    @Command(name = "is_adj", description = "Checks if two vertices are adjacent")
    static class IsAdjacentCmd implements Callable<Void> {
        @Option(names = {"-f", "--from"})
        private String from;

        @Option(names = {"-t", "--to"})
        private String to;

        public Void call() {
            if (checkZone()) return null;
            System.out.println(client.isAdjacent(zone,
                    EdgeId.of(new VertexId(from), new VertexId(to))));
            return null;
        }
    }

    @Command(name = "list_adj", description = "Lists adjacent vertices")
    static class ListAdjacentCmd implements Callable<Void> {
        @Option(names = {"--of"})
        private String of;

        public Void call() {
            if (checkZone()) return null;
            System.out.println(client.listAdjacent(zone, new VertexId(of)));
            return null;
        }
    }

    @Command(name = "list_adj_rev", description = "Lists adjacent vertices (reversed)")
    static class ListAdjacentRevCmd implements Callable<Void> {
        @Option(names = {"--of"})
        private String of;

        public Void call() {
            if (checkZone()) return null;
            System.out.println(client.listAdjacentReversed(zone, new VertexId(of)));
            return null;
        }
    }
}
