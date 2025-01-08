# Git Hooks for Android Development

This directory contains Git hooks for enforcing Android development workflow standards.

## Installation

Run the installation script:

```bash
./scripts/git-hooks/install.sh
```

## Features

The pre-commit hook enforces:

1. Protected Branch Rules
   - Prevents direct commits to main/master/release/production/develop
   - Shows helpful error messages with instructions
   - Provides emergency override option (--no-verify)

2. Branch Naming Convention
   - feature/feature-name (for new features)
   - bugfix/bug-name (for bug fixes)
   - docs/change-name (for documentation)
   - refactor/name (for code refactoring)
   - style/change-name (for styling changes)
   - test/suite-name (for testing changes)
   - chore/task-name (for maintenance tasks)

3. Commit Message Format
   Enforces conventional commits format:
   - feat: new feature
   - fix: bug fix
   - docs: documentation changes
   - style: formatting, layout, etc
   - refactor: code restructuring
   - test: adding unit/UI tests
   - chore: maintenance tasks

4. ktlint Integration
   - Automatically runs ktlint checks if installed
   - Prevents commits if ktlint finds issues

## Manual Override

In case of emergency, you can bypass the pre-commit hook using:
```bash
git commit --no-verify
```

**Note**: This is not recommended for normal development workflow.