package io.github.bineq.daleq.evaluation.resultanalysis;

import com.google.common.base.Preconditions;
import io.github.bineq.daleq.evaluation.ComparisonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.bineq.daleq.evaluation.resultanalysis.DaleqDiffsPatternAnalysis.analyseDiff;

/**
 * Analyse the daleq diff files created when classes compared are not equivalent.
 * Only consider records with javap equivalence.
 * @author jens dietrich
 */
public class AnalyseDaleqDiffs4JavapEquivalent {

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

        analyseDiff(roots,p -> p.result4javap() == ComparisonResult.EQUIVALENT,true);
    }

}
