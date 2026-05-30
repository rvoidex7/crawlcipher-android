# crawlcipher-android

CrawlCipher Android wrapper project.

## Scope
This repository provides an Android terminal host wrapper for the main project at `https://github.com/rvoidex7/CrawlCipher`.

- Runtime-agnostic wrapper design (no game command logic in wrapper code)
- Binary-driven launch flow: package or replace runtime binary without wrapper feature edits
- Touch + hardware keyboard input forwarding to the running binary process
- Prepared for long-term maintenance where runtime evolves independently from wrapper UI

## Runtime Binary Packaging
The app launches an executable from its runtime directory (`/data/data/<package>/no_backup/runtime`).

### Option A: Bundle runtime in APK assets
Put runtime files under:

```text
app/src/main/assets/runtime/
```

Examples:

```text
app/src/main/assets/runtime/crawlcipher
app/src/main/assets/runtime/entrypoint.txt
```

`entrypoint.txt` format:
- First non-empty, non-comment line (`# ...`) = binary filename
- Following lines = one argument per line

Example:

```text
crawlcipher
--profile
mobile
```

If `entrypoint.txt` is absent, the wrapper launches the first non-`.txt` file in the runtime directory.

### Option B: Replace runtime on device
Drop a new executable directly into:

```text
/data/data/<package>/no_backup/runtime
```

Wrapper code does not require per-feature updates for game/runtime changes.

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
