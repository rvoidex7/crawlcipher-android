cmake_minimum_required(VERSION 3.22.1)

project(crawlcipher_pty)

add_library(crawlcipher_pty SHARED pty_bridge.cpp)

find_library(log-lib log)

target_link_libraries(crawlcipher_pty ${log-lib})
