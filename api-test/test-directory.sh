#######################################
# Run all regression tests in a directory, for each request comparing the results of the map-reduce subsetting
# with the database subsetting. Returns with a non-zero exit code if any of the results differ.
# Arguments:
#   $1 REQUEST_DIRECTORY: Directory containing request bodies. Requests in the directory must have the following directory structure: ${STUDY_ID}/${OUTPUT_ENTITY_ID}/*.json.
#   $2 OUTPUT_FILE: File to write CSV performance results to.
#######################################

if (( $# != 2 ))
then
  echo "USAGE: $(basename $0) <directory> <output_file>"
  exit
fi

if [[ -f $2 ]]
then
  echo "Output file $2 already exists"
  exit
fi

CYAN="\e[36m"
ENDCOLOR="\e[0m"
FAILED_TESTS=()

DIRECTORY=$1
OUTPUT_FILE=$2

for file in $(find "$DIRECTORY")
do
    if [[ $file == *.json ]]
    then
      echo $(printf "${CYAN}Running test for request \"$file\"${ENDCOLOR}")
      echo ""
      ./test-study.sh $file $OUTPUT_FILE
      if [[ $? -ne 0 ]]
      then
        FAILED_TESTS+=($file)
      fi
      echo "----------------------------------"
    fi
done

if [[ ${#FAILED_TESTS[@]} -eq 0 ]]
then
    echo "All tests passed!"
    exit 0
else
    echo "The following tests failed:"
    for value in "${FAILED_TESTS[@]}"
    do
      echo "Failed test: $value"
    done
    exit 1
fi
