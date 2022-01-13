# ds-storage

Ds-Storage is persistent storage for metadata in the 'Digitiale samlinger' project. The metadata record are extracted for different collections and added to 
the ds-storage database. The encoding for the metadata is text UTF-8, but the format can be JSON or XML etc. dependant on the specific collection.
The records for a specific collection can be extracted by a streaming API or with a defined batch size. The extract method will return records
newer than the given modified data and are returned sorted by modified date. The responsibility for keeping track of the last modified data is up the
the caller and used as argument for the next extraction call.

For more information see the API description <http://localhost:8080/ds-storage/api/>.


Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11
* Tomcat 9
* PostGreSql

## Build & run

Build with
``` 
mvn package
```

## Create local yaml-file

Take a copy of 'ds-storage-behaviour.yaml'  and name it'ds-storage-environment.yaml'

And update the dbURL for the h2-database file to your environment. Ie. replace XXX with your user.



## Test the webservice with
```
mvn jetty:run
```
## Swagger UI
The Swagger UI is available at <http://localhost:8080/ds-storage/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
