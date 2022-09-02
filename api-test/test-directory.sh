if (( $# != 1 ))
then
  print "USAGE: $0 <directory>"
  exit
fi

CYAN="\e[36m"
ENDCOLOR="\e[0m"

find $1 -print0 | while IFS= read -r -d '' file
do
    if [[ $file == *.json ]]
    then
      echo $(printf "${CYAN}Running test for request \"$file\"${ENDCOLOR}")
      echo ""
      ./test-study.sh $file
      echo "----------------------------------"
    fi
done