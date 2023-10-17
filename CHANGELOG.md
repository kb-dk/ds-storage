# Changelog
All notable changes to ds-storage will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2023-10-18
### Added
- Client for ds-storage
- refactoring
- method to get record with local tree as object, immedient parent and children only. 


## [1.2.0] - 2022-02-XX
### Added
- API service methods renamed (url's).

### Changed
ID normalisation:
If a record contains a invalid character after the recordbase part, it will be normalised and the invalid characters will be replaced.
The original (invalid) id will be stored in the 'orgid' field and flagged for invalid id. Having the original id will make it possible
to track it back to the collection it came from. The record can still be retrieved and updated using the invalid id, but also by the normalised id.
Regexp for recordbase: ([a-z0-9.]+)       
Regexp for id: ([a-z0-9.]+):([a-zA-Z0-9:._-]+)

Jetty port set explicitly to 9072 instead of default 8080 to avoid collisions with projects using default tomcat/jetty setup.


## [1.1.0] - 2022-02-01
### Added
Test release, nothing new


## [1.0.0] - 2022-02-01
### Added

- Initial release of ds-storage


[Unreleased](https://github.com/kb-dk/ds-storage/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-storage/releases/tag/v1.0.0)
