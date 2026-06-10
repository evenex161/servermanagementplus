#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;

out vec4 fragColor;

void main() {
    vec4 blurred = vec4(0.0);
    float totalSamples = 0.0;
    for(float r = -Radius; r <= Radius; r += 1.0) {
        blurred += texture(DiffuseSampler, texCoord + oneTexel * r * BlurDir);
        totalSamples += 1.0;
    }
    fragColor = blurred / totalSamples;
}
