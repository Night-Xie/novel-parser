#!/bin/bash

# Change this to your preferred output directory
OUTPUT_DIR="/Users/nightmare/Documents/HTTP_Novels/"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

echo "Starting batch conversion..."

# Loop through every .txt file in the novels directory
for file in novels/*.txt; do
    if [ -f "$file" ]; then
        echo "-----------------------------------"
        echo "Processing: $file"
        # We only need -i and -o now! The Java code does the rest.
        java src/NovelToHTML.java -i "$file" -o "$OUTPUT_DIR"
    fi
done

echo "-----------------------------------"
echo "All novels converted successfully!"
