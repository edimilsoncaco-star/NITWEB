#include <cstdint>
#include <cstring>
#include <cstdio>
#include <android/log.h>

#define TAG "NexusParser"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern bool pop_packet(uint8_t* buf, int* out_len);

// FILL: capturar os magic bytes reais via tcpdump no Termux:
//   tcpdump -i any -X udp and host <IP_DO_SERVIDOR> -w ff_packets.pcap
// Exemplo provisório — substitua pelos bytes corretos do protocolo da versão em uso
static constexpr uint8_t MAGIC_BYTES[] = { 0xFF, 0xFF }; // FILL
static constexpr int     MAGIC_LEN     = 2;              // FILL: ajustar comprimento

struct PlayerPos {
    int   player_id;
    float x, y, z;
};

static PlayerPos player_table[60] = {};
static int       player_count     = 0;

// Faz parse de um pacote UDP e atualiza player_table[]
// Retorna true se o pacote era válido e continha posição
bool parse_packet(const uint8_t* data, int len) {
    if (len < MAGIC_LEN + 1 + 12) {
        return false; // muito curto
    }

    // Verifica magic bytes
    if (memcmp(data, MAGIC_BYTES, MAGIC_LEN) != 0) {
        return false;
    }

    // FILL: ajuste os offsets abaixo de acordo com o protocolo real capturado
    // Layout assumido após os magic bytes: [1 byte player_id][4 float x][4 float y][4 float z]
    int offset = MAGIC_LEN;

    if (offset + 1 + 12 > len) return false;

    uint8_t pid = data[offset];
    offset += 1;

    float x, y, z;
    memcpy(&x, data + offset,     4);
    memcpy(&y, data + offset + 4, 4);
    memcpy(&z, data + offset + 8, 4);

    if (pid >= 60) {
        LOGE("player_id fora do intervalo: %d", (int)pid);
        return false;
    }

    player_table[pid].player_id = pid;
    player_table[pid].x         = x;
    player_table[pid].y         = y;
    player_table[pid].z         = z;

    if (pid >= player_count) {
        player_count = pid + 1;
    }

    return true;
}

// Drena a fila de pacotes e faz parse de todos
void process_queued_packets() {
    uint8_t buf[1500];
    int     len = 0;
    while (pop_packet(buf, &len)) {
        parse_packet(buf, len);
    }
}

// Retorna ponteiro para a tabela global de posições
PlayerPos* get_player_table(int* out_count) {
    *out_count = player_count;
    return player_table;
}
