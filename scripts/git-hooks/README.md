# Git Hooks

Automated git hooks for code quality and consistency.

## Quick Start

```bash
./scripts/git-hooks/install.sh
```

## Management

Use the management script for easy control:

```bash
# Check hook status
./scripts/manage-hooks.sh status

# Enable hooks
./scripts/manage-hooks.sh enable

# Disable hooks temporarily  
./scripts/manage-hooks.sh disable
```

## Features

### Protected Branches

Prevents direct commits to:

- main, master
- release, production
- develop

### Branch Naming

Format: `{type}/{description}`

- `feat/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation
- `style/` - Code style
- `refactor/` - Code refactoring
- `test/` - Tests
- `chore/` - Maintenance

### Commit Messages

Format: `type(optional-scope): message`

Examples:

```
feat: add user authentication
fix(memory): resolve memory leak
docs: update installation guide
```

### Auto-formatting

- Runs pre-commit format checks
- Auto-commits formatting fixes
- Restores state if format fails

## Skipping Hooks

You can skip the pre-commit hook when needed:

```bash
# Skip pre-commit for one commit using environment variable
SKIP_PRECOMMIT=1 git commit -m "urgent fix"

# Skip all hooks for one commit using git option
git commit -m "urgent fix" --no-verify
```

## Implementation

The hooks enforce:

1. Branch protection
2. Branch naming conventions
3. Commit message format
4. Code formatting standards

See `pre-commit` and `commit-msg` hooks in the `/scripts/git-hooks` directory for details.
