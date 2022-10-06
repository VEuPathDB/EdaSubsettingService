#!/usr/bin/env bash

#######################################
# Run a regression test, comparing the results of the map-reduce subsetting with the database subsetting.
# Returns with a non-zero exit code if the results differ. Prints the CURL request times to STDOUT to compare performance.
# Arguments:
#   $1 REQUEST_FILE: File containing a request body. Must have the following directory structure: ${STUDY_ID}/${OUTPUT_ENTITY_ID}/*.json.
#   $2 OUTPUT_FILE: File to write CSV performance results to.
#######################################

if (( $# != 2 ))
then
  echo "USAGE: $(basename $0) <request_body_file> <output_file>"
  exit
fi

RED="\e[31m"
GREEN="\e[32m"
CYAN="\e[36m"
ENDCOLOR="\e[0m"

REQUEST_FILE=$1
OUTPUT_FILE=$2

# If either required environment variables are not set, check .testenv file.
if [[ -z "${BASE_URL}" || -z "${AUTH_KEY}" ]];
then
  SCRIPT_PATH=$(realpath $0)
  SCRIPT_DIR=$(dirname SCRIPT_PATH)
  echo "Sourcing env file $SCRIPT_DIR/.testenv"
  source "$SCRIPT_DIR/.testenv"
fi

# If env vars still aren't set, error out.
if [[ -z "${BASE_URL}" || -z "${AUTH_KEY}" ]];
then
  echo $(printf "${RED}ERROR: BASE_URL and AUTH_KEY must be set to execute tests.${ENDCOLOR}")
  exit 1;
fi

FILE_REQUEST_BODY=$(cat "$REQUEST_FILE" | sed 's/{{data_source}}/file/g')
DB_REQUEST_BODY=$(cat "$REQUEST_FILE" | sed 's/{{data_source}}/database/g')
REQUEST_DIR=$(dirname "$REQUEST_FILE")
OUTPUT_ENTITY_ID=$(echo "$REQUEST_DIR" | rev | cut -d"/" -f1 | rev)
STUDY_ID=$(dirname "$REQUEST_DIR" | rev | cut -d"/" -f1 | rev)

#######################################
# Executes a CURL request against the BASE_URL
# Globals:
#   BASE_URL: The base URL used to construct CURL request
#   AUTH_KEY: Auth key passed as a header to authenticate with EDA service
# Arguments:
#   $1 REQUEST_BODY: Request body of CURL request
#   $2 OUTPUT_FILE:  File to write the response of the CURL to
#######################################
curl_endpoint ()
{
  # FORMATTING='Establish Connection: %{time_connect}s\nTTFB: %{time_starttransfer}s\nTotal: %{time_total}s\n'
  FORMATTING='%{time_connect},%{time_starttransfer},%{time_total}'
  curl -o $2 -w $FORMATTING -s --location --request POST "${BASE_URL}/studies/${STUDY_ID}/entities/${OUTPUT_ENTITY_ID}/tabular" \
  --header "Auth-Key: ${AUTH_KEY}" \
  --header 'Content-Type: application/json' \
  --data "$1" \
  --insecure
  grep "\"status\":\"server-error\"" $2
  if [[ $? -eq 0 ]]
  then
    echo $(printf "${RED}FAILED: Received server error when calling using map reduce configuration.${ENDCOLOR}")
    exit 1
  fi
}

mkdir -p output
MR_CURL_RESULTS=$(curl_endpoint "$FILE_REQUEST_BODY" "output/file_out")
MR_ROWS_RETURNED=`wc -l output/file_out`
echo "MAP REDUCE: ${MR_CURL_RESULTS}"
echo ""
DB_CURL_RESULTS=$(curl_endpoint "$DB_REQUEST_BODY" "output/db_out")
DB_ROWS_RETURNED=`wc -l output/db_out`
echo "DATABASE: ${DB_CURL_RESULTS}"

NUM_FILTERS=`echo ${FILE_REQUEST_BODY} | jq '.["filters"] | length'`
NUM_OUTPUT_VARIABLES=`echo ${FILE_REQUEST_BODY} | jq '.["outputVariableIds"] | length'`

if diff output/file_out output/db_out
then
  echo -e $(printf "${GREEN}SUCCESS: No differences found in db output and file output!${ENDCOLOR}")
  echo "${STUDY_ID},${OUTPUT_ENTITY_ID},${DB_ROWS_RETURNED},${NUM_OUTPUT_VARIABLES},${NUM_FILTERS},${MR_CURL_RESULTS},${DB_CURL_RESULTS}" >> $2
  exit 0
else
  echo -e $(printf "${RED}FAILED: Detected difference between DB-backed solution output and file-backed solution output.${ENDCOLOR}")
  exit 1
fi
