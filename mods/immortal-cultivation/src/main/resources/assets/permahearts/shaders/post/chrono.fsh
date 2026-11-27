#version 330
#extension GL_ARB_separate_shader_objects : require

// D268 Time Rewind: everything seen through a stopped box turns grey (from inside the box: the whole view).

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// InvViewProj: clip space to camera-relative world. Params: x = boxes, y = stored depth of the empty sky, z = NDC z of the near plane.
// The depth texture holds NDC z (-1..1) as 0..1.
// Box(2i) / Box(2i+1): min / max corner of box i, camera-relative.
layout(std140) uniform ChronoConfig {
    mat4 InvViewProj;
    vec4 Params;
    vec4 Box[8];
};

layout(location = 0) out vec4 fragColor;

vec3 unproject(vec2 uv, float ndcZ) {
    vec4 p = InvViewProj * vec4(uv * 2.0 - 1.0, ndcZ, 1.0);
    return p.xyz / p.w;
}

// Does the segment from the camera (origin) along dir, length len, touch the box?
bool hits(vec3 dir, float len, vec3 lo, vec3 hi) {
    vec3 inv = 1.0 / dir;
    vec3 t0 = lo * inv;
    vec3 t1 = hi * inv;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    float a = max(max(tmin.x, tmin.y), tmin.z);
    float b = min(min(tmax.x, tmax.y), tmax.z);
    return b >= max(a, 0.0) && a <= len;
}

void main() {
    vec4 color = texture(InSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    vec3 nearPoint = unproject(texCoord, Params.z);
    vec3 dir = normalize(nearPoint);
    float len = abs(depth - Params.y) < 1e-7 ? 1e9 : length(unproject(texCoord, depth * 2.0 - 1.0));
    bool grey = false;
    int n = int(Params.x + 0.5);
    for (int i = 0; i < 4; i++) {
        if (i < n && hits(dir, len, Box[i * 2].xyz, Box[i * 2 + 1].xyz)) {
            grey = true;
        }
    }
    if (grey) {
        float l = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        color.rgb = vec3(l * 0.92 + 0.03);
    }
    fragColor = vec4(color.rgb, 1.0);
}
