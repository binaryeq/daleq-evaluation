package io.github.bineq.daleq.evaluation.resultanalysis;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyse the daleq diff files created when classes compared are not equivalent.
 * @author jens dietrich
 */
public class AnalyseDaleqDiffs {

    private final static Logger LOG = LoggerFactory.getLogger(AnalyseDaleqDiffs.class);
    private final static String DIFF_FILE_NAME = "daleq-diff.txt";

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

        AtomicInteger DIFF_COUNTER = new AtomicInteger(0);
        AtomicInteger REMOVED_CHECKCAST = new AtomicInteger(0);
        AtomicInteger CHANGED_CONSTANT = new AtomicInteger(0);
        AtomicInteger STRINGBUILDER_INITIALISATION = new AtomicInteger(0);
        AtomicInteger DEFINITION_OF_SYNTHETIC_METHODS = new AtomicInteger(0);
        AtomicInteger DEFINITION_OF_SYNTHETIC_FIELDS = new AtomicInteger(0);
        AtomicInteger DEFINITION_OF_ANNOTATIONS = new AtomicInteger(0);
        AtomicInteger MISSING_METHOD_SIGNATURE = new AtomicInteger(0);


        AtomicInteger NO_KNOWN_CAUSE = new AtomicInteger(0);
        AtomicInteger VARIOUS_CAUSES = new AtomicInteger(0);

        for (Path root : roots) {
            Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(DIFF_FILE_NAME))
                .forEach(f -> {
                    try {
                        DIFF_COUNTER.incrementAndGet();
                        List<String> lines = Files.readAllLines(f);
                        int causeCounter = 0;
                        if (isRemovedOrAddedCheckcast(lines)) {
                            REMOVED_CHECKCAST.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (isChangedConstant(lines)) {
                            CHANGED_CONSTANT.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (isChangedStringBuilderInitialisation(lines)) {
                            STRINGBUILDER_INITIALISATION.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (isMissingSignature(lines)) {
                            MISSING_METHOD_SIGNATURE.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (isDefintionOfSyntheticMethods(lines)) {
                            DEFINITION_OF_SYNTHETIC_METHODS.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (isDefinitionOfSyntheticFields(lines)) {
                            DEFINITION_OF_SYNTHETIC_FIELDS.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (isDefinitionOfAnnotations(lines)) {
                            DEFINITION_OF_ANNOTATIONS.incrementAndGet();
                            causeCounter=causeCounter+1;
                        }
                        if (causeCounter==0) {
                            NO_KNOWN_CAUSE.incrementAndGet();
                        }
                        if (causeCounter>1) {
                            VARIOUS_CAUSES.incrementAndGet();
                        }


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

        LOG.info("diff files analysed: {}", DIFF_COUNTER.get());
        LOG.info("checkcast removed/added: {}", REMOVED_CHECKCAST.get());
        LOG.info("changed constant: {}", CHANGED_CONSTANT.get());
        LOG.info("stringbuilder initialisation: {}", STRINGBUILDER_INITIALISATION.get());
        LOG.info("definition of synthetic methods: {}", DEFINITION_OF_SYNTHETIC_METHODS.get());
        LOG.info("definition of synthetic fields: {}", DEFINITION_OF_SYNTHETIC_FIELDS.get());
        LOG.info("definition of annotations: {}", DEFINITION_OF_ANNOTATIONS.get());
        LOG.info("missing method signatures: {}", MISSING_METHOD_SIGNATURE.get());

        LOG.info("more than one cause: {}", VARIOUS_CAUSES.get());
        LOG.info("no known causes: {}", NO_KNOWN_CAUSE.get());

    }

    private static boolean isRemovedOrAddedCheckcast(List<String> lines) {
        return lines.stream().anyMatch(line -> line.startsWith("+IDB_CHECKCAST") || line.startsWith("-IDB_CHECKCAST"));
    }

    private static boolean isChangedConstant(List<String> lines) {
        return lines.stream().anyMatch(line -> line.startsWith("-IDB_LDC"))
            && lines.stream().anyMatch(line -> line.startsWith("+IDB_LDC"));
    }

    private static boolean isChangedStringBuilderInitialisation(List<String> lines) {
        // java/lang/StringBuilder <init>  (I)V
        return (lines.stream().anyMatch(line -> line.startsWith("+") && line.contains("java/lang/StringBuilder") && line.contains("<init>") && line.contains("(I)V"))
            && lines.stream().anyMatch(line -> line.startsWith("-") && line.contains("java/lang/StringBuilder") && line.contains("<init>") && line.contains("()V")))

            ||
            (lines.stream().anyMatch(line -> line.startsWith("-") && line.contains("java/lang/StringBuilder") && line.contains("<init>") && line.contains("(I)V"))
                && lines.stream().anyMatch(line -> line.startsWith("+") && line.contains("java/lang/StringBuilder") && line.contains("<init>") && line.contains("()V")))
            ;
    }

    private static boolean isNamingOfLambdas(List<String> lines) {
        // lambda$null
        return (lines.stream().anyMatch(line -> line.startsWith("+IDB_METHOD") && line.contains("lambda$")))
            || (lines.stream().anyMatch(line -> line.startsWith("-IDB_METHOD") && line.contains("lambda$")))
            ;
    }

    private static boolean isDefinitionOfAnnotations(List<String> lines) {
        // lambda$null
        return (lines.stream().anyMatch(line -> line.startsWith("-IDB_ANNOTATION") && line.contains("$")))
            && (lines.stream().anyMatch(line -> line.startsWith("+IDB_ANNOTATION") && line.contains("$")))
            ;
    }

    private static boolean isDefintionOfSyntheticMethods(List<String> lines) {
        // lambda$null
        return (lines.stream().anyMatch(line -> line.startsWith("+IDB_METHOD") && line.contains("$")))
            && (lines.stream().anyMatch(line -> line.startsWith("-IDB_METHOD") && line.contains("$")))
            ;
    }

    private static boolean isDefinitionOfSyntheticFields(List<String> lines) {
        // lambda$null
        return (lines.stream().anyMatch(line -> line.startsWith("+IDB_FIELD") && line.contains("$")))
            && (lines.stream().anyMatch(line -> line.startsWith("-IDB_FIELD") && line.contains("$")))
            ;
    }

    private static boolean isMissingSignature(List<String> lines) {
        return (lines.stream().anyMatch(line -> line.startsWith("+IDB_METHOD_SIGNATURE") && line.endsWith("null")))
            || (lines.stream().anyMatch(line -> line.startsWith("-IDB_METHOD_SIGNATURE") && line.endsWith("null")))
            ;
    }
}
