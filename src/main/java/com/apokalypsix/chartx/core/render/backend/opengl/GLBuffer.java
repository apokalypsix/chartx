package com.apokalypsix.chartx.core.render.backend.opengl;

import com.apokalypsix.chartx.core.render.api.Buffer;
import com.apokalypsix.chartx.core.render.api.BufferDescriptor;
import com.apokalypsix.chartx.core.render.api.DrawMode;
import com.apokalypsix.chartx.core.render.api.VertexAttribute;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * OpenGL implementation of the Buffer interface.
 *
 * <p>Wraps VBO creation, data upload, and drawing using JOGL.
 */
public class GLBuffer implements Buffer {

    private final GLRenderDevice device;
    private final BufferDescriptor descriptor;

    private int vboId = 0;
    private int capacity;
    private int vertexCount = 0;
    private boolean initialized = false;

    /**
     * Creates a new GLBuffer.
     *
     * @param device the render device
     * @param descriptor the buffer descriptor
     */
    public GLBuffer(GLRenderDevice device, BufferDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
        this.capacity = descriptor.getInitialCapacity();

        initialize();
    }

    private void initialize() {
        GL2ES2 gl = device.getGL();

        int[] vboIds = new int[1];
        gl.glGenBuffers(1, vboIds, 0);
        vboId = vboIds[0];

        int usageHint = descriptor.isDynamic() ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW;

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, null, usageHint);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        initialized = true;
    }

    @Override
    public void upload(float[] data, int offset, int count) {
        if (!initialized) {
            throw new IllegalStateException("Buffer not initialized");
        }

        GL2ES2 gl = device.getGL();

        // Resize if necessary
        if (count > capacity) {
            resize(Math.max(count, (int) (capacity * 1.5f)));
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0, (long) count * Float.BYTES,
                FloatBuffer.wrap(data, offset, count));
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        vertexCount = count / descriptor.getFloatsPerVertex();
    }

    @Override
    public void upload(FloatBuffer data, int offset) {
        if (!initialized) {
            throw new IllegalStateException("Buffer not initialized");
        }

        GL2ES2 gl = device.getGL();
        int count = data.remaining();

        // Resize if necessary
        if (count > capacity) {
            resize(Math.max(count, (int) (capacity * 1.5f)));
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferSubData(GL.GL_ARRAY_BUFFER, (long) offset * Float.BYTES,
                (long) count * Float.BYTES, data);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        vertexCount = count / descriptor.getFloatsPerVertex();
    }

    private void resize(int newCapacity) {
        GL2ES2 gl = device.getGL();
        int usageHint = descriptor.isDynamic() ? GL.GL_DYNAMIC_DRAW : GL.GL_STATIC_DRAW;

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) newCapacity * Float.BYTES, null, usageHint);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        capacity = newCapacity;
    }

    @Override
    public void bind() {
        GL2ES2 gl = device.getGL();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);

        // Setup vertex attributes
        List<VertexAttribute> attributes = descriptor.getAttributes();
        int stride = descriptor.getStrideInBytes();

        for (int i = 0; i < attributes.size(); i++) {
            VertexAttribute attr = attributes.get(i);
            gl.glEnableVertexAttribArray(i);
            gl.glVertexAttribPointer(i, attr.getComponents(),
                    toGLType(attr.getType()), attr.isNormalized(),
                    stride, attr.getOffset());
        }
    }

    @Override
    public void unbind() {
        GL2ES2 gl = device.getGL();

        List<VertexAttribute> attributes = descriptor.getAttributes();
        for (int i = 0; i < attributes.size(); i++) {
            gl.glDisableVertexAttribArray(i);
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void draw(DrawMode mode) {
        draw(mode, 0, vertexCount);
    }

    @Override
    public void draw(DrawMode mode, int first, int count) {
        GL2ES2 gl = device.getGL();

        bind();
        gl.glDrawArrays(toGLMode(mode), first, count);
        unbind();
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public void setVertexCount(int count) {
        this.vertexCount = count;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void dispose() {
        if (vboId != 0) {
            device.getGL().glDeleteBuffers(1, new int[]{vboId}, 0);
            vboId = 0;
        }
        initialized = false;
        capacity = 0;
        vertexCount = 0;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the OpenGL VBO ID.
     */
    public int getVboId() {
        return vboId;
    }

    private static int toGLMode(DrawMode mode) {
        return switch (mode) {
            case TRIANGLES -> GL.GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL.GL_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> GL.GL_TRIANGLE_FAN;
            case LINES -> GL.GL_LINES;
            case LINE_STRIP -> GL.GL_LINE_STRIP;
            case LINE_LOOP -> GL.GL_LINE_LOOP;
            case POINTS -> GL.GL_POINTS;
        };
    }

    private static int toGLType(VertexAttribute.AttributeType type) {
        return switch (type) {
            case FLOAT -> GL.GL_FLOAT;
            case INT -> GL2ES2.GL_INT;
            case UNSIGNED_INT -> GL.GL_UNSIGNED_INT;
            case SHORT -> GL.GL_SHORT;
            case UNSIGNED_SHORT -> GL.GL_UNSIGNED_SHORT;
            case BYTE -> GL.GL_BYTE;
            case UNSIGNED_BYTE -> GL.GL_UNSIGNED_BYTE;
        };
    }
}
