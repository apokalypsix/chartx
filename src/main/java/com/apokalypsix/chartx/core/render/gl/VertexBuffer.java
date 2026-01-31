package com.apokalypsix.chartx.core.render.gl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for OpenGL Vertex Buffer Object (VBO).
 *
 * <p>This class manages the lifecycle of GPU buffers and provides a simple API
 * for uploading and drawing vertex data. Compatible with GL2ES2 (no VAO required).
 */
public class VertexBuffer {

    private int vboId = 0;
    private int capacity;          // Capacity in floats
    private int vertexCount;       // Number of vertices
    private int floatsPerVertex;   // Number of floats per vertex (e.g., 2 for x,y or 6 for x,y,r,g,b,a)
    private boolean dynamic;
    private boolean initialized = false;

    // Store attribute configurations for setup before draw
    private final List<AttributeConfig> attributeConfigs = new ArrayList<>();

    private record AttributeConfig(int index, int size, int stride, int offset) {}

    /**
     * Creates a new vertex buffer.
     *
     * @param floatsPerVertex number of float components per vertex (e.g., 2 for position only)
     * @param dynamic true for frequently updated buffers, false for static geometry
     */
    public VertexBuffer(int floatsPerVertex, boolean dynamic) {
        this.floatsPerVertex = floatsPerVertex;
        this.dynamic = dynamic;
        this.capacity = 0;
        this.vertexCount = 0;
    }

    /**
     * Initializes the buffer with the specified capacity.
     * Must be called on the GL thread.
     *
     * @param gl the GL context
     * @param floatCapacity initial capacity in floats
     */
    public void initialize(GL2ES2 gl, int floatCapacity) {
        if (initialized) {
            dispose(gl);
        }

        // Generate VBO
        int[] vboIds = new int[1];
        gl.glGenBuffers(1, vboIds, 0);
        vboId = vboIds[0];

        // Allocate buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) floatCapacity * Float.BYTES, null,
                dynamic ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        capacity = floatCapacity;
        initialized = true;
    }

    /**
     * Uploads vertex data to the buffer.
     *
     * @param gl the GL context
     * @param data the vertex data
     */
    public void upload(GL2ES2 gl, FloatBuffer data) {
        if (!initialized) {
            throw new IllegalStateException("Buffer not initialized");
        }

        int requiredFloats = data.remaining();

        // Resize if necessary
        if (requiredFloats > capacity) {
            int newCapacity = Math.max(requiredFloats, (int) (capacity * 1.5f));
            resize(gl, newCapacity);
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, (long) requiredFloats * Float.BYTES, data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        vertexCount = requiredFloats / floatsPerVertex;
    }

    /**
     * Uploads vertex data to the buffer from a float array.
     *
     * @param gl the GL context
     * @param data the vertex data array
     * @param offset starting offset in the array
     * @param length number of floats to upload
     */
    public void upload(GL2ES2 gl, float[] data, int offset, int length) {
        if (!initialized) {
            throw new IllegalStateException("Buffer not initialized");
        }

        // Resize if necessary
        if (length > capacity) {
            int newCapacity = Math.max(length, (int) (capacity * 1.5f));
            resize(gl, newCapacity);
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, (long) length * Float.BYTES,
                FloatBuffer.wrap(data, offset, length));
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        vertexCount = length / floatsPerVertex;
    }

    /**
     * Resizes the buffer to the new capacity.
     */
    private void resize(GL2ES2 gl, int newCapacity) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) newCapacity * Float.BYTES, null,
                dynamic ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW);
        capacity = newCapacity;
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Binds the VBO and enables configured vertex attributes.
     */
    public void bind(GL2ES2 gl) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        for (AttributeConfig config : attributeConfigs) {
            gl.glEnableVertexAttribArray(config.index);
            gl.glVertexAttribPointer(config.index, config.size, GL.GL_FLOAT, false,
                    config.stride, config.offset);
        }
    }

    /**
     * Unbinds the VBO and disables vertex attributes.
     */
    public void unbind(GL2ES2 gl) {
        for (AttributeConfig config : attributeConfigs) {
            gl.glDisableVertexAttribArray(config.index);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Configures a vertex attribute. The configuration is stored and applied
     * when bind() is called.
     *
     * @param gl the GL context (not used but kept for API compatibility)
     * @param attributeIndex shader attribute index
     * @param size number of components (1-4)
     * @param stride stride in bytes (0 for tightly packed)
     * @param offset offset in bytes
     */
    public void configureAttribute(GL2ES2 gl, int attributeIndex, int size, int stride, int offset) {
        // Remove existing config for this attribute if present
        attributeConfigs.removeIf(c -> c.index == attributeIndex);
        attributeConfigs.add(new AttributeConfig(attributeIndex, size, stride, offset));
    }

    /**
     * Draws the buffer contents using the specified primitive mode.
     *
     * @param gl the GL context
     * @param mode primitive mode (GL_TRIANGLES, GL_LINES, etc.)
     */
    public void draw(GL2ES2 gl, int mode) {
        draw(gl, mode, 0, vertexCount);
    }

    /**
     * Draws a range of vertices.
     *
     * @param gl the GL context
     * @param mode primitive mode
     * @param first first vertex index
     * @param count number of vertices to draw
     */
    public void draw(GL2ES2 gl, int mode, int first, int count) {
        bind(gl);
        gl.glDrawArrays(mode, first, count);
        unbind(gl);
    }

    /**
     * Returns the number of vertices currently in the buffer.
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Returns the capacity in floats.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns true if the buffer has been initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Releases GPU resources. Must be called on the GL thread.
     */
    public void dispose(GL2ES2 gl) {
        if (vboId != 0) {
            gl.glDeleteBuffers(1, new int[]{vboId}, 0);
            vboId = 0;
        }
        initialized = false;
        capacity = 0;
        vertexCount = 0;
        attributeConfigs.clear();
    }
}
