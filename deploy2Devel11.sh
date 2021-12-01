#!/bin/sh

mvn clean package -DskipTests
mv target/ds-storage*.war target/ds-storage.war

scp target/ds-storage.war digisam@devel11:/home/digisam/services/tomcat-apps/

echo "ds-storage deployed to devel11"
