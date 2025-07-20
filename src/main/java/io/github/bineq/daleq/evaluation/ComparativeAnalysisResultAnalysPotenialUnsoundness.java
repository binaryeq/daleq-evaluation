package io.github.bineq.daleq.evaluation;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Analyse records for unsoundness in javap and jnorm
 * i.e. records where they computed equality, but daleq did not.
 * @author jens dietrich
 */
public class ComparativeAnalysisResultAnalysPotenialUnsoundness {


    public static void main(String[] args) throws IOException {
        Preconditions.checkArgument(args.length > 0, "one argument required, csv result summary");
        Path summary = Path.of(args[0]);
        Preconditions.checkState(Files.exists(summary), "report folder does not exist: " + summary);
        Preconditions.checkState(!Files.isDirectory(summary), "report folder is  a directory: " + summary);

        System.out.println("Classes that are javap-equivalent but not daleq equivalent");
        long count = Files.readAllLines(summary).stream()
            .skip(1) // header
            .map(line -> line.split("\t"))
            .filter(values -> values[4].equals("EQUIVALENT") && values[6].equals("NON_EQUIVALENT"))
            .map(values -> {
                System.out.println(values[0] + " (" + values[1] + "," + values[2] + ") -- " + values[3]);
                return values;
            })
            .count();

        System.out.println("total count: " + count);

        System.out.println();
        System.out.println("Classes that are jnorm-equivalent but not daleq equivalent");
        count = Files.readAllLines(summary).stream()
            .skip(1) // header
            .map(line -> line.split("\t"))
            .filter(values -> values[5].equals("EQUIVALENT") && values[6].equals("NON_EQUIVALENT"))
            .map(values -> {
                System.out.println(values[0] + " (" + values[1] + "," + values[2] + ") -- " + values[3]);
                return values;
            })
            .count();

        System.out.println("total count: " + count);
    }
}
