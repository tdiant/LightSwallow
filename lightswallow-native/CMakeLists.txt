cmake_minimum_required(VERSION 3.10)
project(LightSwallow)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_BUILD_TYPE Release)

set(JAVA_INCLUDE_PATH $ENV{JAVA_HOME}/include)
set(JAVA_INCLUDE_PATH2 $ENV{JAVA_HOME}/include/linux)
set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
set(JAVA_AWT_INCLUDE_PATH NotNeeded)

file(GLOB SOURCE_FILES
        "src/*.c"
        "src/*.cpp"
        "src/*.h"
        "src/jni/*.c"
        "src/jni/*.cpp"
        "src/jni/*.h"
        "lib/*.h"
        "lib/jni/*.h"
        )
include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_INCLUDE_PATH2})
include_directories(./)

find_package(JNI REQUIRED)
find_package(fmt CONFIG REQUIRED)

add_library(${PROJECT_NAME}-lib SHARED ${SOURCE_FILES})
target_link_libraries(${PROJECT_NAME}-lib PRIVATE fmt::fmt-header-only)
