#include "../esp/esp_renderer.h"
#include <cstring>
#include <cstdint>
#include <android/log.h>

#define TAG "NexusEntity"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Lê a memória via pid
extern bool read_mem(int pid, uintptr_t addr, void* buf, size_t sz);

// Offsets em bloco centralizado — preencher via GameGuardian/Cheat Engine
namespace OFF {
    uintptr_t ENTITY_LIST   = 0x0; // FILL: endereço da entity list no libil2cpp.so ou libUE4.so
    uintptr_t ENTITY_STRIDE = 0x0; // FILL: stride entre entidades (tamanho de cada ponteiro/struct)
    uintptr_t POS_OFFSET    = 0x0; // FILL: offset do Vec3 de posição dentro da entidade
    uintptr_t HP_OFFSET     = 0x0; // FILL: offset do int de HP
    uintptr_t TEAM_OFFSET   = 0x0; // FILL: offset do int de time
}

struct Entity {
    Vec3 pos;
    int  team;
    int  health;
    bool alive;
};

// Lê a entity list do Free Fire e popula o array out[]
// Retorna número de entidades lidas (0 se offsets não preenchidos)
int read_entity_list(int pid, Entity* out, int max_count) {
    if (OFF::ENTITY_LIST == 0 || OFF::ENTITY_STRIDE == 0) {
        LOGE("Offsets nao preenchidos — preencha namespace OFF em entity_list.cpp");
        return 0;
    }

    int count = 0;
    for (int i = 0; i < max_count; i++) {
        uintptr_t entity_addr = 0;
        uintptr_t entry_ptr   = OFF::ENTITY_LIST + (uintptr_t)(i * OFF::ENTITY_STRIDE);

        // Lê o ponteiro da entidade
        if (!read_mem(pid, entry_ptr, &entity_addr, sizeof(entity_addr))) break;
        if (entity_addr == 0) continue;

        // Lê posição
        Vec3 pos = {};
        if (!read_mem(pid, entity_addr + OFF::POS_OFFSET, &pos, sizeof(pos))) continue;

        // Lê HP
        int hp = 0;
        if (!read_mem(pid, entity_addr + OFF::HP_OFFSET, &hp, sizeof(hp))) continue;

        // Lê time
        int team = 0;
        if (!read_mem(pid, entity_addr + OFF::TEAM_OFFSET, &team, sizeof(team))) continue;

        out[count].pos    = pos;
        out[count].health = hp;
        out[count].team   = team;
        out[count].alive  = (hp > 0);
        count++;
    }

    return count;
}
