# Changelog

All notable changes to ds-storage will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.0.0](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-4.0.0) - 2026-01-29

### Added
- New table in database (ds_transcriptions). DDL to create the table must be run for new release.
- New service method transcription(POST) to add or update a transcription. Also added to DsStorageClient
- New service method to load a transcription. Key is the external fileId (filename)

## [3.0.3](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-3.0.3) - 2025-12-03
### Fixed
- Fix multiple records have same stream defined.This should not happen but it does due to data errors.

## [3.0.1](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-3.0.1) 2025-09-01

### Changed

- Moved storage method that is only used by unit tests to a storage subclass used by unittest. The methods are very
  destructive such as clearing all tables.

## [3.0.0](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-3.0.0) 2025-06-12

### Added

- Added endpoint /record/touch/{id} which allows touching of single records
- Integration unittest with OAuth access token. Require kb-util v.1.6.10

### Fixed

- Fixed post record client method. Now it points at correct endpoint

### Changed

- touchRecord endpoint is now using a POST request instead of a GET request

## [2.3.3](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.3.3) 2025-03-05

## [Unreleased]

- Bumped SwaggerUI dependency to v5.18.2
- Bumped kb-util to v1.6.9 for service2service oauth support.
- Added injection of Oauth token on all service methods when using DsStorageClient.
- Removed auto generated DsStorageClient class that was a blocker for better exception handling. All DsStorageClient
  methods now only throws ServiceException mapped to HTTP status in same way calling the API directly.

### Added

- Property for database connection pool size. Under 'db' property new property: 'connectionPoolSize'. Reasonable values
  are from 10 to 100.

### Fixed

- Fixed inclusion of the same dependencies from multiple sources.

## [2.3.2](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.3.2) 2025-01-07

### Changed

- Upgraded dependency cxf-rt-transports-http to v.3.6.4 (fix memory leak)

## [2.3.1](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.3.1)

### Changed

- make all loggers static
- changed UPDATE_STRATEGY to NONE for ds.tv and ds.radio recordBase (origin)

## [2.3.0](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.3.0)

### Changed

- Changed connection pool size from 10 to 100

## [2.2.0](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.2.0)

### Added

- Enabled OAuth2 on /select (solrSearch) endpoint. Much is copy-paste from ds-image to see it working in two different
  modules.
  Plans are to refactor common functionality out into kb-util/template projects.

### Removed

- Removed non-resolvable git.tag from build.properties
- Removed double logging of part of the URL by bumping kb util to v1.5.10

## [2.1.1](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.1.1) 2024-07-16

### Added

- Added updateReferenceIdForReocrd method. The method is also added to the client.

## [2.1.0](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.1.0) 2024-07-16

### Changed

- Refactored endpoint record/referenceId to records/minimal
- Updated client with a ContinuationStream method calling the endpoint records/minimal
- Deprecated the client method getDsRecordsReferenceIdModifiedAfter

## [2.0.0](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-2.0.0) 2024-07-01

- Bumped version to fix x.y.z release format

## [1.22](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.22) 2024-06-24

## Added

- Added createMapping method (referenceid <-> kalturaId) to client.

## Changed

- Bumped kb-util version to improve YAML logging.

## [1.21](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.21) 2024-06-20

## Added

- Added new API method (/record/referenceids) to extract records with a minimum of fields. Is used to enrich kalturaid
  data from referenceId

## [1.19](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.19) 2024-05-10

### Changed

- Support for dynamically updating values in OpenAPI spec. [DRA-139](https://kb-dk.atlassian.net/browse/DRA-139).
- Change configuration style to camelCase [DRA-431](https://kb-dk.atlassian.net/browse/DRA-431)
- Changed Nexus used for deployment

## Added

- Added sample config files and documentation to distribution tar
  archive. [DRA-417](https://kb-dk.atlassian.net/browse/DRA-417)
- new table Mappings with fields (referenceId,kalturaId) and methods to create/update/read entries
- new service method that will enrich records in the records table with kalturaid from the mapping table
- New service method  (record/updateKalturaId) to update the kalturaId for a record. Both create new record and update
  record will set the kalturareferenceid. [DRA-314](https://kb-dk.atlassian.net/browse/DRA-314)
- new service method (records/updateKalturaId) that updates kalturaId for all records that have referenceId and no
  Kaltura, given the mapping reference<-> KalturaId is found in mapping table.
- Added two new fields to record: kalturareferenceid and kalturainternalid. The kaltura internal id is required by the
  frontend for thumbnails and streaming. [DRA-314](https://kb-dk.atlassian.net/browse/DRA-314)
- Added profiles to POM

### Fixed

- Switch from Jersey to Apache URI Builder to handle parameters
  containing '{' [DRA-338](https://kb-dk.atlassian.net/browse/DRA-338)
- Correct resolving of maven build time in project properties. [DRA-417](https://kb-dk.atlassian.net/browse/DRA-417)
- Wrongly defined URIs in the client.

## [1.18](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.18) 2024-03-08

### Changed

- openAPI endpoints for record/records refatored and changed. This is not backwards compatible.

## [1.16](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.16) 2024-03-05

### Changed

- Changed how the paging-Record-Count is calculated. A simple database lookup is now performed instead of creating an
  intermediary stream.
- Changed the origin used for StorageClient tests as 'ds.radiotv' is removed in the near future.
- Integration test uses aegis configuration.
- bump sbforge-parent to v25

### Removed

- Removed origin enums from openAPI specification

## [1.15](https://github.com/kb-dk/ds-storage/releases/tag/ds-storage-1.15) 2024-02-06

### Added

- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest
  commit and closest tag
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

- new API method to delete records by origin within a time interval. The purpose is to clean up old records after af
  full ingest.

## [1.7.0] - 2023-10-26

### Added

- Record object has new attribute:recordType. allowed recordTypes are defined in the YAML config
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
  If a record contains a invalid character after the recordbase part, it will be normalised and the invalid characters
  will be replaced.
  The original (invalid) id will be stored in the 'orgid' field and flagged for invalid id. Having the original id will
  make it possible
  to track it back to the collection it came from. The record can still be retrieved and updated using the invalid id,
  but also by the normalised id.
  Regexp for recordbase: ([a-z0-9.]+)       
  Regexp for id: ([a-z0-9.]+):([a-zA-Z0-9:._-]+)

- Jetty port set explicitly to 9072 instead of default 8080 to avoid collisions with projects using default tomcat/jetty
  setup.

## [1.1.0] - 2022-02-01

### Added

- Test release, nothing new

## [1.0.0] - 2022-02-01

### Added

- Initial release of ds-storage

[Unreleased](https://github.com/kb-dk/ds-storage/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-storage/releases/tag/v1.0.0)
