# Changelog
All notable changes to ds-storage will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## Changed
- Added two new fields to record: kalturareferenceid and kalturainternalid. The kaltura internal id is required by the frontend for thumbnails and streaming. [DRA-314](https://kb-dk.atlassian.net/browse/DRA-314)  
- New service method to update the kalturaId for a record. Both create new record and update record will set the kalturareferenceid. [DRA-314](https://kb-dk.atlassian.net/browse/DRA-314)
- Support for dynamically updating values in OpenAPI spec. [DRA-139](https://kb-dk.atlassian.net/browse/DRA-139).

## Added
- new table Mappings with fields (referenceId,kalturaId) and methods to create/update/read entries
- new service method that will enrich records in the records table with kalturaid from the mapping table

### Fixed
- Switch from Jersey to Apache URI Builder to handle parameters containing '{' [DRA-338](https://kb-dk.atlassian.net/browse/DRA-338)

## [1.18](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.18) 2024-03-08

### Changed
- openAPI endpoints for record/records refatored and changed. This is not backwards compatible.

## [1.16](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.16) 2024-03-05

### Changed
- Changed how the paging-Record-Count is calculated. A simple database lookup is now performed instead of creating an intermediary stream.
- Changed the origin used for StorageClient tests as 'ds.radiotv' is removed in the near future.
- Integration test uses aegis configuration.
- bump sbforge-parent to v25

### Removed
- Removed origin enums from openAPI specification

## [1.15](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.15) 2024-02-06

### Added
- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag
- Added the Paging-Record-Count header to responses from all endpoints that deliver multiple records.

## [1.14](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.14) 2024-01-22

### Changed
- logback template changes


## [1.12](https://github.com/kb-dk/ds-storage/releases/tag/v1.12) 2023-12-05
### Changed 
- General style of YAML configuration files, by removing the first level of indentation.
- Updated OpenAPI YAML with new example values



## [1.11.0] 2023-11-29
### Changed
- Multiple method names was changed in DsStorageClient to align naming with ds-client
- Some helper classes for continuation streams was moved to kb-util. No change to behaviour

## [1.10.0] 2023-11-27
### Added
- DsStorageClient has new method to stream records with no limit of number of records. 

## [1.9.0] 2023-11-15
### Added
- new API method to delete records by origin within a time interval. The purpose is to clean up old records after af full ingest.


## [1.7.0] - 2023-10-26
### Added
- Record object has new attribute:recordType. allowed recordTypes  are defined in the YAML config
- New method to retrieve all records from an origin and recordtype. The records will contain the local tree.

## [1.3.0] - 2023-10-18
### Added
- Client for ds-storage
- refactoring
- method to get record with local tree as object, immedient parent and children only. 


## [1.2.0] - 2022-02-XX
### Added
- API service methods renamed (url's).

### Changed
- ID normalisation:
  If a record contains a invalid character after the recordbase part, it will be normalised and the invalid characters will be replaced.
  The original (invalid) id will be stored in the 'orgid' field and flagged for invalid id. Having the original id will make it possible
  to track it back to the collection it came from. The record can still be retrieved and updated using the invalid id, but also by the normalised id.
  Regexp for recordbase: ([a-z0-9.]+)       
  Regexp for id: ([a-z0-9.]+):([a-zA-Z0-9:._-]+)

- Jetty port set explicitly to 9072 instead of default 8080 to avoid collisions with projects using default tomcat/jetty setup.


## [1.1.0] - 2022-02-01
### Added
- Test release, nothing new


## [1.0.0] - 2022-02-01
### Added

- Initial release of ds-storage


[Unreleased](https://github.com/kb-dk/ds-storage/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-storage/releases/tag/v1.0.0)
