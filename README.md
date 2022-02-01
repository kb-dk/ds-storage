# ds-storage

Ds-Storage is persistent storage for metadata in the 'Digitiale samlinger' project. 

For more information about ds-storage and usage see the full description in open API <http://localhost:8080/ds-storage/api/>.
 

Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11
* Tomcat 9
* PostGreSql recommended (or any JDBC compliant database implementation). 
  For local unittest as development it uses a file base H2 java database that does not require any software installation.

## Build & run

Build with
``` 
mvn package
```

## Setup required to  run the project local 
Create local yaml-file: Take a copy of 'ds-storage-behaviour.yaml'  and name it'ds-storage-environment.yaml'
Update the dbURL for the h2-database file to your environment. Ie. replace XXX with your user.


## Test the webservice with
```
mvn jetty:run
```
## Swagger UI
The Swagger UI is available at <http://localhost:8080/ds-storage/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 


## Deployment to a server (development/stage/production).
Install Tomcat server 
Install PostgreSql (or any JDBC database). Create a database tablespace and define the tables using the file: create_ds_storage.ddl
Configure tomcat with the context enviroment file ds-storage.xml. Edit the two environment for ds-storage.logback.xml and ds-datahandler.yaml
Configure ds-datahandler.yaml with the JDCB properties for the database. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
