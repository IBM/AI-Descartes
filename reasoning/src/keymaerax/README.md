# Add here KeYmaeraX jar

## Code from
* [KeYmaeraX website](http://www.ls.cs.cmu.edu/KeYmaeraX/)
* [Source Code](https://github.com/LS-Lab/KeYmaeraX-release) (not used)

## License (source code)

KeYmaeraX is licensed under the terms of the GNU GENERAL PUBLIC LICENSE.


## Version
Version 4.9.3

## Usage

Usage: `java -jar keymaerax.jar`

```
  -ui [web server options] |
  -prove file.kyx [-conjecture file2.kyx] [-out file.kyp] [-timeout seconds] [-verbose] |
  -modelplex file.kyx [-monitor ctrl|model] [-out file.kym] [-isar] [-sandbox] [-fallback prg] |
  -codegen file.kyx [-vars var1,var2,..,varn] [-out file.c] [-quantitative ctrl|model|plant] | 
  -striphints file.kyx -out fileout.kyx
  -setup
```

Actions:
```
  -ui        start web user interface with optional server arguments (default)
  -prove     run prover on given archive of models or proofs
  -modelplex synthesize monitor from given model by proof with ModelPlex tactic
  -codegen   generate executable C code from given model file
  -striphints remove all proof annotations from the model
  -parse     return error code 0 if the given model file parses
  -bparse    return error code 0 if given bellerophon tactic file parses
  -repl      prove given model interactively from REPL command line
  -setup     initializes the configuration and lemma cache
```

Additional options:
```
  -tool mathematica|z3 choose which tool to use for real arithmetic
  -mathkernel MathKernel(.exe) path to Mathematica kernel executable
  -jlink path/to/jlinkNativeLib path to Mathematica J/Link library directory
  -timeout  how many seconds to try proving before giving up, forever if <=0
  -monitor  ctrl|model what kind of monitor to generate with ModelPlex
  -vars     use ordered list of variables, treating others as constant functions
  -interval guard reals by interval arithmetic in floating point (recommended)
  -nointerval skip interval arithmetic presuming no floating point errors
  -savept path export proof term s-expression from -prove to given path
  -launch   use present JVM instead of launching one with a bigger stack
  -lax      use lax mode with more flexible parser, printer, prover etc.
  -strict   use strict mode with no flexibility in prover
  -debug    use debug mode with exhaustive messages
  -nodebug  disable debug mode to suppress intermediate messages
  -security use security manager imposing some runtime security restrictions
  -help     Display this usage information
  -license  Show license agreement for using this software
```
