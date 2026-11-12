#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <android/log.h>

#define TAG "NexusMem"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Abre /proc e varre buscando o PID do pacote
int open_process(const char* pkg_name) {
    DIR* dir = opendir("/proc");
    if (!dir) {
        LOGE("Nao foi possivel abrir /proc");
        return -1;
    }

    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        // Só entradas numéricas são PIDs
        char* end;
        long pid = strtol(entry->d_name, &end, 10);
        if (*end != '\0') continue;

        // Lê /proc/<pid>/cmdline
        char path[64];
        snprintf(path, sizeof(path), "/proc/%ld/cmdline", pid);

        FILE* f = fopen(path, "r");
        if (!f) continue;

        char cmdline[256] = {};
        fread(cmdline, 1, sizeof(cmdline) - 1, f);
        fclose(f);

        if (strstr(cmdline, pkg_name) != nullptr) {
            closedir(dir);
            LOGI("PID encontrado: %ld para %s", pid, pkg_name);
            return (int)pid;
        }
    }

    closedir(dir);
    LOGE("Processo %s nao encontrado", pkg_name);
    return -1;
}

// Lê memória via /proc/<pid>/mem usando pread (funciona sem root em alguns casos)
bool read_mem(int pid, uintptr_t addr, void* buf, size_t sz) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/mem", pid);

    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        LOGE("Nao foi possivel abrir /proc/%d/mem", pid);
        return false;
    }

    ssize_t read_bytes = pread(fd, buf, sz, (off_t)addr);
    close(fd);

    if (read_bytes != (ssize_t)sz) {
        LOGE("read_mem falhou: addr=0x%lx sz=%zu leu=%zd", (unsigned long)addr, sz, read_bytes);
        return false;
    }

    return true;
}

// Faz parse de /proc/<pid>/maps para achar o base address de uma lib
uintptr_t find_module_base(int pid, const char* lib_name) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);

    FILE* f = fopen(path, "r");
    if (!f) {
        LOGE("Nao foi possivel abrir /proc/%d/maps", pid);
        return 0;
    }

    char line[512];
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, lib_name) == nullptr) continue;
        // Formato: startaddr-endaddr perms offset dev inode pathname
        uintptr_t start = 0;
        sscanf(line, "%lx-", &start);
        fclose(f);
        LOGI("Base de %s: 0x%lx", lib_name, (unsigned long)start);
        return start;
    }

    fclose(f);
    LOGE("Lib %s nao encontrada em maps do pid %d", lib_name, pid);
    return 0;
}
