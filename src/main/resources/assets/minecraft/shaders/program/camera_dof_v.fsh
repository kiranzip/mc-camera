#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec2 InSize;
uniform float NearPlane;
uniform float FarPlane;
uniform float FocusDistance;
uniform float FocalLength;
uniform float Aperture;
uniform float MaxBlur;
uniform float Exposure;
uniform float GrainAmount;
uniform float TimeSeed;

in vec2 texCoord;
out vec4 fragColor;

const int TAPS = 11;
const float SENSOR_WIDTH_MM = 36.0;
const float ACCEPTABLE_COC_MM = 0.020;

float linearise(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * NearPlane * FarPlane) / (FarPlane + NearPlane - z * (FarPlane - NearPlane));
}

float cocAt(vec2 uv) {
    float subject = max(linearise(texture(DepthSampler, uv).r), 0.05) * 1000.0;
    float focus = FocusDistance * 1000.0;
    float lens = max(focus - FocalLength, 1.0);

    float cocMm = (FocalLength * FocalLength / Aperture) * abs(subject - focus) / (subject * lens);
    float pixelsPerMm = InSize.x / SENSOR_WIDTH_MM;
    return clamp((cocMm - ACCEPTABLE_COC_MM) * pixelsPerMm, 0.0, MaxBlur);
}

float smoothedCoc(vec2 uv, vec2 texel) {
    float c = cocAt(uv);
    c += cocAt(uv + texel * vec2( 1.5,  1.5));
    c += cocAt(uv + texel * vec2(-1.5,  1.5));
    c += cocAt(uv + texel * vec2( 1.5, -1.5));
    c += cocAt(uv + texel * vec2(-1.5, -1.5));
    return c * 0.2;
}

vec3 gather(vec2 direction, vec2 texel, float radius) {
    vec3 sum = texture(DiffuseSampler, texCoord).rgb;
    float total = 1.0;

    for (int i = -TAPS; i <= TAPS; i++) {
        if (i == 0) {
            continue;
        }
        float t = float(i) / float(TAPS);
        float offsetPixels = t * radius;
        vec2 uv = clamp(texCoord + direction * texel * offsetPixels, vec2(0.0), vec2(1.0));

        float tapCoc = cocAt(uv);
        float reach = clamp(tapCoc - abs(offsetPixels) + 1.0, 0.0, 1.0);
        float weight = exp(-t * t * 1.8) * reach;

        sum += texture(DiffuseSampler, uv).rgb * weight;
        total += weight;
    }

    return sum / total;
}

float noise(vec2 uv) {
    return fract(sin(dot(uv + vec2(TimeSeed), vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 texel = 1.0 / InSize;
    float radius = smoothedCoc(texCoord, texel);

    vec3 colour = radius <= 0.5
        ? texture(DiffuseSampler, texCoord).rgb
        : gather(vec2(0.0, 1.0), texel, radius);

    colour *= Exposure;

    vec3 shouldered = colour / (colour + vec3(0.28)) * 1.28;
    colour = mix(colour, shouldered, clamp(Exposure - 1.0, 0.0, 1.0));

    if (GrainAmount > 0.0) {
        colour += (noise(texCoord) - 0.5) * GrainAmount;
    }

    fragColor = vec4(clamp(colour, 0.0, 1.0), 1.0);
}
