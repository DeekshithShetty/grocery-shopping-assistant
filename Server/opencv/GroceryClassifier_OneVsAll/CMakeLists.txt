cmake_minimum_required(VERSION 2.8)
project( GroceryClassifier )
find_package( OpenCV REQUIRED )
add_executable( GroceryClassifier main.cpp )

find_package(OpenMP)
if (OPENMP_FOUND)
    set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${OpenMP_C_FLAGS}")
    set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
endif()

SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -std=c++0x")

target_link_libraries( GroceryClassifier ${OpenCV_LIBS} )