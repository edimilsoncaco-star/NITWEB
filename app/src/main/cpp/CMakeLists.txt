cmake_minimum_required(VERSION 3.22)
project(nexus_esp)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Flags de release — reduz tamanho e surface de detecção
set(CMAKE_CXX_FLAGS_RELEASE "-O2 -fvisibility=hidden -ffunction-sections -fdata-sections")
set(CMAKE_SHARED_LINKER_FLAGS "-Wl,--gc-sections -Wl,--strip-all")

add_library(
    nexus_esp
    SHARED
    main.cpp
    esp/esp_line.cpp
    esp/esp_box.cpp
    esp/esp_skeleton.cpp
    esp/esp_distance.cpp
    aimbot/aimbot.cpp
    memory/mem_reader.cpp
    memory/entity_list.cpp
    vpn/local_proxy.cpp
    vpn/packet_parser.cpp
)

find_library(log-lib     log)
find_library(android-lib android)
find_library(EGL-lib     EGL)
find_library(GLESv2-lib  GLESv2)

target_link_libraries(
    nexus_esp
    ${log-lib}
    ${android-lib}
    ${EGL-lib}
    ${GLESv2-lib}
)

target_include_directories(nexus_esp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/esp
    ${CMAKE_CURRENT_SOURCE_DIR}/aimbot
    ${CMAKE_CURRENT_SOURCE_DIR}/memory
    ${CMAKE_CURRENT_SOURCE_DIR}/vpn
)
