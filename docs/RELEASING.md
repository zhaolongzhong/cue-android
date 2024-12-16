# Release Process

This document describes the process for releasing new versions of the Cue Android app.

## Prerequisites

1. Access to the Google Play Console
2. Required secrets configured in GitHub:
   - `KEYSTORE_BASE64`: Base64-encoded keystore file
   - `KEYSTORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password
   - `PLAY_STORE_CONFIG_JSON`: Google Play Store service account JSON

## Release Process

1. Create a release branch:
   ```bash
   git checkout -b release/X.Y.Z
   ```
   where X.Y.Z is the semantic version number (e.g., 1.0.0)

2. Push the branch to trigger the release workflow:
   ```bash
   git push origin release/X.Y.Z
   ```

3. The workflow will:
   - Build a release AAB
   - Upload to Play Store internal track
   - Create a git tag

4. Monitor the release in:
   - GitHub Actions: https://github.com/zhaolongzhong/cue-android/actions
   - Play Console: https://play.google.com/console

5. After testing the internal release:
   - Promote to production in Play Console
   - Merge the release branch to main

## Version Scheme

- Version name: X.Y.Z (semantic versioning)
- Version code: Auto-generated based on timestamp