# Change Log
All notable changes to this project will be documented in this file, which follows the guidelines
on [Keep a CHANGELOG](http://keepachangelog.com/). This project adheres to
[Semantic Versioning](http://semver.org/).

## [Unreleased]

## [25.104.0-M2] - 2026-07-07
### Changed
- Updated parent `service-parent-pom` to `25.104.0-M6` and `coredomain.version` to `25.104.0-M6` — picks up the released platform chain (platform-libraries M7 / framework M3 / event-store M4), whose event-listener service-component now delivers `persistence-jpa` (the event-stream self-healing `EntityManagerFlushInterceptor` + `EntityManagerProducer`). Full integration test suite green (205 passed, 3 skipped)

### Fixed
- Corrected the project version to the `25.104.0-M` milestone line (was `25.104.2-M2-SNAPSHOT`; the release pipeline had been incrementing the patch digit `.0 → .1 → .2` on each milestone release instead of the `-M` suffix)
