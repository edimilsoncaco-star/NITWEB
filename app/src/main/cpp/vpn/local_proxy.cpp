#include <cstdint>
#include <cstring>
#include <cstddef>
#include <atomic>
#include <android/log.h>

#define TAG "NexusProxy"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static constexpr int   QUEUE_CAP    = 256;
static constexpr int   MAX_PKT_SIZE = 1500;

struct Packet {
    uint8_t data[MAX_PKT_SIZE];
    int     len;
};

static Packet        s_queue[QUEUE_CAP];
static std::atomic<int> s_head{0}; // producer escreve aqui
static std::atomic<int> s_tail{0}; // consumer lê aqui

// Chamado pelo VpnService.kt via JNI a cada pacote recebido
void push_packet(const uint8_t* data, int len) {
    if (len <= 0 || len > MAX_PKT_SIZE) {
        LOGE("push_packet: tamanho invalido %d", len);
        return;
    }

    int head = s_head.load(std::memory_order_relaxed);
    int next = (head + 1) % QUEUE_CAP;

    if (next == s_tail.load(std::memory_order_acquire)) {
        // Fila cheia — descarta o pacote mais antigo avançando o tail
        s_tail.store((s_tail.load() + 1) % QUEUE_CAP, std::memory_order_release);
    }

    memcpy(s_queue[head].data, data, (size_t)len);
    s_queue[head].len = len;
    s_head.store(next, std::memory_order_release);
}

// Chamado pelo packet_parser para consumir um pacote
// Retorna false se a fila estiver vazia
bool pop_packet(uint8_t* buf, int* out_len) {
    int tail = s_tail.load(std::memory_order_relaxed);
    if (tail == s_head.load(std::memory_order_acquire)) {
        return false; // vazia
    }

    *out_len = s_queue[tail].len;
    memcpy(buf, s_queue[tail].data, (size_t)*out_len);
    s_tail.store((tail + 1) % QUEUE_CAP, std::memory_order_release);
    return true;
}
