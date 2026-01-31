cmake_minimum_required(VERSION 3.20)
project(chartx-metal LANGUAGES C CXX OBJC OBJCXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_OBJCXX_STANDARD 17)

# macOS deployment target
set(CMAKE_OSX_DEPLOYMENT_TARGET "10.15" CACHE STRING "Minimum macOS version")

# Build universal binary for both arm64 and x86_64
set(CMAKE_OSX_ARCHITECTURES "arm64;x86_64" CACHE STRING "Build architectures")

# Find Java and JNI
# Use JAVA_HOME if set, otherwise find Java automatically
if(DEFINED ENV{JAVA_HOME})
    set(JAVA_HOME $ENV{JAVA_HOME})
else()
    execute_process(
        COMMAND /usr/libexec/java_home
        OUTPUT_VARIABLE JAVA_HOME
        OUTPUT_STRIP_TRAILING_WHITESPACE
        ERROR_QUIET
    )
endif()

if(JAVA_HOME)
    message(STATUS "Using JAVA_HOME: ${JAVA_HOME}")
    set(JAVA_INCLUDE_PATH "${JAVA_HOME}/include")
    set(JAVA_INCLUDE_PATH2 "${JAVA_HOME}/include/darwin")
    # Set hints for FindJNI
    set(JAVA_AWT_LIBRARY "${JAVA_HOME}/lib")
    set(JAVA_JVM_LIBRARY "${JAVA_HOME}/lib/server/libjvm.dylib")
endif()

find_package(JNI REQUIRED)
message(STATUS "JNI_INCLUDE_DIRS: ${JNI_INCLUDE_DIRS}")

# Find Metal and related frameworks
find_library(METAL_FRAMEWORK Metal REQUIRED)
find_library(FOUNDATION_FRAMEWORK Foundation REQUIRED)
find_library(QUARTZCORE_FRAMEWORK QuartzCore REQUIRED)

message(STATUS "Metal framework: ${METAL_FRAMEWORK}")
message(STATUS "Foundation framework: ${FOUNDATION_FRAMEWORK}")
message(STATUS "QuartzCore framework: ${QUARTZCORE_FRAMEWORK}")

# Source files
set(NATIVE_SOURCES
    src/MetalNative.mm
)

# Create shared library
add_library(chartx-metal SHARED ${NATIVE_SOURCES})

# Include directories
target_include_directories(chartx-metal PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link frameworks
target_link_libraries(chartx-metal PRIVATE
    ${METAL_FRAMEWORK}
    ${FOUNDATION_FRAMEWORK}
    ${QUARTZCORE_FRAMEWORK}
)

# Compiler flags
target_compile_options(chartx-metal PRIVATE
    -fobjc-arc                    # Enable Automatic Reference Counting
    -fvisibility=hidden           # Hide symbols by default
    -Wall                         # Enable warnings
    -Wextra
)

# Set library properties
set_target_properties(chartx-metal PROPERTIES
    OUTPUT_NAME "chartx-metal"
    PREFIX "lib"
    SUFFIX ".dylib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
)

# Install target
install(TARGETS chartx-metal
    LIBRARY DESTINATION lib
)

# Print build info
message(STATUS "Building chartx-metal for architectures: ${CMAKE_OSX_ARCHITECTURES}")
message(STATUS "macOS deployment target: ${CMAKE_OSX_DEPLOYMENT_TARGET}")
