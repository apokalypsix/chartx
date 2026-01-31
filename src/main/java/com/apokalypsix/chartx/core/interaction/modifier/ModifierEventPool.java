package com.apokalypsix.chartx.core.interaction.modifier;

import com.apokalypsix.chartx.core.interaction.modifier.ModifierKeyEventArgs;
import com.apokalypsix.chartx.core.interaction.modifier.ModifierMouseEventArgs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Object pool for modifier event arguments to avoid GC pressure.
 *
 * <p>Mouse move events can occur at very high frequency (hundreds per second
 * during dragging). Creating new event args objects for each event would
 * cause significant GC churn. This pool reuses event args objects.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Acquire from pool
 * ModifierMouseEventArgs args = ModifierEventPool.acquireMouseArgs();
 * try {
 *     args.populate(mouseEvent, scaledX, scaledY, surface);
 *     modifiers.onMouseMoved(args);
 * } finally {
 *     ModifierEventPool.release(args);
 * }
 * }</pre>
 *
 * <p>The pool is thread-local to avoid synchronization overhead.
 */
public final class ModifierEventPool {

    /** Default pool size */
    private static final int DEFAULT_POOL_SIZE = 4;

    /** Thread-local pool for mouse event args */
    private static final ThreadLocal<Pool<ModifierMouseEventArgs>> MOUSE_POOL =
            ThreadLocal.withInitial(() -> new Pool<>(ModifierMouseEventArgs::new, DEFAULT_POOL_SIZE));

    /** Thread-local pool for key event args */
    private static final ThreadLocal<Pool<ModifierKeyEventArgs>> KEY_POOL =
            ThreadLocal.withInitial(() -> new Pool<>(ModifierKeyEventArgs::new, DEFAULT_POOL_SIZE));

    private ModifierEventPool() {
        // Utility class
    }

    /**
     * Acquires a mouse event args from the pool.
     *
     * <p>The returned object is reset and ready for use. Call
     * {@link #release(ModifierMouseEventArgs)} when done.
     *
     * @return a pooled mouse event args
     */
    public static ModifierMouseEventArgs acquireMouseArgs() {
        return MOUSE_POOL.get().acquire();
    }

    /**
     * Releases a mouse event args back to the pool.
     *
     * @param args the args to release
     */
    public static void release(ModifierMouseEventArgs args) {
        if (args != null) {
            MOUSE_POOL.get().release(args);
        }
    }

    /**
     * Acquires a key event args from the pool.
     *
     * <p>The returned object is reset and ready for use. Call
     * {@link #release(ModifierKeyEventArgs)} when done.
     *
     * @return a pooled key event args
     */
    public static ModifierKeyEventArgs acquireKeyArgs() {
        return KEY_POOL.get().acquire();
    }

    /**
     * Releases a key event args back to the pool.
     *
     * @param args the args to release
     */
    public static void release(ModifierKeyEventArgs args) {
        if (args != null) {
            KEY_POOL.get().release(args);
        }
    }

    /**
     * Clears all pools for the current thread.
     *
     * <p>This is primarily for testing and debugging.
     */
    public static void clearPools() {
        MOUSE_POOL.get().clear();
        KEY_POOL.get().clear();
    }

    /**
     * Generic object pool implementation.
     *
     * @param <T> the type of objects in the pool
     */
    private static final class Pool<T> {
        private final Deque<T> pool;
        private final Supplier<T> factory;
        private final int maxSize;
        private final Resetter<T> resetter;

        Pool(Supplier<T> factory, int maxSize) {
            this.pool = new ArrayDeque<>(maxSize);
            this.factory = factory;
            this.maxSize = maxSize;
            this.resetter = findResetter(factory);
        }

        @SuppressWarnings("unchecked")
        private Resetter<T> findResetter(Supplier<T> factory) {
            // Create one to check the type
            T sample = factory.get();
            if (sample instanceof ModifierMouseEventArgs) {
                return (Resetter<T>) (Resetter<ModifierMouseEventArgs>) ModifierMouseEventArgs::reset;
            } else if (sample instanceof ModifierKeyEventArgs) {
                return (Resetter<T>) (Resetter<ModifierKeyEventArgs>) ModifierKeyEventArgs::reset;
            }
            return obj -> {}; // No-op for unknown types
        }

        T acquire() {
            T obj = pool.pollFirst();
            if (obj == null) {
                obj = factory.get();
            }
            return obj;
        }

        void release(T obj) {
            resetter.reset(obj);
            if (pool.size() < maxSize) {
                pool.addFirst(obj);
            }
            // If pool is full, let the object be GC'd
        }

        void clear() {
            pool.clear();
        }

        @FunctionalInterface
        interface Resetter<T> {
            void reset(T obj);
        }
    }
}
