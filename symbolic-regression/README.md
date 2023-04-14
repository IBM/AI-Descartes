# Symbolic Regression for Scientific Discovery 

This repository contains the code to run the experiments in the paper [AI Descartes: Combining Data and Theory for Derivable Scientific Discovery](https://arxiv.org/abs/2109.01634).

***
## Requirements

This code requires:
* Java. It has been tested on Linux with
Java openjdk version `11.0.15`.
* The commercial solver [BARON](https://minlp.com/baron-solver). It has been tested with BARON version `17.4.1`.

***
## Setup
Download and install BARON.
BARON's webpage is [here](https://minlp.com/baron-solver).

After installing BARON, edit the file `opts-common.yaml` in the directory `run_emf`
and specify the paths `path_to_baron_executable`
and `path_to_baron_license_file` in the lines:

* `baron_exec: path_to_baron_executable/baron`
* `LicName: path_to_baron_license_file/baronlice.txt`

If not available yet, download and install Java.

***

## How to run the code

To run the code, first move to the directory `run_emf`.
That directory contains:
* Some datasets inside the directory `datasets`.
* The executable `runnableemf.jar`.
* The script `emf.sh`
* Several parameter files `*.yaml`.

To run the code, type:

```
./emf.sh path_to_dataset yaml_file_name > output_file.log
```

For example:

```
./emf.sh datasets/kepler/solar opts-kepler.yaml > kepler_solar.log
```
```
./emf.sh datasets/langmuir/table_IX opts-langmuir.yaml > langmuir_table_IX.log
```
```
./emf.sh datasets/relativistic_time_dilation opts-relativistic_time_dilation.yaml > relativistic_time_dilation.log
```


***

## How to modify the code and compile

The Java code is inside the directory `emf`.

One way to modify the code and create a new executable,
is to use Eclipse.
Eclipse can be downloaded [here](https://www.eclipse.org/).

The steps to modify the code and create a new executable in Eclipse are:
1. Install Eclipse.
2. Import the project emf into Eclipse.
3. Modify the code.
4. Create a new runnable JAR file in Eclipse by doing:
   * From the menu bar's File menu, select Export.
   * Expand the Java node and select Runnable JAR file. Click Next.
   * In the Runnable JAR File Specification page, select a `Java Application` launch configuration to use to create a runnable JAR.
   * In the Export destination field, either type or click Browse to select a location for the JAR file.
   * Select an appropriate library handling strategy.

