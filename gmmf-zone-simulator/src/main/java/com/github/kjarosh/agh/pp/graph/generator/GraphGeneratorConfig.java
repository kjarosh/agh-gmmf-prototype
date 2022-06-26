package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GraphGeneratorConfig {
    private int zones;

    private int providers;
    private int spaces;
    private DistributionConfig providersPerSpace;

    private DistributionConfig treeDepth;
    private DistributionConfig usersPerGroup;
    private DistributionConfig groupsPerGroup;

    private double differentGroupZoneProb;
    private double differentUserZoneProb;
    private double existingUserProb;
    private double existingGroupProb;

    public static abstract class DistributionConfig {
        private static final Pattern pattern = Pattern.compile("(?<fn>[a-z]+)\\((?<arg>[^,]+(,[^,]+)*)\\)");

        protected final Random random = new Random();

        @JsonCreator
        public static DistributionConfig fromString(String repr) {
            Matcher matcher = pattern.matcher(repr);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Wrong pattern");
            }

            String func = matcher.group("fn");
            String[] args = matcher.group("arg").split(",");

            switch (func) {
                case "normal":
                    return new NormalDistributionConfig(
                            Double.parseDouble(args[0]),
                            Double.parseDouble(args[1]));
                case "lnormal":
                    return new LogNormalDistributionConfig(
                            Double.parseDouble(args[0]),
                            Double.parseDouble(args[1]));
                case "enormal":
                    return new ExpNormalDistributionConfig(
                            Double.parseDouble(args[0]),
                            Double.parseDouble(args[1]));
                case "uniform":
                    return new UniformDistributionConfig(
                            Double.parseDouble(args[0]),
                            Double.parseDouble(args[1]));
                case "constant":
                    return new ConstantDistributionConfig(
                            Double.parseDouble(args[0]));
            }

            throw new IllegalArgumentException("Unknown distribution: " + func);
        }

        public abstract double avg();

        public abstract double next();

        public int nextInt() {
            return Math.toIntExact(Math.round(next()));
        }

        @Override
        @JsonValue
        public abstract String toString();
    }

    public static class NormalDistributionConfig extends DistributionConfig {
        private final double avg;
        private final double std;

        public NormalDistributionConfig(double avg, double std) {
            this.avg = avg;
            this.std = std;
        }

        @Override
        public double avg() {
            return avg;
        }

        @Override
        public double next() {
            return random.nextGaussian() * std + avg;
        }

        @Override
        public String toString() {
            return "normal(" + avg + "," + std + ")";
        }
    }

    public static class LogNormalDistributionConfig extends DistributionConfig {
        private final double avg;
        private final double std;

        public LogNormalDistributionConfig(double avg, double std) {
            this.avg = avg;
            this.std = std;
        }

        @Override
        public double avg() {
            return avg;
        }

        @Override
        public double next() {
            return Math.pow(Math.E, random.nextGaussian() * Math.log(std) + Math.log(avg));
        }

        @Override
        public String toString() {
            return "lnormal(" + avg + "," + std + ")";
        }
    }

    public static class ExpNormalDistributionConfig extends DistributionConfig {
        private final double avg;
        private final double std;

        public ExpNormalDistributionConfig(double avg, double std) {
            this.avg = avg;
            this.std = std;
        }

        @Override
        public double avg() {
            return avg;
        }

        @Override
        public double next() {
            return Math.log(random.nextGaussian() * Math.exp(std) + Math.exp(avg));
        }

        @Override
        public String toString() {
            return "enormal(" + avg + "," + std + ")";
        }
    }

    public static class UniformDistributionConfig extends DistributionConfig {
        private final double min;
        private final double max;

        public UniformDistributionConfig(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public double avg() {
            return (min + max) / 2;
        }

        @Override
        public double next() {
            return random.nextDouble() * (max - min) + min;
        }

        @Override
        public String toString() {
            return "uniform(" + min + "," + max + ")";
        }
    }

    public static class ConstantDistributionConfig extends DistributionConfig {
        private final double value;

        public ConstantDistributionConfig(double value) {
            this.value = value;
        }

        @Override
        public double avg() {
            return value;
        }

        @Override
        public double next() {
            return value;
        }

        @Override
        public String toString() {
            return "constant(" + value + ")";
        }
    }
}
