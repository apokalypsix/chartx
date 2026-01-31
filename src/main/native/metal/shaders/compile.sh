#!/bin/bash
#
# Compiles Metal shaders into individual .metallib files
#
# Each shader is compiled to its own .metallib because they share
# common function names (vertexMain, fragmentMain) that would conflict
# if linked into a single library.
#
# Usage: ./compile.sh [output_dir]
#   output_dir - Optional directory for output (default: current directory)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${1:-$SCRIPT_DIR}"
BUILD_DIR="$SCRIPT_DIR/build"

# Shader files to compile
SHADERS=(
    "Default.metal"
    "Simple.metal"
    "Text.metal"
)

echo "=== Metal Shader Compiler ==="
echo "Source directory: $SCRIPT_DIR"
echo "Output directory: $OUTPUT_DIR"
echo ""

# Create build directory for intermediate files
mkdir -p "$BUILD_DIR"

# Check for xcrun
if ! command -v xcrun &> /dev/null; then
    echo "Error: xcrun not found. Please install Xcode Command Line Tools."
    echo "Run: xcode-select --install"
    exit 1
fi

# Check for metal compiler (requires full Xcode, not just Command Line Tools)
if ! xcrun -sdk macosx --find metal &> /dev/null; then
    echo "Error: Metal compiler not found."
    echo ""
    echo "The Metal compiler requires full Xcode installation:"
    echo "  1. Install Xcode from the App Store"
    echo "  2. Run: sudo xcode-select -s /Applications/Xcode.app/Contents/Developer"
    echo "  3. Run: sudo xcodebuild -license accept"
    echo ""
    exit 1
fi

echo "Metal compiler: $(xcrun -sdk macosx --find metal)"
echo ""

# Compile each shader to its own .metallib
COMPILED_COUNT=0
for shader in "${SHADERS[@]}"; do
    if [[ -f "$SCRIPT_DIR/$shader" ]]; then
        base_name="${shader%.metal}"
        air_file="$BUILD_DIR/${base_name}.air"
        metallib_file="$OUTPUT_DIR/${base_name}.metallib"

        echo "Compiling $shader..."

        # Compile to .air (intermediate representation)
        xcrun -sdk macosx metal -c "$SCRIPT_DIR/$shader" -o "$air_file" \
            -std=macos-metal2.0 \
            -mmacosx-version-min=10.15

        # Link to .metallib
        xcrun -sdk macosx metallib "$air_file" -o "$metallib_file"

        echo "  -> $metallib_file"
        ((COMPILED_COUNT++))
    else
        echo "Warning: $shader not found, skipping"
    fi
done

# Clean up intermediate files
rm -rf "$BUILD_DIR"

# Check if we compiled any shaders
if [[ $COMPILED_COUNT -eq 0 ]]; then
    echo "Error: No shaders were compiled"
    exit 1
fi

echo ""
echo "=== Build Complete ==="
echo "Compiled $COMPILED_COUNT shaders:"
ls -lh "$OUTPUT_DIR"/*.metallib 2>/dev/null || true
