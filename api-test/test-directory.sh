if (( $# != 1 ))
then
  print "USAGE: $0 <directory>"
  exit
fi

CYAN="\e[36m"
ENDCOLOR="\e[0m"
FAILED_TESTS=()

for file in "$1/*"
do
  echo $file
done

for file in $(find "$1")
do
    if [[ $file == *.json ]]
    then
      echo $(printf "${CYAN}Running test for request \"$file\"${ENDCOLOR}")
      echo ""
      ./test-study.sh $file
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
