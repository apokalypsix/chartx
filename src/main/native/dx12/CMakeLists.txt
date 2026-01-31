# CMakeLists.txt for ChartX DirectX 12 Native Library
# This file builds the DX12 JNI library for Windows

cmake_minimum_required(VERSION 3.16)
project(chartx-dx12 LANGUAGES CXX)

# C++17 required for structured bindings and other features
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Windows-specific settings
if(NOT WIN32)
    message(FATAL_ERROR "This project can only be built on Windows")
endif()

# Find JNI
find_package(JNI REQUIRED)
if(NOT JNI_FOUND)
    message(FATAL_ERROR "JNI not found. Make sure JAVA_HOME is set correctly.")
endif()

message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
message(STATUS "JNI libraries: ${JNI_LIBRARIES}")

# DirectX 12 libraries (from Windows SDK)
set(DX12_LIBS
    d3d12.lib
    dxgi.lib
    d3dcompiler.lib
)

# Source files
set(SOURCES
    src/DX12Native.cpp
)

# Header files (for IDE integration)
set(HEADERS
    include/DX12Common.h
)

# Create shared library (DLL on Windows)
add_library(chartx-dx12 SHARED ${SOURCES} ${HEADERS})

# Include directories
target_include_directories(chartx-dx12 PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link libraries
target_link_libraries(chartx-dx12 PRIVATE
    ${DX12_LIBS}
)

# Compile definitions
target_compile_definitions(chartx-dx12 PRIVATE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
    _CRT_SECURE_NO_WARNINGS
    UNICODE
    _UNICODE
)

# Debug definitions
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_definitions(chartx-dx12 PRIVATE _DEBUG)
endif()

# MSVC-specific options
if(MSVC)
    # Warning level
    target_compile_options(chartx-dx12 PRIVATE /W4)

    # Enable multi-processor compilation
    target_compile_options(chartx-dx12 PRIVATE /MP)

    # Disable specific warnings
    target_compile_options(chartx-dx12 PRIVATE
        /wd4100  # unreferenced formal parameter
        /wd4458  # declaration hides class member
    )
endif()

# Set output name without lib prefix
set_target_properties(chartx-dx12 PROPERTIES
    PREFIX ""
    OUTPUT_NAME "chartx-dx12"
)

# Output directory for the built library
set(NATIVE_OUTPUT_DIR "${CMAKE_SOURCE_DIR}/../resources/native/windows/x64")

# Copy DLL to resources directory after build
add_custom_command(TARGET chartx-dx12 POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E make_directory "${NATIVE_OUTPUT_DIR}"
    COMMAND ${CMAKE_COMMAND} -E copy
        $<TARGET_FILE:chartx-dx12>
        "${NATIVE_OUTPUT_DIR}/"
    COMMENT "Copying chartx-dx12.dll to ${NATIVE_OUTPUT_DIR}"
)

# Installation rules (optional, for system-wide install)
install(TARGETS chartx-dx12
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)

# Print configuration summary
message(STATUS "")
message(STATUS "ChartX DX12 Native Library Configuration:")
message(STATUS "  Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  C++ standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "  Output directory: ${NATIVE_OUTPUT_DIR}")
message(STATUS "")

# Build instructions printed at configure time
message(STATUS "Build Instructions:")
message(STATUS "  1. Open Visual Studio Developer Command Prompt")
message(STATUS "  2. cd to this directory")
message(STATUS "  3. mkdir build && cd build")
message(STATUS "  4. cmake .. -G \"Visual Studio 17 2022\" -A x64")
message(STATUS "  5. cmake --build . --config Release")
message(STATUS "")
