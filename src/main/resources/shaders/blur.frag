#version 130
in vec2 FragUV;
out vec4 OutColor;
uniform sampler2D uTexture;
uniform vec2 uDirection;
uniform vec2 uTexelSize;
uniform float uRadius;
void main() {
    vec4 sum = vec4(0.0);
    float total = 0.0;
    int r = int(uRadius);
    for (int i = -r; i <= r; i++) {
        float w = exp(-float(i * i) / (2.0 * uRadius * uRadius + 1.0));
        vec2 off = uDirection * uTexelSize * float(i);
        sum += texture2D(uTexture, FragUV + off) * w;
        total += w;
    }
    OutColor = sum / total;
}
