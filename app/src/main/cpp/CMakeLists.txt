project(NativeProject)

cmake_minimum_required(VERSION 3.18.1)

project(native)
set(CMAKE_CXX_STANDARD 20)

SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS_RELEASE}     -O2 -s -Wno-unused-value")
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_RELEASE} -O2 -s -Wno-unused-value")

set(CMAKE_C_STANDARD 11)

ADD_SUBDIRECTORY(${CMAKE_CURRENT_SOURCE_DIR}/native)
