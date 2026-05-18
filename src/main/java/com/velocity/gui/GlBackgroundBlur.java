package com.velocity.gui;

import com.velocity.gui.framework.UiColors;
import com.velocity.gui.framework.UiScale;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import imgui.ImDrawList;
import imgui.ImGui;

/**
 * Real separable Gaussian blur on the overlay framebuffer (ported intent from C++ blur.cpp).
 * Disabled automatically if GL setup fails.
 */
public final class GlBackgroundBlur {
    private GlBackgroundBlur() {}

    public static final boolean ENABLED = true;

    private static boolean initialized = false;
    private static boolean available = false;

    private static int program = 0;
    private static int uTexture, uDirection, uTexelSize, uRadius;
    private static int vao, vbo;
    private static int fboA, fboB;
    private static int texSource, texTempA, texTempB;
    private static int width, height;
    private static int blurredTexture = 0;

    public static int blurredTextureId() {
        return blurredTexture;
    }

    public static boolean isAvailable() {
        return ENABLED && available;
    }

    public static void resize(int w, int h) {
        if (!ENABLED) return;
        if (w <= 0 || h <= 0) return;
        if (w == width && h == height && initialized) return;
        width = w;
        height = h;
        initIfNeeded();
        if (!available) return;
        destroyTextures();
        texSource = createTexture(w, h);
        texTempA = createTexture(w, h);
        texTempB = createTexture(w, h);
        fboA = createFbo(texTempA);
        fboB = createFbo(texTempB);
    }

    /** Call after ImGui has been rendered to capture the current overlay frame. */
    public static void captureFrame() {
        if (!ENABLED || !available || width <= 0 || height <= 0) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texSource);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
    }

    /** Blur captured frame; call before drawing menu background. */
    public static void blur() {
        if (!ENABLED || !available || texSource == 0) return;
        float radius = 8f;
        blurPass(texSource, texTempA, fboA, 1f, 0f, radius);
        blurPass(texTempA, texTempB, fboB, 0f, 1f, radius);
        blurPass(texTempB, texTempA, fboA, 1f, 0f, radius);
        blurPass(texTempA, texTempB, fboB, 0f, 1f, radius);
        blurredTexture = texTempB;
    }

    public static void drawBehindMenu(ImDrawList dl, float x, float y, float w, float h, float alpha) {
        if (!isAvailable() || blurredTexture == 0 || width <= 0 || height <= 0) return;
        // Map menu rect to framebuffer UVs (capture is full-screen, menu-only region)
        float u0 = x / width;
        float v0 = y / height;
        float u1 = (x + w) / width;
        float v1 = (y + h) / height;
        int col = UiColors.getClr(1f, 1f, 1f, alpha);
        dl.addImageRounded(blurredTexture, x, y, x + w, y + h, u0, v0, u1, v1, col, UiScale.s(16f));
    }

    private static void initIfNeeded() {
        if (initialized) return;
        initialized = true;
        try {
            String vert = loadResource("shaders/blur.vert");
            String frag = loadResource("shaders/blur.frag");
            program = linkProgram(vert, frag);
            uTexture = GL20.glGetUniformLocation(program, "uTexture");
            uDirection = GL20.glGetUniformLocation(program, "uDirection");
            uTexelSize = GL20.glGetUniformLocation(program, "uTexelSize");
            uRadius = GL20.glGetUniformLocation(program, "uRadius");

            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            float[] quad = {
                    -1f, -1f, 0f, 0f,
                    1f, -1f, 1f, 0f,
                    1f, 1f, 1f, 1f,
                    -1f, -1f, 0f, 0f,
                    1f, 1f, 1f, 1f,
                    -1f, 1f, 0f, 1f
            };
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buf = stack.mallocFloat(quad.length);
                buf.put(quad).flip();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
            }
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);
            GL30.glBindVertexArray(0);

            available = true;
        } catch (Exception e) {
            System.err.println("[Velocity] GlBackgroundBlur init failed, blur disabled: " + e.getMessage());
            available = false;
        }
    }

    private static void blurPass(int srcTex, int dstTex, int fbo, float dirX, float dirY, float radius) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(program);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, srcTex);
        GL20.glUniform1i(uTexture, 0);
        GL20.glUniform2f(uDirection, dirX, dirY);
        GL20.glUniform2f(uTexelSize, 1f / width, 1f / height);
        GL20.glUniform1f(uRadius, radius);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static int createTexture(int w, int h) {
        int tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        return tex;
    }

    private static int createFbo(int tex) {
        int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, tex, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return fbo;
    }

    private static void destroyTextures() {
        if (texSource != 0) GL11.glDeleteTextures(texSource);
        if (texTempA != 0) GL11.glDeleteTextures(texTempA);
        if (texTempB != 0) GL11.glDeleteTextures(texTempB);
        if (fboA != 0) GL30.glDeleteFramebuffers(fboA);
        if (fboB != 0) GL30.glDeleteFramebuffers(fboB);
        texSource = texTempA = texTempB = 0;
        fboA = fboB = 0;
        blurredTexture = 0;
    }

    private static String loadResource(String path) throws Exception {
        try (InputStream in = GlBackgroundBlur.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int linkProgram(String vertSrc, String fragSrc) {
        int vs = compile(GL20.GL_VERTEX_SHADER, vertSrc);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, fragSrc);
        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glBindAttribLocation(prog, 0, "Position");
        GL20.glBindAttribLocation(prog, 1, "UV");
        GL20.glLinkProgram(prog);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return prog;
    }

    private static int compile(int type, String src) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        return shader;
    }
}
