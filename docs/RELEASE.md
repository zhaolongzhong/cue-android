# Release Process

## Application Configuration

- **Production App ID**: `ai.plusonelabs.app.dev`
- **Debug App ID**: `ai.plusonelabs.app.dev.debug`
- **Namespace**: `com.example.cue`

## Local Development

### Building Debug APK
```bash
./scripts/build-release.sh --debug
```

### Building Release APK/AAB
```bash
./scripts/build-release.sh --release
```

## Release Requirements

### 1. Keystore Setup

For release builds, you need to configure signing:

1. Copy `keystore.properties.sample` to `keystore.properties`
2. Generate a keystore (if you don't have one):
   ```bash
   keytool -genkey -v -keystore upload-keystore.jks \
           -keyalg RSA -keysize 2048 -validity 10000 \
           -alias upload-keystore
   ```
3. Update `keystore.properties` with your credentials

### 2. API Configuration

Create `local.properties` with:
```properties
OPENAI_API_KEY=your_key_here
ANTHROPIC_API_KEY=your_key_here
API_BASE_URL=https://your-api.com
WEBSOCKET_BASE_URL=wss://your-api.com
```

## GitHub Actions Release Pipeline

### Automatic Release

Push to a release branch:
```bash
git checkout -b release/android/1.0.0
git push origin release/android/1.0.0
```

### Manual Release

Use the GitHub Actions workflow dispatch to manually trigger a release.

### Required Secrets

Configure these in GitHub repository settings:

- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_PASSWORD` - Key password
- `BASE64_ENCODED_KEYSTORE` - Base64 encoded keystore file
- `SERVICE_ACCOUNT_JSON` - Google Play service account JSON
- `OPENAI_API_KEY` - OpenAI API key
- `ANTHROPIC_API_KEY` - Anthropic API key
- `API_BASE_URL` - Backend API URL
- `WEBSOCKET_BASE_URL` - WebSocket URL

### Encoding Keystore

To encode your keystore for GitHub secrets:
```bash
base64 -i upload-keystore.jks | pbcopy
```

## Google Play Setup

1. Create app in Google Play Console with package name `ai.plusonelabs.app.dev`
2. Create service account for API access
3. Grant "Release Manager" permissions to service account
4. Download JSON key and add to GitHub secrets

## Version Management

- Version name: Set in branch name (e.g., `release/android/1.0.0`)
- Version code: Automatically incremented based on tags
- Tags format: `android-{version}-{code}` (e.g., `android-1.0.0-1`)

## Build Outputs

- Debug APK: `build/outputs/app-debug.apk`
- Release APK: `build/outputs/app-release.apk`
- Release Bundle: `build/outputs/app-release.aab`