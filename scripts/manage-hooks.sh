#!/bin/bash

# Git Hooks Management Script
# Usage: ./scripts/manage-hooks.sh {enable|disable|status}

set -e

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

case "$1" in
    enable)
        echo "🔧 Installing git hooks..."
        ./scripts/git-hooks/install.sh
        echo "✅ Git hooks enabled"
        echo ""
        echo "💡 To skip pre-commit hook when needed:"
        echo "   SKIP_PRECOMMIT=1 git commit -m \"message\""
        echo "   # or"
        echo "   git commit -m \"message\" --no-verify"
        ;;
    disable)
        echo "🔧 Disabling git hooks..."
        if [ -f .git/hooks/pre-commit ]; then
            mv .git/hooks/pre-commit .git/hooks/pre-commit.disabled
            echo "⚠️  Pre-commit hook disabled"
        fi
        if [ -f .git/hooks/commit-msg ]; then
            mv .git/hooks/commit-msg .git/hooks/commit-msg.disabled
            echo "⚠️  Commit-msg hook disabled"
        fi
        echo "✅ Git hooks disabled"
        echo ""
        echo "💡 To re-enable: ./scripts/manage-hooks.sh enable"
        ;;
    status)
        echo "📊 Git Hooks Status:"
        if [ -x .git/hooks/pre-commit ]; then
            echo "  ✅ pre-commit: enabled"
        elif [ -f .git/hooks/pre-commit.disabled ]; then
            echo "  ⚠️  pre-commit: disabled (use 'enable' to restore)"
        else
            echo "  ❌ pre-commit: not installed"
        fi
        
        if [ -x .git/hooks/commit-msg ]; then
            echo "  ✅ commit-msg: enabled"
        elif [ -f .git/hooks/commit-msg.disabled ]; then
            echo "  ⚠️  commit-msg: disabled (use 'enable' to restore)"
        else
            echo "  ❌ commit-msg: not installed"
        fi
        
        echo ""
        echo "💡 Usage options:"
        if [ -x .git/hooks/pre-commit ]; then
            echo "  Skip once: SKIP_PRECOMMIT=1 git commit -m \"message\""
            echo "  Skip once: git commit -m \"message\" --no-verify"
            echo "  Disable:   ./scripts/manage-hooks.sh disable"
        else
            echo "  Enable:    ./scripts/manage-hooks.sh enable"
        fi
        ;;
    *)
        echo "Git Hooks Management"
        echo ""
        echo "Usage: $0 {enable|disable|status}"
        echo ""
        echo "Commands:"
        echo "  enable   - Install/enable git hooks"
        echo "  disable  - Temporarily disable git hooks"
        echo "  status   - Show current hook status"
        echo ""
        echo "Examples:"
        echo "  $0 status                     # Check current status"
        echo "  $0 disable                   # Disable hooks temporarily"
        echo "  SKIP_PRECOMMIT=1 git commit  # Skip pre-commit for one commit"
        echo "  git commit --no-verify        # Skip all hooks for one commit"
        exit 1
        ;;
esac
