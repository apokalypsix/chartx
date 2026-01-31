package com.apokalypsix.chartx.core.render.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describes the layout and usage of a vertex buffer.
 */
public class BufferDescriptor {

    private final int floatsPerVertex;
    private final int initialCapacity;
    private final boolean dynamic;
    private final List<VertexAttribute> attributes;

    private BufferDescriptor(Builder builder) {
        this.floatsPerVertex = builder.floatsPerVertex;
        this.initialCapacity = builder.initialCapacity;
        this.dynamic = builder.dynamic;
        this.attributes = Collections.unmodifiableList(Arrays.asList(builder.attributes));
    }

    /**
     * Creates a new buffer descriptor builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple dynamic buffer with position (2D) and color (RGBA).
     */
    public static BufferDescriptor positionColor2D(int initialCapacity) {
        return builder()
                .floatsPerVertex(6)  // x, y, r, g, b, a
                .initialCapacity(initialCapacity)
                .dynamic(true)
                .attributes(
                        VertexAttribute.floatAttr("aPosition", 2, 0),
                        VertexAttribute.floatAttr("aColor", 4, 8)
                )
                .build();
    }

    /**
     * Creates a simple dynamic buffer with position only (2D).
     */
    public static BufferDescriptor positionOnly2D(int initialCapacity) {
        return builder()
                .floatsPerVertex(2)  // x, y
                .initialCapacity(initialCapacity)
                .dynamic(true)
                .attributes(
                        VertexAttribute.floatAttr("aPosition", 2, 0)
                )
                .build();
    }

    /**
     * Creates a buffer for text rendering with position, texcoord, and color.
     */
    public static BufferDescriptor textBuffer(int initialCapacity) {
        return builder()
                .floatsPerVertex(8)  // x, y, u, v, r, g, b, a
                .initialCapacity(initialCapacity)
                .dynamic(true)
                .attributes(
                        VertexAttribute.floatAttr("aPosition", 2, 0),
                        VertexAttribute.floatAttr("aTexCoord", 2, 8),
                        VertexAttribute.floatAttr("aColor", 4, 16)
                )
                .build();
    }

    public int getFloatsPerVertex() {
        return floatsPerVertex;
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public List<VertexAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Returns the stride (bytes per vertex).
     */
    public int getStrideInBytes() {
        return floatsPerVertex * 4;
    }

    public static class Builder {
        private int floatsPerVertex = 6;
        private int initialCapacity = 1024;
        private boolean dynamic = true;
        private VertexAttribute[] attributes = new VertexAttribute[0];

        public Builder floatsPerVertex(int floatsPerVertex) {
            this.floatsPerVertex = floatsPerVertex;
            return this;
        }

        public Builder initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        public Builder dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            return this;
        }

        public Builder attributes(VertexAttribute... attributes) {
            this.attributes = attributes;
            return this;
        }

        public BufferDescriptor build() {
            return new BufferDescriptor(this);
        }
    }
}
