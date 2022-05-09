# Ds-storage(Digitale Samlinger) by the Royal Danish Library. 
    
      
## A metadata storage.
Ds-storage is a storage for metadata to describe objects in collections.  
The metadata format is UTF-8 encoded but format can be anything from text,XML,JSON etc. depending on the metadata format for that collection.
The basic idea behind ds-storage is a single access point to metadata describing objects belonging to many different collections. Having a simple API to 
store and retrieve the records. Instead of integrating to wide range of different APIs located on different servers, the ds-storage offers
a unified API for all of them and is optimized for retrieving a specific record very fast. 
    
## Records
Objects in ds-storage are called records. A record has a metadata field that is UTF-8 encoded. The value of the metadata will typical be same as
in the originating collection if harvested with OAI-PMH etc. 
        
A record must has a recordbase that is prefined in the ds-storage configuration. This is the name of the collection in ds-storage.
The recordId must also have the recordbase as prefix so it is easy from a recordid to see where it belongs.
When a record is created it will be given a creation time (cTime) that will never be changed and a modification time (mTime). If a
record is later update only the mTime will be updated. The timeformat of cTime and mTime is system milis with 3 added digits and is
guaranteed to be unique values.                      
    
## Naming convention for recordbase and id
The recordbase must only contain lowercase letters and dot (.) as separator.     
The id must start with the recordbase followed by colon (:). The part after recordbase must only contain of upper of lower case letters, digits and
the following characters:  : . _ -
        
Regexp for recordbase: ([a-z0-9.]+)
    
Regexp for id: ([a-z0-9.]+):([a-zA-Z0-9:._-]+)
    
## ID normalisation                     
If a record contains a invalid character after the recordbase part, it will be normalised and the invalid characters will be replaced.
The original (invalid) id will be stored in the 'orgid' field and flagged for invalid id. Having the original id will make it possible
to track it back to the collection it came from. The record can still be retrieved and updated using the invalid id, but also by the normalised id.                      
                                          
## Record hierarchy                 
The datamodel allows a single optional parent and unlimited number of children. Every recordbase is configured with a transitive 
update strategy that makes sense for the recordbase. When a record is created or update it can update the mTime of parent and all children
if defined for the recordbase. The possible updatestrategies are: NONE, ALL, PARENT, CHILDREN.  (see #updatestrategy schema)
        
## API   
Records can be extracted by recordId or as a list by specified recordbase and last modification time (mTime). The uniqueness of mTime
will ensure batching through the records using mTime will not return same record twice.  
    
         
Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11
* Tomcat 9
* PostGreSql recommended (or any JDBC compliant database implementation). 
* For local unittest as development it uses a file base H2 java database that does not require any software installation.

## Build & run

Build with
``` 
mvn package
```

## Setup required to run the project local 
Create local yaml-file: Take a copy of 'ds-storage-behaviour.yaml'  and name it'ds-storage-environment.yaml'

Update the dbURL for the h2-database file to your environment. Ie. replace XXX with your user.

The H2 will be created if does not exists and data will be persistent between sessions. Delete the h2-file if you want to reset the database.


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


See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
