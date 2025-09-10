# Ds-storage(Digitale Samlinger) by the Royal Danish Library. 

## ⚠️ Warning: Copyright Notice
Please note that it is not permitted to download and/or otherwise reuse content from the DR-archive at The Danish Royal Library.


## A metadata storage.
Ds-storage is a storage for metadata to describe objects in collections.  
The metadata format is UTF-8 encoded but format can be anything from text,XML,JSON etc. depending on the metadata format for that collection.
The basic idea behind ds-storage is a single access point to metadata describing objects belonging to many different collections. Having a simple API to 
store and retrieve the records. Instead of integrating to wide range of different APIs located on different servers, the ds-storage offers
a unified API for all of them and is optimized for retrieving a specific record very fast. 
    
See the Open API documentation for more details. (ds-storage-openapi_v1.yaml)

            
Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11
* Tomcat 9
* PostGreSql recommended (or any JDBC compliant database implementation)
* . 
* For local unittest as development it uses a file base H2 java database that does not require any software installation.

## Build & run

Build with
``` 
mvn package
```

## Setup required to run the project local 
Create local yaml-file: Take a copy of 'ds-storage-behaviour.yaml'  and name it'ds-storage-environment.yaml'

Update the dbURL for the h2-database file to your environment. I.e. replace XXX with your user.

The H2 will be created if it does not exist and data will be persistent between sessions. Delete the h2-file if you want to reset the database.


## Test the webservice with
```
mvn jetty:run
```
## Swagger UI
The Swagger UI is available at <http://localhost:9072/ds-storage/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 


## Deployment to a server (development/stage/production).
Install Tomcat9 server 

Install PostgreSql (or any JDBC database).

Create a database tablespace and define the tables using the file: resources/ddl/create_ds_storage.ddl

Configure tomcat with the context enviroment file conf/ocp/ds-storage.xml. Notice it points to the location on the file system where the yaml and logback file are located.

Edit  conf/ds-storage.logback.xml

Make a ds-storage.yaml file. (Make a copy of /conf/ds-storage-environment.yaml rename it, and edit the properties). 

Configure conf/ds-storage.yaml with the JDCB properties for the database. 

## Using a client to call the service 
This project produces a support JAR containing client code for calling the service from Java.
This can be used from an external project by adding the following to the [pom.xml](pom.xml):
```xml
<!-- Used by the OpenAPI client -->
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.2</version>
</dependency>

<dependency>
    <groupId>dk.kb.storage</groupId>
    <artifactId>ds-storage</artifactId>
    <version>1.0-SNAPSHOT</version>
    <type>jar</type>
    <classifier>classes</classifier>
    <!-- Do not perform transitive dependency resolving for the OpenAPI client -->
    <exclusions>
        <exclusion>
          <groupId>*</groupId>
          <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
after this a client can be created with
```java
    DsStorageClient storageClient = new DsStorageClient("https://example.com/ds-storage/v1");
```
During development, a SNAPSHOT for the OpenAPI client can be installed locally by running
```shell
mvn install
```

## Other
See the file [DEVELOPER.md](DEVELOPER.md) for more developer specific details.

