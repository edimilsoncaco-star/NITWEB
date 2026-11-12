#include <jni.h>
#include <cstring>
#include <cstdlib>
#include <android/log.h>
#include "esp/esp_renderer.h"

#define TAG "NexusJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// --- Declarações externas ---
extern void      push_packet(const uint8_t* data, int len);
extern void      process_queued_packets();

struct PlayerPos {
    int   player_id;
    float x, y, z;
};
extern PlayerPos* get_player_table(int* out_count);

struct AimbotPlayerPos {
    Vec3 world;
    bool alive;
    int  team;
};
extern Vec2 run_aimbot(const AimbotPlayerPos*, int, float, float, float, const float*);

// View matrix row-major 4x4 — identidade como padrão seguro
static float g_view_matrix[16] = {
    1,0,0,0,
    0,1,0,0,
    0,0,1,0,
    0,0,0,1
};

// Resolução de tela — atualizada via runAimbot a cada frame
static float g_sw = 1080.0f;
static float g_sh = 1920.0f;

// Detecta Free Fire via /proc — 0=nenhum 1=FF 2=FFMax
static int detect_game_internal() {
    const char* pkgs[2] = { "com.dts.freefireth", "com.dts.freefiremax" };
    for (int i = 0; i < 2; i++) {
        char cmd[128];
        snprintf(cmd, sizeof(cmd),
            "grep -rl '%s' /proc/*/cmdline 2>/dev/null | head -1", pkgs[i]);
        FILE* pipe = popen(cmd, "r");
        if (!pipe) continue;
        char result[64] = {};
        fgets(result, sizeof(result), pipe);
        pclose(pipe);
        if (result[0] != '\0') return i + 1;
    }
    return 0;
}

// ===================== JNI Exports =====================

extern "C"
JNIEXPORT void JNICALL
Java_com_nexus_painel_NexusBridge_parsePacket(
    JNIEnv* env, jobject, jbyteArray data, jint len)
{
    if (!data || len <= 0) return;
    jbyte* raw = env->GetByteArrayElements(data, nullptr);
    if (!raw) return;
    push_packet(reinterpret_cast<uint8_t*>(raw), (int)len);
    process_queued_packets();
    env->ReleaseByteArrayElements(data, raw, JNI_ABORT);
}

// Retorna [count, id0, screenX0, screenY0, dist0_meters, id1, ...]
// screenX/Y já projetados via world_to_screen com g_view_matrix
// dist em metros (z world / 100)
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_nexus_painel_NexusBridge_getPlayerPositions(JNIEnv* env, jobject)
{
    int count = 0;
    PlayerPos* table = get_player_table(&count);

    int total = 1 + count * 4;
    jfloatArray result = env->NewFloatArray(total);
    if (!result) return nullptr;

    jfloat* buf = new jfloat[total];
    buf[0] = (float)count;

    for (int i = 0; i < count; i++) {
        Vec3 world = { table[i].x, table[i].y, table[i].z };
        ScreenPos sp = world_to_screen(world, g_view_matrix, g_sw, g_sh);

        float dist = (world.z > 0.0f) ? (world.z / 100.0f) : 999.0f;

        buf[1 + i * 4 + 0] = (float)table[i].player_id;
        buf[1 + i * 4 + 1] = sp.visible ? sp.x : -1.0f;
        buf[1 + i * 4 + 2] = sp.visible ? sp.y : -1.0f;
        buf[1 + i * 4 + 3] = dist;
    }

    env->SetFloatArrayRegion(result, 0, total, buf);
    delete[] buf;
    return result;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_nexus_painel_NexusBridge_runAimbot(
    JNIEnv* env, jobject, jfloat fov, jfloat screenW, jfloat screenH)
{
    // Atualiza resolução global
    g_sw = screenW;
    g_sh = screenH;

    int count = 0;
    PlayerPos* table = get_player_table(&count);

    AimbotPlayerPos* apl = new AimbotPlayerPos[count];
    for (int i = 0; i < count; i++) {
        apl[i].world = { table[i].x, table[i].y, table[i].z };
        apl[i].alive = true;
        apl[i].team  = 0;
    }

    Vec2 target = run_aimbot(apl, count, fov, screenW * 0.5f, screenH * 0.5f, g_view_matrix);
    delete[] apl;

    jfloatArray result = env->NewFloatArray(2);
    if (!result) return nullptr;
    jfloat out[2] = { target.x, target.y };
    env->SetFloatArrayRegion(result, 0, 2, out);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nexus_painel_NexusBridge_detectGame(JNIEnv*, jobject) {
    return (jint)detect_game_internal();
}

// Recebe float[16] row-major da view matrix — OverlayService chama a cada frame
// FILL: alimentar com a view matrix real extraída do Free Fire via mem_reader
extern "C"
JNIEXPORT void JNICALL
Java_com_nexus_painel_NexusBridge_updateViewMatrix(
    JNIEnv* env, jobject, jfloatArray matrix)
{
    if (!matrix) return;
    if (env->GetArrayLength(matrix) < 16) return;
    jfloat* m = env->GetFloatArrayElements(matrix, nullptr);
    if (!m) return;
    memcpy(g_view_matrix, m, 16 * sizeof(float));
    env->ReleaseFloatArrayElements(matrix, m, JNI_ABORT);
}
