cmake_minimum_required(VERSION 3.22.1)
project("beoutsec" C CXX)

# ─── C++ standard ────────────────────────────────────────────────────────────
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ─── Security & Optimization flags ───────────────────────────────────────────
add_compile_options(
    -O3
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -fstack-protector-strong
    -D_FORTIFY_SOURCE=2
)

add_link_options(
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,-z,relro
    -Wl,-z,now
)

# ─── libbeoutsec ─────────────────────────────────────────────────────────────
# Self-contained: jni.h + string.h + stdint.h + stdlib.h + stdio.h only.
# No dependency on libnext.so or any other prebuilt binary.
# Supports all ABIs: armeabi-v7a, arm64-v8a, x86, x86_64.
add_library(
    beoutsec
    SHARED
    beoutsec.cpp
)

target_include_directories(beoutsec PRIVATE
    ${ANDROID_NDK}/sysroot/usr/include
)

target_link_libraries(beoutsec
    android
    log
)
