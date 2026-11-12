#include "esp_renderer.h"

// Retorna 4 floats: x1,y1 (centro inferior da tela) → x2,y2 (posição do enemy)
// Espessura 2px é aplicada pelo renderer Kotlin/Canvas no lado Java
void draw_esp_line(ScreenPos enemy, float sw, float sh, float* out_x1, float* out_y1, float* out_x2, float* out_y2) {
    *out_x1 = sw * 0.5f;
    *out_y1 = sh;
    *out_x2 = enemy.x;
    *out_y2 = enemy.y;
}
