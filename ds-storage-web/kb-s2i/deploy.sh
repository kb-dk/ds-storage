#!/usr/bin/env bash

cp -- /tmp/src/conf/ocp/logback.xml "$CONF_DIR/logback.xml"
# There are normally two configurations: core and environment
cp -- /tmp/src/conf/ds-storage-*.yaml "$CONF_DIR/"
 
ln -s -- "$TOMCAT_APPS/ds-storage.xml" "$DEPLOYMENT_DESC_DIR/ds-storage.xml"
