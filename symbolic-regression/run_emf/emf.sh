dir=$1

yamlfile="opts-Feynman-dims.yaml"
if [ $# -eq 2 ]
  then
    yamlfile=$2
fi
echo $yamlfile

java -jar runnableemf.jar opts-common.yaml $yamlfile $dir/ 

