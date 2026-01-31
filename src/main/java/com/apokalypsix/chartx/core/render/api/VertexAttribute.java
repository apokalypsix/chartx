package com.apokalypsix.chartx.core.render.api;

/**
 * Describes a vertex attribute within a vertex buffer.
 *
 * <p>Vertex attributes define how vertex data is laid out in memory
 * and how it maps to shader inputs.
 */
public class VertexAttribute {

    private final String name;
    private final int components;
    private final AttributeType type;
    private final boolean normalized;
    private final int offset;

    /**
     * Creates a new vertex attribute.
     *
     * @param name the attribute name (must match shader input)
     * @param components number of components (1-4)
     * @param type the data type
     * @param normalized whether to normalize integer values to [0,1] or [-1,1]
     * @param offset byte offset within the vertex
     */
    public VertexAttribute(String name, int components, AttributeType type,
                           boolean normalized, int offset) {
        this.name = name;
        this.components = components;
        this.type = type;
        this.normalized = normalized;
        this.offset = offset;
    }

    /**
     * Creates a float attribute (most common case).
     */
    public static VertexAttribute floatAttr(String name, int components, int offset) {
        return new VertexAttribute(name, components, AttributeType.FLOAT, false, offset);
    }

    public String getName() {
        return name;
    }

    public int getComponents() {
        return components;
    }

    public AttributeType getType() {
        return type;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public int getOffset() {
        return offset;
    }

    /**
     * Returns the size in bytes of this attribute.
     */
    public int getSizeInBytes() {
        return components * type.getSizeInBytes();
    }

    /**
     * Vertex attribute data types.
     */
    public enum AttributeType {
        FLOAT(4),
        INT(4),
        UNSIGNED_INT(4),
        SHORT(2),
        UNSIGNED_SHORT(2),
        BYTE(1),
        UNSIGNED_BYTE(1);

        private final int sizeInBytes;

        AttributeType(int sizeInBytes) {
            this.sizeInBytes = sizeInBytes;
        }

        public int getSizeInBytes() {
            return sizeInBytes;
        }
    }
}
