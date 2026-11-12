#include "esp_renderer.h"

// Retorna 8 floats: (x,y) dos 4 cantos do retângulo 2D ao redor do player
// Proporção 1:2 (largura : altura), calculada a partir da distância head→foot
// out[0..7] = {left,top, right,top, right,bottom, left,bottom}
void draw_esp_box(ScreenPos head, ScreenPos foot, float* out8) {
    float height = foot.y - head.y;
    if (height < 1.0f) height = 1.0f;

    float width  = height * 0.5f; // proporção 1:2
    float cx     = (head.x + foot.x) * 0.5f;

    float left   = cx - width * 0.5f;
    float right  = cx + width * 0.5f;
    float top    = head.y;
    float bottom = foot.y;

    // top-left
    out8[0] = left;  out8[1] = top;
    // top-right
    out8[2] = right; out8[3] = top;
    // bottom-right
    out8[4] = right; out8[5] = bottom;
    // bottom-left
    out8[6] = left;  out8[7] = bottom;
}
