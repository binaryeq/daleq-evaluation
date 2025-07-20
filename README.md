# DALEQ Evaluation Experiments and Examples

## Downloading The Alternative Build Dataset

Download the dataset from [https://zenodo.org/records/14915249](https://zenodo.org/records/14915249). 

## Building 

1. Clone DALEQ from [https://github.com/binaryeq/daleq](https://github.com/binaryeq/daleq).
2. Check the version of DALEQ used in `pom.xml`, and check out the tag corresponding to this. 
3. Locally install DALEQ by running `mvn clean install`.

## Prerequisites

### Download and Install JNorm

Download the *JNorm* from [https://github.com/stschott/jnorm-tool/releases/download/v1.0.0/jnorm-cli-1.0.0.jar](https://github.com/stschott/jnorm-tool/releases/download/v1.0.0/jnorm-cli-1.0.0.jar) and copy it into `tools/`.

### Download and Install Souffle

Follow these [instructions](https://souffle-lang.github.io/install) to download and install souffle.

## TODO

Set up Java 17 to succesfully run *JNorm*.

# Running Evaluation Experiments

1. compute the classpath using `dependency:build-classpath`
2. run `java -cp <classpath> -ea -DSOUFFLE=<souffle-home> <output-folder> <input1>.tsv <input2>.tsv`

The input files are the *.tsv files from the alternative build dataset,
such as `gav_gaoss.tsv` (for jars from Google GAOSS), `gav_mvnc.tsv` (for jars from Maven Central) and
`gav_obfs.tsv` (for jars from Oracle Build-From-Source)

The expected runtimes (depending on hardware being used) are 3-6 hours for comparing mvnc with obfs, and 2-4 days form comparing mvnc with gaoss. 



