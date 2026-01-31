#!/bin/bash
#
# Builds the chartx-metal native library (libchartx-metal.dylib)
#
# Usage: ./build.sh [options]
#   --clean       Clean build directory before building
#   --release     Build with optimizations (default: Debug)
#   --install     Copy dylib to resources directory
#   --verbose     Show detailed build output
#   --help        Show this help message
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
PROJECT_ROOT="$SCRIPT_DIR/../../../.."
RESOURCES_DIR="$PROJECT_ROOT/resources/native/darwin"

# Default options
CLEAN=false
BUILD_TYPE="Debug"
INSTALL=false
VERBOSE=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN=true
            shift
            ;;
        --release)
            BUILD_TYPE="Release"
            shift
            ;;
        --install)
            INSTALL=true
            shift
            ;;
        --verbose)
            VERBOSE="VERBOSE=1"
            shift
            ;;
        --help)
            echo "Usage: ./build.sh [options]"
            echo ""
            echo "Options:"
            echo "  --clean       Clean build directory before building"
            echo "  --release     Build with optimizations (default: Debug)"
            echo "  --install     Copy dylib to resources directory"
            echo "  --verbose     Show detailed build output"
            echo "  --help        Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Run './build.sh --help' for usage"
            exit 1
            ;;
    esac
done

echo "=== ChartX Metal Native Library Builder ==="
echo ""

# Check for required tools
if ! command -v cmake &> /dev/null; then
    echo "Error: cmake not found. Please install CMake."
    echo "  brew install cmake"
    exit 1
fi

if ! command -v make &> /dev/null; then
    echo "Error: make not found. Please install Xcode Command Line Tools."
    echo "  xcode-select --install"
    exit 1
fi

# Check Xcode license acceptance
if ! /usr/bin/xcrun clang --version &> /dev/null; then
    echo "Error: Xcode license has not been accepted."
    echo ""
    echo "Please run the following command and try again:"
    echo "  sudo xcodebuild -license accept"
    echo ""
    exit 1
fi

# Check for stale CMake cache from different directory
CMAKE_CACHE="$BUILD_DIR/CMakeCache.txt"
if [[ -f "$CMAKE_CACHE" ]]; then
    CACHED_SOURCE=$(grep "CMAKE_HOME_DIRECTORY:INTERNAL=" "$CMAKE_CACHE" 2>/dev/null | cut -d= -f2)
    if [[ -n "$CACHED_SOURCE" && "$CACHED_SOURCE" != "$SCRIPT_DIR" ]]; then
        echo "Detected stale CMake cache from different directory."
        echo "  Cached: $CACHED_SOURCE"
        echo "  Current: $SCRIPT_DIR"
        echo "Auto-cleaning build directory..."
        rm -rf "$BUILD_DIR"
    fi
fi

# Clean if requested
if [[ "$CLEAN" == true ]]; then
    echo "Cleaning build directory..."
    rm -rf "$BUILD_DIR"
fi

# Create build directory
mkdir -p "$BUILD_DIR"

# Configure with CMake
echo "Configuring with CMake (Build type: $BUILD_TYPE)..."
cd "$BUILD_DIR"
cmake -DCMAKE_BUILD_TYPE="$BUILD_TYPE" ..

# Build
echo ""
echo "Building libchartx-metal.dylib..."
make $VERBOSE -j$(sysctl -n hw.ncpu)

# Check if build succeeded
DYLIB_PATH="$BUILD_DIR/libchartx-metal.dylib"
if [[ ! -f "$DYLIB_PATH" ]]; then
    echo "Error: Build failed - dylib not found"
    exit 1
fi

echo ""
echo "=== Build Complete ==="
echo "Output: $DYLIB_PATH"
ls -lh "$DYLIB_PATH"

# Show architecture info
echo ""
echo "Architectures:"
lipo -info "$DYLIB_PATH"

# Install if requested
if [[ "$INSTALL" == true ]]; then
    echo ""
    echo "Installing to resources directory..."
    mkdir -p "$RESOURCES_DIR"
    cp "$DYLIB_PATH" "$RESOURCES_DIR/"
    echo "Installed: $RESOURCES_DIR/libchartx-metal.dylib"
fi

# Also build shaders if compile.sh exists
SHADER_SCRIPT="$SCRIPT_DIR/shaders/compile.sh"
if [[ -f "$SHADER_SCRIPT" ]]; then
    echo ""
    echo "=== Building Metal Shaders ==="
    bash "$SHADER_SCRIPT" "$BUILD_DIR"

    if [[ "$INSTALL" == true ]]; then
        for metallib in "$BUILD_DIR"/*.metallib; do
            if [[ -f "$metallib" ]]; then
                cp "$metallib" "$RESOURCES_DIR/"
                echo "Installed: $RESOURCES_DIR/$(basename "$metallib")"
            fi
        done
    fi
fi

echo ""
echo "Done."
