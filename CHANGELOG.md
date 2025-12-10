# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **BREAKING**: Updated Micronaut Framework from 3.9.5 to 4.10.2
- **BREAKING**: Updated Micronaut Gradle Plugin from 3.7.10 to 4.6.1
- **BREAKING**: Upgraded Gradle from 7.3.1 to 8.14
- **BREAKING**: Upgraded Java from version 11 to 21
- Updated Shadow plugin from legacy version 7.1.2 to new Gradle Shadow plugin 8.3.5
- Updated Docker base image from `azul/zulu-openjdk-alpine:11` to `azul/zulu-openjdk-alpine:21`
- **BREAKING**: Removed Google Cloud Platform deployment from CI/CD workflow
- Simplified GitHub Actions workflow to build Docker image and create releases on main branch
- Docker images now pushed to GitHub Container Registry (ghcr.io) instead of GCP
- Updated GitHub Actions versions (checkout@v4, cache@v4, setup-java@v4, docker/login-action@v3, docker/metadata-action@v5)
- Migrated from `javax.validation.*` to `jakarta.validation.*` annotations (Jakarta EE 9+ compatibility)
- Updated kafka-connect-transform-xml package from `com.github.jcustenborder` to `io.github.deepshore` (version 0.1.5.5)

### Added
- Added `io.micronaut.validation:micronaut-validation` dependency (replacing old `io.micronaut:micronaut-validation`)
- Added `io.micronaut.validation:micronaut-validation-processor` for annotation processing
- Added `jakarta.validation:jakarta.validation-api` dependency
- Added Confluent Maven repository configuration
- Added duplicate handling strategy for Gradle Copy tasks to resolve build conflicts
- Added `application-test.yml` with random port configuration for test environment
- Added GitHub Container Registry publishing with automatic tagging (latest, branch-sha, branch name)

### Fixed
- Fixed duplicate jar handling during build process (jaxb-core conflicts)
- Fixed test port binding issues by configuring random port allocation for tests
- Fixed validation module dependencies for Micronaut 4.x compatibility

### Migration Notes
- **Java 21 Required**: This version requires Java 21 or higher (required by kafka-connect-transform-xml subdependency)
- **Gradle 8.14**: Updated build system requires Gradle 8.14
- **Validation API**: All `javax.validation` imports must be updated to `jakarta.validation`
- **Micronaut 4.x Breaking Changes**: Applications must be reviewed for Micronaut 4.x breaking changes. See [Micronaut 4 Upgrade Guide](https://micronaut.io/2023/05/09/upgrade-to-micronaut-framework-4-0-0/)
