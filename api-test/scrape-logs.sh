#!/bin/bash

#######################################
# Scrapes the Subsetting service log for example requests.
# Arguments:
#   $1 LOG_PATH: Path to the log file(s) to scrape.
#   $2 OUTPUT_DIR: Directory to dump scraped requests to.
#######################################

if (( $# != 2 ))
then
  echo "USAGE: $0 <log_path> <output_dir>"
  exit
fi

i=0
DIGESTS=()
LOG_PATH=$1
OUTPUT_DIR=$2

# Example zgrep output, output spans two lines with a third line to ignore:
# Note these logs lines always appear next to each other as they are logged in the same log statement on separate lines.
# /var/log/containers/archive/docker.eda_qa_merging_1.log-20220201.gz:ESC[mESC[32m2022-01-20 16:43:34.550 [rid:     ] INFO  ClientUtil:74 - Will send following POST request to http://subsetting-internal:80/ss-internal/studies/GEMSCC0003-1/entities/EUPATH0000096/tabular
# /var/log/containers/archive/docker.eda_qa_merging_1.log-20220201.gz:{"filters":[],"outputVariableIds":["EUPATH_0010367"]}
# /var/log/containers/archive/docker.eda_qa_merging_1.log-20220201.gz:--
zgrep -A1 "http://subsetting-internal:80/ss-internal/studies/" $LOG_PATH | while read line
do
    # zgrep with context contains the log file name. Strip this off so only log context remains.
    LINE_WITHOUT_LOG_NAME=`echo ${line} | sed s/.*log.*\.gz\://g`
    if [[ ${LINE_WITHOUT_LOG_NAME::1} == "-" ]]; then
        echo "Found line to skip"
    elif [[ ${LINE_WITHOUT_LOG_NAME::1} == "{"  ]]; then
        NEW_REQUEST=`echo $LINE_WITHOUT_LOG_NAME | sed s/\}$/\\,\"reportConfig\":\{\"dataSource\":\"\{\{data_source\}\}\"\}\}/g`
        MD5=`echo $NEW_REQUEST | md5sum`
        if [[ " ${DIGESTS[*]} " =~ " ${MD5} " ]]; then
            echo "Already have this digest ${NEW_REQUEST}"
        else
            DIGESTS+=("${MD5}")
            echo $NEW_REQUEST
            echo "Output file: $OUTPUT_FILE"
            echo $NEW_REQUEST > $OUTPUT_FILE
        fi
    else
      # echo $line | grep -o "studies/.*/entities/[A-Za-z0-9_-]*/"
      STUDY=`echo $line | grep -o "studies/.*/entities/[A-Za-z0-9_-]*/" | cut -d"/" -f2`
      ENTITY=`echo $line | grep -o "studies/.*/entities/[A-Za-z0-9_-]*/" | cut -d"/" -f4`
      mkdir -p "${OUTPUT_DIR}/${STUDY}/${ENTITY}"
      OUTPUT_FILE="${OUTPUT_DIR}/${STUDY}/${ENTITY}/$((i++)).json"
    fi
done