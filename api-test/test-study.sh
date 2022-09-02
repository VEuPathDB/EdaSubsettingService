#!/usr/bin/env bash

if (( $# != 1 ))
then
  print "USAGE: $0 <request_body_file>"
  exit
fi

RED="\e[31m"
GREEN="\e[32m"
CYAN="\e[36m"
ENDCOLOR="\e[0m"

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

FILE_REQUEST_BODY=$(cat "$1" | sed 's/{{data_source}}/file/g')
DB_REQUEST_BODY=$(cat "$1" | sed 's/{{data_source}}/database/g')

REQUEST_DIR=$(dirname "$1")
OUTPUT_ENTITY_ID=$(echo "$REQUEST_DIR" | rev | cut -d"/" -f1 | rev)
STUDY_ID=$(dirname "$REQUEST_DIR" | rev | cut -d"/" -f1 | rev)

echo "MAP REDUCE:"
curl -o output/file_out -w 'Establish Connection: %{time_connect}s\nTTFB: %{time_starttransfer}s\nTotal: %{time_total}s\n' -s --location --request POST "${BASE_URL}/studies/${STUDY_ID}/entities/${OUTPUT_ENTITY_ID}/tabular" \
--header "Auth-Key: ${AUTH_KEY}" \
--header 'Content-Type: application/json' \
--data-raw "$FILE_REQUEST_BODY" \
--insecure
grep "\"status\":\"server-error\"" output/file_out

if [[ $? -eq 0 ]]
then
  echo $(printf "${RED}FAILED: Received server error when calling using map reduce configuratoin.${ENDCOLOR}")
  exit 1
fi

echo ""
echo "DATABASE:"
curl -o output/db_out -s \
-w 'Establish Connection: %{time_connect}s\nTTFB: %{time_starttransfer}s\nTotal: %{time_total}s\n' \
--location --request POST "${BASE_URL}/studies/${STUDY_ID}/entities/${OUTPUT_ENTITY_ID}/tabular" \
--header "Auth-Key: ${AUTH_KEY}" \
--header 'Content-Type: application/json' \
--data-raw "$DB_REQUEST_BODY" \
--insecure
grep "\"status\":\"server-error\"" output/db_out

if [[ $? -eq 0 ]]
then
  echo $(printf "${RED}FAILED: Received server error when calling using DB configuration.${ENDCOLOR}")
  exit 1
fi

echo ""
if diff output/file_out output/db_out
then
  echo -e $(printf "${GREEN}SUCCESS: No differences found in db output and file output!${ENDCOLOR}")
  exit 0
else
  echo -e $(printf "${RED}FAILED: Detected difference between DB-backed solution output and file-backed solution output.${ENDCOLOR}")
  exit 1
fi