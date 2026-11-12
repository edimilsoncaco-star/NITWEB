#include "esp_renderer.h"
#include <cstring>

// Resultado do esqueleto: até 14 segmentos + 1 círculo de cabeça
// Cada segmento: 4 floats (x1,y1,x2,y2)
// Círculo de cabeça: retorna cx,cy,raio (os 3 primeiros floats do out_circle)
// out_segments: float[14 * 4] = 56 floats
// out_circle: float[3] = {cx, cy, radius}
// out_seg_count: número de segmentos escritos

void draw_esp_skeleton(const ScreenPos* bones16, float* out_segments, int* out_seg_count, float* out_circle) {
    int idx = 0;

    auto push_seg = [&](int a, int b) {
        if (!bones16[a].visible || !bones16[b].visible) return;
        out_segments[idx * 4 + 0] = bones16[a].x;
        out_segments[idx * 4 + 1] = bones16[a].y;
        out_segments[idx * 4 + 2] = bones16[b].x;
        out_segments[idx * 4 + 3] = bones16[b].y;
        idx++;
    };

    // Coluna central
    push_seg(NECK,  CHEST);
    push_seg(CHEST, PELVIS);

    // Braço esquerdo
    push_seg(CHEST,      L_SHOULDER);
    push_seg(L_SHOULDER, L_ELBOW);
    push_seg(L_ELBOW,    L_WRIST);

    // Braço direito
    push_seg(CHEST,      R_SHOULDER);
    push_seg(R_SHOULDER, R_ELBOW);
    push_seg(R_ELBOW,    R_WRIST);

    // Perna esquerda
    push_seg(PELVIS, L_HIP);
    push_seg(L_HIP,  L_KNEE);
    push_seg(L_KNEE, L_ANKLE);

    // Perna direita
    push_seg(PELVIS, R_HIP);
    push_seg(R_HIP,  R_KNEE);
    push_seg(R_KNEE, R_ANKLE);

    *out_seg_count = idx;

    // Cabeça: círculo centrado no osso HEAD, raio 8px
    out_circle[0] = bones16[HEAD].x;
    out_circle[1] = bones16[HEAD].y;
    out_circle[2] = 8.0f;
}
