#include "../esp/esp_renderer.h"
#include <cmath>
#include <cfloat>

struct PlayerPos {
    Vec3 world;
    bool alive;
    int  team;
};

// Retorna coordenada de tela do player mais próximo do crosshair dentro do FOV
// Retorna {-1, -1} se nenhum player estiver dentro do raio FOV
Vec2 run_aimbot(
    const PlayerPos* players,
    int              count,
    float            fov_radius,
    float            screen_cx,
    float            screen_cy,
    const float*     view_matrix
) {
    Vec2 result = {-1.0f, -1.0f};

    if (fov_radius <= 0.0f || count <= 0) {
        return result;
    }

    float best_dist = FLT_MAX;

    for (int i = 0; i < count; i++) {
        if (!players[i].alive) continue;

        ScreenPos sp = world_to_screen(players[i].world, view_matrix, screen_cx * 2.0f, screen_cy * 2.0f);
        if (!sp.visible) continue;

        float dx = sp.x - screen_cx;
        float dy = sp.y - screen_cy;
        float angular_dist = sqrtf(dx * dx + dy * dy);

        if (angular_dist < fov_radius && angular_dist < best_dist) {
            best_dist = angular_dist;
            result.x  = sp.x;
            result.y  = sp.y;
        }
    }

    return result;
}
