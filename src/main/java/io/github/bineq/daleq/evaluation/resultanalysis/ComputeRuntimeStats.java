package io.github.bineq.daleq.evaluation.resultanalysis;

import com.google.common.base.Preconditions;
import com.google.common.math.Stats;
import org.checkerframework.checker.units.qual.A;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compute stats about the runtime of building the EDBs/IDBs.
 * Relies on the timestamp files generated in the result set.
 * @author jens dietrich
 */
public class ComputeRuntimeStats {

    public static final String TIMESTAMP_FILENAME = "computation-time-in-ms.txt";

    public static void main (String[] args) throws Exception {

        Preconditions.checkArgument(args.length>0);
        List<Path> roots = Stream.of(args)
            .map(n -> Path.of(n))
            .map(p -> {
                Preconditions.checkState(Files.exists(p));
                Preconditions.checkState(Files.isDirectory(p));
                return p;
            })
            .collect(Collectors.toUnmodifiableList());

        List<Path> timestampFiles = new ArrayList<>();
        for (Path root : roots) {
            List<Path> timestampFiles2 = Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().equals(TIMESTAMP_FILENAME))
                .collect(Collectors.toList());
            timestampFiles.addAll(timestampFiles2);
        }

        System.out.println(""+timestampFiles.size()+ " found");
        List<Integer> timestamps =  new ArrayList<>(timestampFiles.size());
        for (Path timestampFile : timestampFiles) {
            List<String> lines = Files.readAllLines(timestampFile);
            int timestamp = Integer.parseInt(lines.get(0));
            if (timestamp>10_000) {
                System.out.println("big timestamp " + timestamp + " for " + timestampFile);
            }
            timestamps.add(timestamp);
        }

        Stats stats = Stats.of(timestamps);

        System.out.println("timestamps analysed: " + stats.count());
        System.out.println("mean: " + stats.mean());
        System.out.println("max: " + stats.max());
        System.out.println("min: " + stats.min());
        System.out.println("stddev: " + stats.populationStandardDeviation());
    }
}