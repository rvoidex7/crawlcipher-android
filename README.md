# CrawlCipher-Android

CrawlCipher-Android wrapper project.

## Scope
This repository provides an Android wrapper host for the main project at `https://github.com/rvoidex7/CrawlCipher`.

- Runtime-agnostic architecture (no game feature logic in wrapper code)
- PTY-backed runtime execution for raw terminal fidelity
- Embedded terminal emulator renderer for ANSI/alternate screen/cursor behavior
- Binary-driven launch flow: package or replace runtime binary without wrapper feature edits
- Control overlay + hardware key forwarding (wrapper is not exposed as command-shell UX)
- Runtime contract (`runtime.json`) support for ABI/version/integrity policy

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
app/src/main/assets/runtime/runtime.json
```

`entrypoint.txt` format (fallback):
- First non-empty, non-comment line (`# ...`) = binary filename
- Following lines = one argument per line

Example:

```text
crawlcipher
--profile
mobile
```

If `entrypoint.txt` and `runtime.json` are both absent, the wrapper launches the first non-config file in the runtime directory.

### Runtime Contract (`runtime.json`)
When present, `runtime.json` is authoritative for launch + validation policy.

Example:

```json
{
  "entrypoint": "crawlcipher",
  "arguments": ["--profile", "mobile"],
  "supportedAbis": ["arm64-v8a", "armeabi-v7a"],
  "runtimeVersion": "1.8.0",
  "minWrapperVersion": "1.0.0",
  "sha256": "optional_sha256_hex",
  "signature": "optional_signature_payload"
}
```

Policy:
- ABI compatibility is validated before launch with clear user-facing status message.
- If `sha256` is set, runtime binary integrity is verified before launch.
- If `signature` is set, wrapper verifies `SHA256withRSA` signature using `public_key.pem` in runtime directory.
- If `minWrapperVersion` is set, wrapper version gate is enforced.

### Option B: Replace runtime on device
Drop a new executable directly into:

```text
/data/data/<package>/no_backup/runtime
```

Wrapper code does not require per-feature updates for game/runtime changes.

## Runtime Debugging
- In-app **Logs** button shows runtime lifecycle and output debug buffer.
- Debug log includes launch events, output fragments, and exit code.
- This keeps runtime diagnosis in wrapper scope without adding game-specific code.

## Compatibility Smoke Matrix
The repository includes baseline smoke matrix definitions for:
- startup success/failure scenarios
- input-path validation scenarios
- clean-exit expectations

This keeps binary compatibility expectations stable when runtime builds evolve.

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
