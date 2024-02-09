add_library(
    native
    SHARED
    native.cpp
    elf.cpp
    AndroidHelper.cpp
)
find_library(
    log-lib
    log
)

target_link_libraries(
    native
    ${log-lib}
)