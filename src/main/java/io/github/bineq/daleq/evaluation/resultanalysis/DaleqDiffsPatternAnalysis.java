package io.github.bineq.daleq.evaluation.resultanalysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.bineq.daleq.evaluation.RunComparativeEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Algorithm / patterns to analyse generated daleq diff files.
 * @author jens dietrich
 */
public class DaleqDiffsPatternAnalysis {

    private final static Logger LOG = LoggerFactory.getLogger(DaleqDiffsPatternAnalysis.class);
    final static String DIFF_FILE_NAME = "daleq-diff.txt";

    private static final Function<String,String> GetLastToken = s -> {
        String[] tokens = s.split("\t");
        return tokens[tokens.length-1];
    };

    private static final BiFunction<String,Integer,String> GetTokenAt = (s,i) -> {
        String[] tokens = s.split("\t");
        return tokens[i];
    };

    private static final Function<String,String> RemoveDiffChar = s -> {
        char c = s.charAt(0) ;
        assert c=='-' || c=='+';
        return s.substring(1);
    };

    static void analyseDiff (List<Path> roots, Predicate<RunComparativeEvaluation.ComparativeEvaluationResultRecord> filter, boolean printDetails) throws Exception {

        AtomicInteger DIFF_COUNTER = new AtomicInteger(0);
        AtomicInteger REMOVED_CHECKCAST = new AtomicInteger(0);
        AtomicInteger CHANGED_CONSTANT = new AtomicInteger(0);
        AtomicInteger STRINGBUILDER_INITIALISATION = new AtomicInteger(0);
        AtomicInteger DEFINITION_OF_SYNTHETIC_METHODS = new AtomicInteger(0);
        AtomicInteger DEFINITION_OF_SYNTHETIC_FIELDS = new AtomicInteger(0);
        AtomicInteger DEFINITION_OF_ANNOTATIONS = new AtomicInteger(0);
        AtomicInteger MISSING_METHOD_SIGNATURE = new AtomicInteger(0);
        AtomicInteger ACCESS_CHANGED = new AtomicInteger(0);
        AtomicInteger NO_KNOWN_CAUSE = new AtomicInteger(0);
        AtomicInteger VARIOUS_CAUSES = new AtomicInteger(0);
        AtomicInteger DIFF_MISSING = new AtomicInteger(0);

        for (Path root : roots) {

            Path summary = root.resolve("summary.csv");
            Preconditions.checkState(Files.exists(summary));

            List<RunComparativeEvaluation.ComparativeEvaluationResultRecord> records =
                Files.readAllLines(summary)
                    .stream()
                    .skip(1)
                    .map(line -> RunComparativeEvaluation.ComparativeEvaluationResultRecord.parse(line))
                    .filter(filter)
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
                    try {
                        DIFF_COUNTER.incrementAndGet();
                        List<String> lines = Files.readAllLines(daleqDiff);
                        int causeCounter = 0;
                        if (isRemovedOrAddedCheckcast(lines)) {
                            REMOVED_CHECKCAST.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"removed checkcast");
                        }
                        if (isChangedConstantLoad(lines)) {
                            CHANGED_CONSTANT.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"changed constant");
                        }
                        if (isChangedStringBuilderInitialisation(lines)) {
                            STRINGBUILDER_INITIALISATION.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"StringBuilder initialisation");
                        }
                        if (isMissingSignature(lines)) {
                            MISSING_METHOD_SIGNATURE.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"missing method signature");
                        }
                        if (isDefintionOfSyntheticMethods(lines)) {
                            DEFINITION_OF_SYNTHETIC_METHODS.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"synthetic methods renamed");
                        }
                        if (isDefinitionOfSyntheticFields(lines)) {
                            DEFINITION_OF_SYNTHETIC_FIELDS.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"synthetic fields renamed");
                        }
                        if (isDefinitionOfAnnotations(lines)) {
                            DEFINITION_OF_ANNOTATIONS.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"annotations changed");
                        }
                        if (isAccessChanged(lines)) {
                            ACCESS_CHANGED.incrementAndGet();
                            causeCounter=causeCounter+1;
                            printDetails(printDetails,record,daleqDiff,"access changed");
                        }
                        if (causeCounter==0) {
                            NO_KNOWN_CAUSE.incrementAndGet();
                            printDetails(printDetails,record,daleqDiff,"unknown cause");
                        }
                        if (causeCounter>1) {
                            VARIOUS_CAUSES.incrementAndGet();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    DIFF_MISSING.incrementAndGet();
                }
            }
        }

        LOG.info("diff files analysed: {}", stringify(DIFF_COUNTER,DIFF_COUNTER));
        LOG.info("checkcast removed/added: {}", stringify(REMOVED_CHECKCAST,DIFF_COUNTER));
        LOG.info("changed constant: {}", stringify(CHANGED_CONSTANT,DIFF_COUNTER));
        LOG.info("stringbuilder initialisation: {}", stringify(STRINGBUILDER_INITIALISATION,DIFF_COUNTER));
        LOG.info("definition of synthetic methods: {}", stringify(DEFINITION_OF_SYNTHETIC_METHODS,DIFF_COUNTER));
        LOG.info("definition of synthetic fields: {}", stringify(DEFINITION_OF_SYNTHETIC_FIELDS,DIFF_COUNTER));
        LOG.info("definition of annotations: {}", stringify(DEFINITION_OF_ANNOTATIONS,DIFF_COUNTER));
        LOG.info("missing method signatures: {}", stringify(MISSING_METHOD_SIGNATURE,DIFF_COUNTER));
        LOG.info("access changed: {}", stringify(ACCESS_CHANGED,DIFF_COUNTER));

        LOG.info("more than one cause: {}", stringify(VARIOUS_CAUSES,DIFF_COUNTER));

    }

    private static void printDetails(boolean printDetails, RunComparativeEvaluation.ComparativeEvaluationResultRecord record, Path daleqDiff, String cause) {
        if (printDetails) {
            System.out.println("daleq diff classified: " + daleqDiff);
            System.out.println("\tcause: " + cause);
            System.out.println("\tproviders: " + record.provider1() + " vs " + record.provider2());
            System.out.println("\tgav: " + record.gav());
            System.out.println("\tclass: " + record.clazz());
        }
    }

    private static String stringify(AtomicInteger counter, AtomicInteger total) {
        NumberFormat percentFormatter = NumberFormat.getPercentInstance();
        percentFormatter.setMinimumFractionDigits(2);
        percentFormatter.setMaximumFractionDigits(2);
        double rel = ((double)counter.get()) / ((double) total.get());
        return "" + counter.get() + " (" + percentFormatter.format(rel) +")";
    }

    private static boolean isRemovedOrAddedCheckcast(List<String> lines) {
        Set<String> addedTypesChecked =lines.stream().filter(line -> line.startsWith("+IDB_CHECKCAST")).map(GetLastToken).collect(Collectors.toSet());
        Set<String> removedTypesChecked =lines.stream().filter(line -> line.startsWith("-IDB_CHECKCAST")).map(GetLastToken).collect(Collectors.toSet());
        return !Sets.symmetricDifference(addedTypesChecked, removedTypesChecked).isEmpty();
    }

    // compare the constant values being loaded
    private static boolean isChangedConstantLoad(List<String> lines) {
        Set<String> addedConstants =lines.stream().filter(line -> line.startsWith("+IDB_LDC")).map(GetLastToken).collect(Collectors.toSet());
        Set<String> removedConstants =lines.stream().filter(line -> line.startsWith("-IDB_LDC")).map(GetLastToken).collect(Collectors.toSet());
        return !Sets.symmetricDifference(addedConstants, removedConstants).isEmpty();
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


    private static boolean isDefinitionOfAnnotations(List<String> lines) {
        Set<String> addedAnnotations = lines.stream().filter(line -> line.startsWith("+IDB_ANNOTATION")).map(RemoveDiffChar).collect(Collectors.toSet());
        Set<String> removedAnnotations =lines.stream().filter(line -> line.startsWith("-IDB_ANNOTATION")).map(RemoveDiffChar).collect(Collectors.toSet());
        return !Sets.symmetricDifference(addedAnnotations, removedAnnotations).isEmpty();
    }

    private static boolean isDefintionOfSyntheticMethods(List<String> lines) {
        Set<String> synthMethodsAdded =lines.stream()
            .filter(line -> line.startsWith("+IDB_METHOD"))
            .filter(line -> line.contains("$"))
            .map(line -> GetTokenAt.apply(line,2))
            .collect(Collectors.toSet());
        Set<String> synthMethodsRemoved =lines.stream()
            .filter(line -> line.startsWith("-IDB_METHOD"))
            .filter(line -> line.contains("$"))
            .map(line -> GetTokenAt.apply(line,2))
            .collect(Collectors.toSet());
        return !Sets.symmetricDifference(synthMethodsAdded, synthMethodsRemoved).isEmpty();
    }

    private static boolean isAccessChanged(List<String> lines) {
        Set<String> addedAccess = lines.stream().filter(line -> line.startsWith("+IDB_ACCESS")).map(GetLastToken).collect(Collectors.toSet());
        Set<String> removedAccess =lines.stream().filter(line -> line.startsWith("-IDB_ACCESS")).map(GetLastToken).collect(Collectors.toSet());
        return !Sets.symmetricDifference(addedAccess, removedAccess).isEmpty();
    }

    private static boolean isDefinitionOfSyntheticFields(List<String> lines) {
        Set<String> synthFieldsAdded =lines.stream()
            .filter(line -> line.startsWith("+IDB_FIELD"))
            .filter(line -> line.contains("$"))
            .map(line -> GetTokenAt.apply(line,2))
            .collect(Collectors.toSet());
        Set<String> synthFieldsRemoved =lines.stream()
            .filter(line -> line.startsWith("-IDB_FIELD"))
            .filter(line -> line.contains("$"))
            .map(line -> GetTokenAt.apply(line,2))
            .collect(Collectors.toSet());
        return !Sets.symmetricDifference(synthFieldsAdded, synthFieldsRemoved).isEmpty();

    }

    private static boolean isMissingSignature(List<String> lines) {
        Set<String> addedNullSignatures = lines.stream().filter(line -> line.startsWith("+IDB_METHOD_SIGNATURE")).map(GetLastToken).filter(s -> s.equals("null")).collect(Collectors.toSet());
        Set<String> removedNullSignatures =lines.stream().filter(line -> line.startsWith("-IDB_METHOD_SIGNATURE")).map(GetLastToken).filter(s -> s.equals("null")).collect(Collectors.toSet());
        return !Sets.symmetricDifference(addedNullSignatures, removedNullSignatures).isEmpty();
    }

}
