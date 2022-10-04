#!/bin/bash
i=0
DIGESTS=()
STUDY_NAME=$1
zgrep -A1 "http://subsetting-internal:80/ss-internal/studies/${STUDY_NAME}" /var/log/containers/archive/docker.eda_qa_merging* | while read line
do
    LINE_WITHOUT_LOG_NAME=`echo ${line} | sed s/.*log.*\.gz\://g`
    # echo "First char: ${line::1}"
    if [[ ${LINE_WITHOUT_LOG_NAME::1} == "-" ]]; then
        echo "Found line to skip"
    elif [[ ${LINE_WITHOUT_LOG_NAME::1} == "{"  ]]; then
        NEW_REQUEST=`echo $LINE_WITHOUT_LOG_NAME | sed s/\}$/\\,\"reportConfig\":\{\"dataSource\":\"\{\{data_source\}\}\"\}\}/g`
        # MD5 is imperfect here, as ordering of params can differ for a semantically identical request.
        MD5=`echo $NEW_REQUEST | md5sum`
        if [[ " ${DIGESTS[*]} " =~ " ${MD5} " ]]; then
            echo "Already have this digest ${NEW_REQUEST}"
        else
            DIGESTS+=("${MD5}")
            echo $NEW_REQUEST
            echo $NEW_REQUEST > $OUTPUT_FILE
        fi
    else
      # echo $line | grep -o "studies/.*/entities/[A-Za-z0-9_-]*/"
      STUDY=`echo $line | grep -o "studies/.*/entities/[A-Za-z0-9_-]*/" | cut -d"/" -f2`
      if [[ $STUDY == "${STUDY_NAME}" && "${NEW_REQUEST}" != "SKIP" ]]; then
        ENTITY=`echo $line | grep -o "studies/.*/entities/[A-Za-z0-9_-]*/" | cut -d"/" -f4`
        mkdir -p "requests/${STUDY}/${ENTITY}"
        echo "Study: ${STUDY}, Entity: ${ENTITY}, Log line: ${LINE_WITHOUT_LOG_NAME}"
        OUTPUT_FILE="requests/${STUDY}/${ENTITY}/$((i++)).json"
      fi
    fi
done