#pragma once
#include <cmath>

struct Vec3 {
    float x, y, z;
};

struct Vec2 {
    float x, y;
};

struct Color {
    float r, g, b, a;
};

constexpr Color WHITE  = {1.0f, 1.0f, 1.0f, 1.0f};
constexpr Color RED    = {1.0f, 0.0f, 0.0f, 1.0f};
constexpr Color GREEN  = {0.0f, 1.0f, 0.0f, 1.0f};

struct ScreenPos {
    float x, y;
    bool visible;
};

enum BoneId {
    HEAD       = 0,
    NECK       = 1,
    CHEST      = 2,
    PELVIS     = 3,
    L_SHOULDER = 4,
    L_ELBOW    = 5,
    L_WRIST    = 6,
    R_SHOULDER = 7,
    R_ELBOW    = 8,
    R_WRIST    = 9,
    L_HIP      = 10,
    L_KNEE     = 11,
    L_ANKLE    = 12,
    R_HIP      = 13,
    R_KNEE     = 14,
    R_ANKLE    = 15
};

struct Bone {
    Vec3 pos;
    int  id;
};

// viewMatrix4x4: row-major 4x4, layout [0..15]
// sw, sh: screen width/height in pixels
inline ScreenPos world_to_screen(Vec3 world, const float* viewMatrix4x4, float sw, float sh) {
    ScreenPos result = {0.0f, 0.0f, false};

    float clipX = viewMatrix4x4[0]  * world.x + viewMatrix4x4[1]  * world.y + viewMatrix4x4[2]  * world.z + viewMatrix4x4[3];
    float clipY = viewMatrix4x4[4]  * world.x + viewMatrix4x4[5]  * world.y + viewMatrix4x4[6]  * world.z + viewMatrix4x4[7];
    float clipW = viewMatrix4x4[12] * world.x + viewMatrix4x4[13] * world.y + viewMatrix4x4[14] * world.z + viewMatrix4x4[15];

    if (clipW < 0.001f) {
        return result;
    }

    float ndcX =  clipX / clipW;
    float ndcY = -clipY / clipW; // Y invertido (OpenGL → screen)

    result.x       = (ndcX + 1.0f) * 0.5f * sw;
    result.y       = (ndcY + 1.0f) * 0.5f * sh;
    result.visible = (ndcX >= -1.0f && ndcX <= 1.0f && ndcY >= -1.0f && ndcY <= 1.0f);

    return result;
}
