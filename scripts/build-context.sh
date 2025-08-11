#!/bin/bash

# Build context document for Cue Android
# Collects README files and documentation to create CONTEXT.md

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUTPUT_FILE="$SCRIPT_DIR/CONTEXT.md"

# Ignore patterns
IGNORE_PATTERNS=(
    "*/git-hooks/*"
    "*/build/*"
    "*/.gradle/*"
    "*/temp/*"
)

echo "Building context from: $PROJECT_ROOT"
echo "Output will be saved to: $OUTPUT_FILE"

# Initialize stats
files_processed=0
total_chars=0
processed_files=()

# Function to check if path should be ignored
should_ignore() {
    local path="$1"
    for pattern in "${IGNORE_PATTERNS[@]}"; do
        if [[ $path == $pattern ]]; then
            return 0
        fi
    done
    return 1
}

# Get project name from directory
PROJECT_NAME=$(basename "$PROJECT_ROOT")

# Start the context file
cat > "$OUTPUT_FILE" << EOF
# ${PROJECT_NAME} Context

This document provides an overview of the ${PROJECT_NAME} codebase structure and documentation.

**Working Directory:** $PROJECT_ROOT
**Generated on:** $(date)

EOF

# Add docs/rules.md first if it exists
if [ -f "$PROJECT_ROOT/docs/rules.md" ]; then
    file_chars=$(wc -c < "$PROJECT_ROOT/docs/rules.md")
    total_chars=$((total_chars + file_chars))
    files_processed=$((files_processed + 1))
    processed_files+=("docs/rules.md")
    
    echo "## Development Rules" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo "**File:** docs/rules.md" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo '```markdown' >> "$OUTPUT_FILE"
    cat "$PROJECT_ROOT/docs/rules.md" >> "$OUTPUT_FILE"
    echo '```' >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
fi

# Add README files
echo "## README Files" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Find and process all README.md files
for readme_file in $(find "$PROJECT_ROOT" -name "README.md" | sort); do
    # Check if file should be ignored
    if should_ignore "$readme_file"; then
        continue
    fi
    relative_path="${readme_file#$PROJECT_ROOT/}"
    
    file_chars=$(wc -c < "$readme_file")
    total_chars=$((total_chars + file_chars))
    files_processed=$((files_processed + 1))
    processed_files+=("$relative_path")
    
    dir_name=$(dirname "$relative_path")
    if [ "$dir_name" = "." ]; then
        package_name="Root"
    else
        package_name=$(echo "$dir_name" | cut -d'/' -f1)
    fi
    
    echo "### $package_name" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo "**File:** $relative_path" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo '```markdown' >> "$OUTPUT_FILE"
    cat "$readme_file" >> "$OUTPUT_FILE"
    echo '```' >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

# Add other documentation files from docs folder
if [ -d "$PROJECT_ROOT/docs" ]; then
    echo "## Documentation Files" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"

    for doc_file in $(find "$PROJECT_ROOT/docs" -name "*.md" -not -name "CONTEXT.md" | sort); do
        # Check if file should be ignored
        if should_ignore "$doc_file"; then
            continue
        fi
        relative_path="${doc_file#$PROJECT_ROOT/}"
        filename=$(basename "$doc_file")
        
        # Skip rules.md as it's already added above
        if [ "$filename" = "rules.md" ]; then
            continue
        fi
        
        file_chars=$(wc -c < "$doc_file")
        total_chars=$((total_chars + file_chars))
        files_processed=$((files_processed + 1))
        processed_files+=("$relative_path")
        
        echo "### $filename" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo "**File:** $relative_path" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        echo '```markdown' >> "$OUTPUT_FILE"
        cat "$doc_file" >> "$OUTPUT_FILE"
        echo '```' >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
    done
fi

# Add configuration files from .cue and .claude directories
config_dirs=(".cue" ".claude")
config_found=false

for config_dir in "${config_dirs[@]}"; do
    if [ -d "$PROJECT_ROOT/$config_dir" ]; then
        for config_file in $(find "$PROJECT_ROOT/$config_dir" -name "*.md" 2>/dev/null | sort); do
            if [ ! -f "$config_file" ]; then
                continue
            fi
            
            if ! $config_found; then
                echo "## Configuration Files" >> "$OUTPUT_FILE"
                echo "" >> "$OUTPUT_FILE"
                config_found=true
            fi
            
            relative_path="${config_file#$PROJECT_ROOT/}"
            filename=$(basename "$config_file")
            
            file_chars=$(wc -c < "$config_file")
            total_chars=$((total_chars + file_chars))
            files_processed=$((files_processed + 1))
            processed_files+=("$relative_path")
            
            echo "### $filename" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            echo "**File:** $relative_path" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            echo '```markdown' >> "$OUTPUT_FILE"
            cat "$config_file" >> "$OUTPUT_FILE"
            echo '```' >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
        done
    fi
done

# Remove consecutive empty lines from the output file
sed -i '' '/^$/N;/^\n$/d' "$OUTPUT_FILE"

# Copy to CLAUDE.md at project root
cp "$OUTPUT_FILE" "$PROJECT_ROOT/CLAUDE.md"

echo ""
echo "========== STATS =========="
echo "Files included:"
for file in "${processed_files[@]}"; do
    echo "  - $file"
done
echo ""
echo "Files processed: $files_processed"
echo "Total characters: $total_chars"
echo "Output file size: $(wc -c < "$OUTPUT_FILE") characters"
echo "=========================="
echo ""
echo "Context document generated successfully!"
echo "Output saved to: $OUTPUT_FILE"
echo "Also saved to: $PROJECT_ROOT/CLAUDE.md"
