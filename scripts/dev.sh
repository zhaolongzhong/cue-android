#!/bin/bash

# CUE Android - Unified Development Script
# Single entry point for all development workflows

set -e

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS_DIR="$PROJECT_ROOT/scripts"
LOGS_DIR="$PROJECT_ROOT/logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to display usage
usage() {
    echo "CUE Android Development Tool"
    echo ""
    echo "Usage: $0 <command> [arguments] [flags]"
    echo ""
    echo "Commands:"
    echo "  build                    - Build and run app on device"
    echo "  test [options]           - Run tests (-u unit, -i instrumented, -a all)"
    echo "  debug                    - Full debug session (build + screenshot + logs)"
    echo "  message <text>           - Send message in app"
    echo "  logs [--follow]          - Pull and show app logs"
    echo "  screenshot               - Take app screenshot"
    echo "  interact <cmd> [args]    - Direct UI interaction"
    echo "  lint                     - Run code linting"
    echo "  format                   - Format code"
    echo ""
    echo "Global flags:"
    echo "  --quiet                  - Minimal output"
    echo "  --json                   - Machine readable output (where supported)"
    echo "  --help                   - Show help for specific command"
    echo ""
    echo "Quick examples:"
    echo "  $0 build                 # Build and run"
    echo "  $0 debug                 # Complete debug workflow"
    echo "  $0 message 'Hello'       # Send test message"
    echo "  $0 logs --follow         # Pull logs and follow"
    echo "  $0 interact tap 500 1000 # Tap coordinates"
    echo ""
    echo "For detailed help: $0 <command> --help"
}

# Parse global flags
QUIET=false
JSON_OUTPUT=false
SHOW_HELP=false

# Process arguments
ARGS=()
while [[ $# -gt 0 ]]; do
    case $1 in
        --quiet)
            QUIET=true
            shift
            ;;
        --json)
            JSON_OUTPUT=true
            shift
            ;;
        --help)
            SHOW_HELP=true
            shift
            ;;
        *)
            ARGS+=("$1")
            shift
            ;;
    esac
done

# Restore positional parameters
set -- "${ARGS[@]}"

# Helper functions
log_info() {
    if [ "$QUIET" = false ]; then
        echo -e "${BLUE}ℹ️  $1${NC}"
    fi
}

log_success() {
    if [ "$QUIET" = false ]; then
        echo -e "${GREEN}✅ $1${NC}"
    fi
}

log_error() {
    echo -e "${RED}❌ $1${NC}" >&2
}

log_warning() {
    if [ "$QUIET" = false ]; then
        echo -e "${YELLOW}⚠️  $1${NC}"
    fi
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Main command processing
COMMAND="${1:-}"

case "$COMMAND" in
    "build")
        if [ "$SHOW_HELP" = true ]; then
            echo "Build and run the app on connected device/emulator"
            echo ""
            echo "Usage: $0 build [--quiet]"
            echo ""
            echo "This command:"
            echo "  1. Checks for connected device/emulator"
            echo "  2. Builds debug APK"
            echo "  3. Installs on device"
            echo "  4. Launches the app"
            exit 0
        fi
        
        log_info "Building and running app..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/run.sh" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/run.sh"
        fi
        log_success "App built and launched"
        ;;
        
    "test")
        if [ "$SHOW_HELP" = true ]; then
            echo "Run unit and/or instrumented tests"
            echo ""
            echo "Usage: $0 test [options]"
            echo ""
            echo "Options:"
            echo "  -u, --unit              Run unit tests only (default)"
            echo "  -i, --instrumented      Run instrumented tests only"
            echo "  -a, --all               Run all tests"
            echo ""
            echo "Examples:"
            echo "  $0 test                 # Run unit tests"
            echo "  $0 test -a              # Run all tests"
            exit 0
        fi
        
        # Pass remaining args to test script
        shift
        log_info "Running tests..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/test.sh" "$@" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/test.sh" "$@"
        fi
        log_success "Tests completed"
        ;;
        
    "debug")
        if [ "$SHOW_HELP" = true ]; then
            echo "Complete debug workflow"
            echo ""
            echo "Usage: $0 debug [--quiet]"
            echo ""
            echo "This command runs a full debug session:"
            echo "  1. Builds and deploys app"
            echo "  2. Takes screenshot"
            echo "  3. Pulls application logs"
            echo "  4. Analyzes logs for errors"
            echo "  5. Shows debug commands"
            exit 0
        fi
        
        log_info "Starting debug session..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/debug-session.sh" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/debug-session.sh"
        fi
        ;;
        
    "message")
        if [ "$SHOW_HELP" = true ]; then
            echo "Send a message in the app"
            echo ""
            echo "Usage: $0 message <text> [--quiet]"
            echo ""
            echo "This command:"
            echo "  1. Finds and focuses text input field"
            echo "  2. Enters the specified text"
            echo "  3. Taps send button"
            echo ""
            echo "Example:"
            echo "  $0 message 'Hello World'"
            exit 0
        fi
        
        if [ -z "$2" ]; then
            log_error "Message text required"
            echo "Usage: $0 message <text>"
            exit 1
        fi
        
        MESSAGE_TEXT="$2"
        log_info "Sending message: $MESSAGE_TEXT"
        
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/interact.sh" message "$MESSAGE_TEXT" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/interact.sh" message "$MESSAGE_TEXT"
        fi
        log_success "Message sent"
        ;;
        
    "logs")
        if [ "$SHOW_HELP" = true ]; then
            echo "Pull and display app logs"
            echo ""
            echo "Usage: $0 logs [--follow] [--quiet]"
            echo ""
            echo "Options:"
            echo "  --follow                Continue watching logs (like tail -f)"
            echo ""
            echo "This command:"
            echo "  1. Pulls logs from device to local machine"
            echo "  2. Shows recent log entries"
            echo "  3. Optionally follows new log entries"
            exit 0
        fi
        
        FOLLOW_LOGS=false
        if [[ " ${ARGS[*]} " =~ " --follow " ]]; then
            FOLLOW_LOGS=true
        fi
        
        log_info "Pulling app logs..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/pull-logs.sh" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/pull-logs.sh"
        fi
        
        # Find the latest log file
        LATEST_LOG=$(find "$LOGS_DIR" -name "cue-*.log" -type f -exec ls -t {} + | head -n1)
        
        if [ -n "$LATEST_LOG" ] && [ -f "$LATEST_LOG" ]; then
            if [ "$FOLLOW_LOGS" = true ]; then
                log_info "Following logs... (Ctrl+C to exit)"
                tail -f "$LATEST_LOG"
            else
                if [ "$QUIET" = false ]; then
                    echo ""
                    echo "📋 Recent log entries:"
                    echo "===================="
                fi
                tail -20 "$LATEST_LOG"
                if [ "$QUIET" = false ]; then
                    echo ""
                    echo "💡 For live logs: $0 logs --follow"
                    echo "💡 Full log: cat $LATEST_LOG"
                fi
            fi
        else
            log_warning "No log files found. Try running the app first."
        fi
        ;;
        
    "screenshot")
        if [ "$SHOW_HELP" = true ]; then
            echo "Take screenshot of current app state"
            echo ""
            echo "Usage: $0 screenshot [--quiet]"
            echo ""
            echo "Screenshots are saved to: logs/screenshots/"
            exit 0
        fi
        
        log_info "Taking screenshot..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/screenshot.sh" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/screenshot.sh"
        fi
        log_success "Screenshot captured"
        ;;
        
    "interact")
        if [ "$SHOW_HELP" = true ]; then
            echo "Direct UI interaction with the app"
            echo ""
            echo "Usage: $0 interact <command> [arguments]"
            echo ""
            echo "Available interaction commands:"
            echo "  tap <x> <y>              - Tap at coordinates"
            echo "  swipe <x1> <y1> <x2> <y2> - Swipe gesture"
            echo "  text <text>              - Input text"
            echo "  send                     - Tap send button"
            echo "  back                     - Press back button"
            echo "  home                     - Press home button"
            echo "  find <text>              - Find UI element"
            echo "  ui-dump                  - Export UI hierarchy"
            echo ""
            echo "Examples:"
            echo "  $0 interact tap 500 1000"
            echo "  $0 interact text 'Hello'"
            echo "  $0 interact find 'Login'"
            exit 0
        fi
        
        if [ -z "$2" ]; then
            log_error "Interaction command required"
            echo "Usage: $0 interact <command> [arguments]"
            echo "Try: $0 interact --help"
            exit 1
        fi
        
        # Pass all args after 'interact' to the interact script
        shift
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/interact.sh" "$@" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/interact.sh" "$@"
        fi
        ;;
        
    "lint")
        if [ "$SHOW_HELP" = true ]; then
            echo "Run code linting checks"
            echo ""
            echo "Usage: $0 lint [--quiet]"
            echo ""
            echo "Checks code formatting using Spotless"
            exit 0
        fi
        
        log_info "Running lint checks..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/lint.sh" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/lint.sh"
        fi
        log_success "Lint checks passed"
        ;;
        
    "format")
        if [ "$SHOW_HELP" = true ]; then
            echo "Auto-format code"
            echo ""
            echo "Usage: $0 format [--quiet]"
            echo ""
            echo "Automatically formats code using Spotless"
            exit 0
        fi
        
        log_info "Formatting code..."
        if [ "$QUIET" = true ]; then
            "$SCRIPTS_DIR/format.sh" >/dev/null 2>&1
        else
            "$SCRIPTS_DIR/format.sh"
        fi
        log_success "Code formatted"
        ;;
        
    "")
        usage
        ;;
        
    *)
        log_error "Unknown command: $COMMAND"
        echo ""
        echo "Available commands: build, test, debug, message, logs, screenshot, interact, lint, format"
        echo "For help: $0 --help"
        exit 1
        ;;
esac