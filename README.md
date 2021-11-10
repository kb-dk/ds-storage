# ds-storage

**Description of the application goes here**

Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11

## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 8080 and the default Hello World service can be accessed at
<http://localhost:8080/ds-storage/v1/hello>

The Swagger UI is available at <http://localhost:8080/ds-storage/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
