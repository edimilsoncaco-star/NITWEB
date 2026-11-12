#include "esp_renderer.h"
#include <cmath>
#include <cstdio>
#include <cstring>

static char dist_buf[16];

float get_distance(Vec3 local, Vec3 enemy) {
    float dx = enemy.x - local.x;
    float dy = enemy.y - local.y;
    float dz = enemy.z - local.z;
    return sqrtf(dx * dx + dy * dy + dz * dz);
}

// Retorna ponteiro para buffer estático — copie antes do próximo uso
const char* format_distance(float dist) {
    int meters = (int)(dist + 0.5f);
    snprintf(dist_buf, sizeof(dist_buf), "%dm", meters);
    return dist_buf;
}
