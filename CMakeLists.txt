cmake_minimum_required(VERSION 3.10)
project(LightSwallow)

include_directories(${PROJECT_SOURCE_DIR})

set(CMAKE_CXX_STANDARD 17)
SET(CMAKE_CXX_COMPILER /usr/bin/g++)
#set(CMAKE_EXE_LINKER_FLAGS "-static-libgcc -static-libstdc++")

include_directories(include
        ${PROJECT_SOURCE_DIR}/lib
        )

#add_library(file_utils SHARED
#        ${PROJECT_SOURCE_DIR}/lib/file_utils.h
#        ${PROJECT_SOURCE_DIR}/lib/file_utils.cpp
#        )

add_library(pro_shared SHARED
        ${PROJECT_SOURCE_DIR}/lib/cgroups.h
        ${PROJECT_SOURCE_DIR}/lib/cgroups.cpp
        ${PROJECT_SOURCE_DIR}/lib/file_utils.h
        ${PROJECT_SOURCE_DIR}/lib/file_utils.cpp
        ${PROJECT_SOURCE_DIR}/SandboxExec.h
        ${PROJECT_SOURCE_DIR}/SandboxExec.cpp
        )


file(GLOB SOURCE_FILES "*.cpp" ".h")

add_executable(LightSwallow main.cpp)

#target_link_libraries(LightSwallow file_utils)
target_link_libraries(LightSwallow pro_shared)