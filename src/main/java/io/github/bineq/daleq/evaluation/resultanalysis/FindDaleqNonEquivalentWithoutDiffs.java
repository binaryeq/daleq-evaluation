package io.github.bineq.daleq.evaluation.resultanalysis;

import com.google.common.base.Preconditions;
import io.github.bineq.daleq.evaluation.ComparisonResult;
import io.github.bineq.daleq.evaluation.RunComparativeEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.bineq.daleq.evaluation.resultanalysis.AnalyseDaleqDiffs.DIFF_FILE_NAME;

/**
 * Find cases where classes are not daleq equivalent, but no diff file has been generated.
 * @author jens dietrich
 */
public class FindDaleqNonEquivalentWithoutDiffs {

    private final static Logger LOG = LoggerFactory.getLogger(FindDaleqNonEquivalentWithoutDiffs.class);

    public static void main (String[] args) throws Exception {

        AtomicInteger counterDiffFilesDoExit = new AtomicInteger(0);
        AtomicInteger counterDiffFilesDontExit = new AtomicInteger(0);

        Preconditions.checkArgument(args.length>0);
        List<Path> roots = Stream.of(args)
            .map(n -> Path.of(n))
            .map(p -> {
                Preconditions.checkState(Files.exists(p));
                Preconditions.checkState(Files.isDirectory(p));
                return p;
            })
            .collect(Collectors.toUnmodifiableList());

        for (Path root : roots) {

            Path summary = root.resolve("summary.csv");
            Preconditions.checkState(Files.exists(summary));

            List<RunComparativeEvaluation.ComparativeEvaluationResultRecord> records =
                Files.readAllLines(summary)
                    .stream()
                    .skip(1)
                    .map(line -> RunComparativeEvaluation.ComparativeEvaluationResultRecord.parse(line))
                    .filter(record -> record.result4daleq()== ComparisonResult.NON_EQUIVALENT)
                    .collect(Collectors.toUnmodifiableList());

            LOG.info("{} daleq non-equivalent records imported from {}", records.size(), summary);

            for (RunComparativeEvaluation.ComparativeEvaluationResultRecord record: records) {
                // construct folder
                Path dir = root.resolve(record.gav());
                String clazzDirName = record.clazz()
                    .replace('/','.')
                    .replace(".class","")
                    .replace("$","_____")
                    ;
                dir = dir.resolve(clazzDirName);
                dir = dir.resolve("daleq");
                if (!Files.exists(dir)) {
                    LOG.warn("folder not found: {}", dir);
                }

                Path daleqDiff = dir.resolve(DIFF_FILE_NAME);
                if (Files.exists(daleqDiff)) {
                    counterDiffFilesDoExit.incrementAndGet();
                }
                else {
                    counterDiffFilesDontExit.incrementAndGet();
                }
            }
        }

        LOG.info("Daleq non-equivalent without diffs: {}", counterDiffFilesDontExit.get());
        LOG.info("Daleq non-equivalent with diffs: {}", counterDiffFilesDoExit.get());
    }
}
