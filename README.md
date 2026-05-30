# crawlcipher-android

CrawlCipher Android wrapper project.

## Scope
This repository provides a simple Android wrapper app for the main project at `https://github.com/rvoidex7/CrawlCipher`.

- Embedded terminal-like interface without requiring external Termux installation
- Touch command input
- External hardware keyboard Enter support for the first release
- Bridge classes ready for packaged game binary/assets integration

## Build and Run
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

> Note: Actual CrawlCipher package/binary integration is intentionally left as placeholder in this repo, so you can connect your own packaged runtime later.

## Play Store Deployment
Fastlane files are included:

```bash
bundle install
bundle exec fastlane android build_release_bundle
bundle exec fastlane android deploy_internal
```

Required secret:
- `GOOGLE_PLAY_JSON_KEY`: Google Play service account JSON key (workflow or local environment)

GitHub Actions workflow: `.github/workflows/android-release.yml`
