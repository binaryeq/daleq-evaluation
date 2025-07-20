package io.github.bineq.daleq.evaluation;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.bineq.daleq.evaluation.RunEvaluation.getProviderName;
import static io.github.bineq.daleq.evaluation.RunEvaluation.parseRecords;

public class CopyClasses4Analysis {

    final static Logger LOG = LoggerFactory.getLogger(CopyClasses4Analysis.class);

    public static void main (String[] args) throws IOException {

        String gav = args[0];
        String className = args[1];
        LOG.info("GAV: {}", gav);
        LOG.info("Class name: {}", className);
        Path destDir = Path.of(args[2]);
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }
        LOG.info("Copying destination folder: {}", destDir);

        List<Path> datasets = Stream.of(args).skip(3)
            .map(arg -> {
                Path path = Path.of(arg);
                Preconditions.checkArgument(Files.exists(path));
                Preconditions.checkArgument(path.toString().endsWith(".tsv"));
                return path;
            })
            .collect(Collectors.toUnmodifiableList());


        List<String> providers = datasets.stream()
            .map(f -> getProviderName(f))
            .collect(Collectors.toUnmodifiableList());

        List<Set<Record>> records = datasets.stream()
            .map(f -> {
                try {
                    LOG.info("Parsing records from " + f);
                    Set<Record> records2 = parseRecords(f);
                    LOG.info("\t" + records2.size() + " parsed");
                    return records2;
                } catch (IOException e) {
                    LOG.error("Error parsing " + f);
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toUnmodifiableList());

        for (int i=0;i<records.size();i++) {
            String provider = providers.get(i);
            Path dir = destDir.resolve(provider);
            Files.createDirectories(dir);
            Record record = records.get(i).stream()
                .filter(r -> r.gav().equals(gav))
                .findFirst().get();
            assert record != null;
            Path binJar = record.binMainFile();
            assert Files.exists(binJar);
            LOG.info("located bin jar: {}", binJar);

            try {
                String unqualifiedClassName = className.substring(className.lastIndexOf('/') + 1);
                Content content = new Content(binJar, className);
                byte[] data = content.load();
                Path classFile = dir.resolve(unqualifiedClassName + ".class");
                Files.write(classFile, data);
                LOG.info("class bin file copied to: {}", classFile);
            }
            catch (Exception e) {
                LOG.error("Error copying bytecode for " + className, e);
            }

            try {

                // for inner classes we need to locate top level class
                String outerClassName = className.contains("$")?className.substring(0, className.indexOf('$')):className;
                outerClassName = className.replace(".class",".java");
                String unqualifiedOuterClassName = outerClassName.substring(outerClassName.lastIndexOf('/') + 1);
                Path srcJar = record.srcMainFile();
                assert Files.exists(srcJar);
                LOG.info("located src jar: {}", srcJar);
                Content content = new Content(srcJar, outerClassName);
                byte[] data = content.load();
                Path srcFile = dir.resolve(unqualifiedOuterClassName + ".java");
                Files.write(srcFile, data);
                LOG.info("class src file copied to: {}", srcFile);
            }
            catch (Exception e) {
                LOG.error("Error copying sourcecode for " + className, e);
            }
        }

    }



}
