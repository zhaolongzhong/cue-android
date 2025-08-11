# Development Scripts Guide

This document describes the development scripts available in the `/scripts` directory for the CUE Android project.

## Overview

The project includes a unified development interface (`./scripts/dev.sh`) plus individual shell scripts and tools to streamline Android development workflows including building, testing, debugging, and device interaction.

## Quick Start - Unified Interface

**Primary Entry Point**: `./scripts/dev.sh`

The `dev.sh` script provides a single, consistent interface to all development tools:

```bash
# Most common workflows
./scripts/dev.sh build                    # Build and run app
./scripts/dev.sh debug                    # Complete debug session  
./scripts/dev.sh message "test"           # Send test message
./scripts/dev.sh logs --follow            # Pull and follow logs
./scripts/dev.sh screenshot               # Take screenshot

# Get help
./scripts/dev.sh --help                   # Show all commands
./scripts/dev.sh <command> --help         # Command-specific help

# Quiet mode for automation
./scripts/dev.sh build --quiet
./scripts/dev.sh logs --quiet
```

**Available Commands**:
- `build` - Build and run app on device
- `test` - Run tests (unit, instrumented, or all)
- `debug` - Full debug session (build + screenshot + logs + analysis)
- `message <text>` - Send message in app
- `logs [--follow]` - Pull and show app logs  
- `screenshot` - Take app screenshot
- `interact <cmd> [args]` - Direct UI interaction
- `lint` - Run code linting
- `format` - Format code

## Core Development Scripts

### Build & Test Scripts

#### `./scripts/run.sh`
**Purpose**: Build and run the app on connected device/emulator  
**Usage**: `./scripts/run.sh`

- Automatically starts emulator if none running
- Builds debug APK and installs on device
- Launches the app
- Provides helpful command suggestions for next steps

**Requirements**: Android SDK, connected device or available AVD

#### `./scripts/test.sh`
**Purpose**: Run unit and instrumented tests  
**Usage**: 
- `./scripts/test.sh` - Run unit tests (default)
- `./scripts/test.sh -u` - Run unit tests only  
- `./scripts/test.sh -i` - Run instrumented tests only
- `./scripts/test.sh -a` - Run all tests

**Output**: Test reports saved to `app/build/reports/tests/`

#### `./scripts/lint.sh`
**Purpose**: Run code linting and formatting checks  
**Usage**: `./scripts/lint.sh`

Uses Spotless for Kotlin code formatting validation.

#### `./scripts/format.sh`
**Purpose**: Auto-format code using Spotless  
**Usage**: `./scripts/format.sh`

Automatically fixes formatting issues found by lint.

### Debugging & Interaction Scripts

#### `./scripts/debug-session.sh`
**Purpose**: Complete debugging workflow - build, deploy, capture state  
**Usage**: `./scripts/debug-session.sh`

**Workflow**:
1. Checks device connection
2. Builds and deploys app
3. Takes screenshot
4. Pulls application logs
5. Analyzes logs for errors/warnings
6. Provides debug command suggestions

**Output**: Screenshots in `logs/screenshots/`, logs in `logs/`

#### `./scripts/interact.sh`
**Purpose**: Interact with the running app via ADB  
**Usage**: `./scripts/interact.sh <command> [args]`

**Commands**:
- `tap <x> <y>` - Tap at coordinates
- `swipe <x1> <y1> <x2> <y2> [duration]` - Swipe gesture  
- `text <text>` - Input text (auto-finds text fields)
- `send` - Send message (tap send button)
- `message <text>` - Complete message flow (input + send)
- `back` - Press back button
- `home` - Press home button  
- `screenshot` - Take screenshot
- `ui-dump` - Dump UI hierarchy to XML
- `find <text>` - Find UI element by text

**Examples**:
```bash
./scripts/interact.sh message "Hello World"
./scripts/interact.sh tap 500 1000
./scripts/interact.sh find "Login"
```

#### `./scripts/screenshot.sh`
**Purpose**: Capture app screenshot for debugging  
**Usage**: `./scripts/screenshot.sh`

**Output**: 
- Timestamped screenshot in `logs/screenshots/`
- `latest.png` symlink for quick access
- Suggests `open` command for viewing

#### `./scripts/pull-logs.sh`
**Purpose**: Pull application logs from device to local machine  
**Usage**: `./scripts/pull-logs.sh`

**Process**:
1. Connects to app's internal storage via `run-as`
2. Pulls all `.log` files from app's `files/logs/` directory
3. Saves to local `logs/` directory

**Output**: Logs available in `logs/cue-YYYY-MM-DD.log` format

### Advanced Tools

#### `./scripts/android_emulator.py`
**Purpose**: Python-based programmatic Android interface  
**Usage**: `python3 scripts/android_emulator.py <action> [options]`

**Actions**:
- `screenshot` - Take screenshot with UI analysis
- `tap --x X --y Y` - Tap coordinates
- `tap-element --text "Button Text"` - Tap by element text
- `input-text --text "Hello"` - Input text
- `send-message --text "Hello"` - Send chat message
- `ui-state` - Get detailed UI state

**Features**:
- Returns structured JSON results with `--json` flag
- Automatic UI element detection and classification
- Screen state analysis (login, home, chat, etc.)
- Suggests available actions based on current screen
- Error handling with detailed feedback

**Example**:
```bash
python3 scripts/android_emulator.py screenshot --json
python3 scripts/android_emulator.py send-message --text "Test message"
```

### Utility Scripts

#### `./scripts/build-context.sh`
**Purpose**: Generate project context documentation  
**Usage**: `./scripts/build-context.sh`

Creates `CLAUDE.md` with consolidated project documentation.

## Log Management

### Log Locations
- **Device**: `/data/data/com.example.cue/files/logs/`
- **Local**: `logs/cue-YYYY-MM-DD.log`
- **Screenshots**: `logs/screenshots/`

### Log Filtering
```bash
# View logs with FileLogger prefix
adb logcat | grep "\[AppLog\]"

# Pull and view local logs  
tail -f logs/cue-$(date +%Y-%m-%d).log
```

## Workflow Examples

### Quick Development Cycle (Recommended - Unified Interface)
```bash
# Build and run
./scripts/dev.sh build

# Take screenshot and check state
./scripts/dev.sh screenshot

# Send test message
./scripts/dev.sh message "test"

# Pull logs to check behavior
./scripts/dev.sh logs
```

### Full Debug Session
```bash
# Complete debugging workflow (recommended)
./scripts/dev.sh debug

# Interactive debugging
./scripts/dev.sh interact ui-dump
./scripts/dev.sh interact find "Send"
./scripts/dev.sh interact tap 500 1000
```

### Automated Testing
```bash
# Run all tests
./scripts/dev.sh test -a

# Check code format
./scripts/dev.sh lint

# Fix formatting if needed
./scripts/dev.sh format

# Automation-friendly (quiet mode)
./scripts/dev.sh build --quiet && ./scripts/dev.sh test --quiet
```

### Legacy Individual Scripts (still supported)
```bash
# Direct script usage (if preferred)
./scripts/run.sh
./scripts/debug-session.sh  
./scripts/interact.sh message "test"
./scripts/pull-logs.sh
```

## Troubleshooting

### Common Issues

**Build Failures**
- Check `build_output.log` for errors
- Try `./gradlew clean assembleDebug`
- Ensure Android SDK is properly configured

**Device Connection**
- Run `adb devices` to verify connection
- Restart adb: `adb kill-server && adb start-server`
- Check USB debugging is enabled

**Log Access Issues**
- App must be debuggable build
- Ensure app has written logs (run app first)
- Check device storage permissions

**UI Interaction Failures**
- Take screenshot to verify current state
- Use `ui-dump` to inspect element hierarchy
- Check coordinates are within screen bounds

### Performance Tips

- Use `python3 scripts/android_emulator.py` for programmatic access
- Combine commands: `./scripts/interact.sh message "test" && ./scripts/screenshot.sh`
- Use `--json` output for parsing in other tools

## Benefits of Unified Interface

### For Manual Use
- **Single entry point** - No need to remember multiple script names
- **Consistent help system** - `./scripts/dev.sh <command> --help` for any command
- **Better discoverability** - `./scripts/dev.sh --help` shows all available options
- **Improved UX** - Color-coded output, clear success/error messages

### For Automation
- **Consistent flags** - `--quiet` and `--json` work across commands where applicable
- **Reliable exit codes** - Proper error handling for CI/CD integration
- **Composable** - Easy to chain commands: `./scripts/dev.sh build --quiet && ./scripts/dev.sh test --quiet`

### Backward Compatibility
- All individual scripts remain fully functional
- No breaking changes to existing workflows
- Choose unified interface (`./scripts/dev.sh`) or individual scripts as preferred

## Script Maintenance

All scripts follow these conventions:
- Exit on error (`set -e`)
- Provide clear error messages with helpful suggestions
- Include usage examples
- Support both manual and automated use cases
- Clean up temporary files
- Unified interface provides consistent UX patterns

**Recommendation**: Use `./scripts/dev.sh` for new workflows, but individual scripts remain supported for existing automation.

For script modifications, ensure backward compatibility and update this documentation.