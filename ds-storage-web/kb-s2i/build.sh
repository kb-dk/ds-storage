#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/ds-storage-*.war "$TOMCAT_APPS/ds-storage.war"
cp -- /tmp/src/conf/ocp/ds-storage.xml "$TOMCAT_APPS/ds-storage.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/ds-storage.war")
