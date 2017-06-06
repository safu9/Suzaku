cmake_minimum_required(VERSION 3.4.1)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/../jniLibs)

add_library(lib_tag SHARED IMPORTED)
set_target_properties(lib_tag PROPERTIES IMPORTED_LOCATION
    ${distribution_DIR}/tag/lib/${ANDROID_ABI}/libtag.so)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

add_library(taglib_wrapper SHARED taglib_wrapper.cpp)

target_include_directories(taglib_wrapper PRIVATE
                           ${distribution_DIR}/tag/inc)

target_link_libraries(taglib_wrapper
                      android
                      lib_tag
                      log)
