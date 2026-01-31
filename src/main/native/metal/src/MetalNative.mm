/**
 * MetalNative.mm - JNI implementation for Metal rendering backend
 *
 * This file implements all native methods declared in MetalNative.java.
 * Uses Objective-C++ to bridge between JNI and Metal API.
 */

#import <Metal/Metal.h>
#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#include <jni.h>

// ============================================================================
// Object Handle Management
// ============================================================================

// Map to store retained Metal objects, keyed by handle value
static NSMapTable<NSNumber*, id>* metalObjects = nil;
static dispatch_once_t metalObjectsOnce;

static void ensureObjectMapInitialized() {
    dispatch_once(&metalObjectsOnce, ^{
        metalObjects = [[NSMapTable alloc] initWithKeyOptions:NSMapTableStrongMemory
                                                 valueOptions:NSMapTableStrongMemory
                                                     capacity:256];
    });
}

// Stores a retained object and returns its handle
static jlong storeObject(id obj) {
    ensureObjectMapInitialized();
    if (!obj) return 0;

    // Retain the object
    CFRetain((__bridge CFTypeRef)obj);

    jlong handle = (jlong)(__bridge void*)obj;
    @synchronized(metalObjects) {
        [metalObjects setObject:obj forKey:@(handle)];
    }
    return handle;
}

// Retrieves an object by handle
static id getObject(jlong handle) {
    if (handle == 0) return nil;
    ensureObjectMapInitialized();

    @synchronized(metalObjects) {
        return [metalObjects objectForKey:@(handle)];
    }
}

// Releases an object by handle
static void releaseObject(jlong handle) {
    if (handle == 0) return;
    ensureObjectMapInitialized();

    @synchronized(metalObjects) {
        id obj = [metalObjects objectForKey:@(handle)];
        if (obj) {
            [metalObjects removeObjectForKey:@(handle)];
            CFRelease((__bridge CFTypeRef)obj);
        }
    }
}

// ============================================================================
// JNI Method Naming
// Package: com.apokalypsix.chartx.render.backend.metal
// Class: MetalNative
// ============================================================================

#ifdef __cplusplus
extern "C" {
#endif

// ============================================================================
// Device Management
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createDevice
  (JNIEnv *env, jclass clazz)
{
    @autoreleasepool {
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        if (!device) {
            return 0;
        }
        return storeObject(device);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyDevice
  (JNIEnv *env, jclass clazz, jlong deviceHandle)
{
    releaseObject(deviceHandle);
}

JNIEXPORT jboolean JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_isDeviceReady
  (JNIEnv *env, jclass clazz, jlong deviceHandle)
{
    id<MTLDevice> device = getObject(deviceHandle);
    return device != nil ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_getDeviceName
  (JNIEnv *env, jclass clazz, jlong deviceHandle)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device) {
            return env->NewStringUTF("Unknown");
        }

        NSString* name = device.name;
        return env->NewStringUTF(name ? [name UTF8String] : "Unknown");
    }
}

JNIEXPORT jint JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_getMaxTextureSize
  (JNIEnv *env, jclass clazz, jlong deviceHandle)
{
    // Modern macOS GPUs support at least 16384
    return 16384;
}

JNIEXPORT jfloat JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_getMaxLineWidth
  (JNIEnv *env, jclass clazz, jlong deviceHandle)
{
    // Metal doesn't support wide lines natively
    return 1.0f;
}

// ============================================================================
// Command Queue
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createCommandQueue
  (JNIEnv *env, jclass clazz, jlong deviceHandle)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device) return 0;

        id<MTLCommandQueue> queue = [device newCommandQueue];
        return storeObject(queue);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyCommandQueue
  (JNIEnv *env, jclass clazz, jlong queueHandle)
{
    releaseObject(queueHandle);
}

// ============================================================================
// Command Buffer and Encoding
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createCommandBuffer
  (JNIEnv *env, jclass clazz, jlong queueHandle)
{
    @autoreleasepool {
        id<MTLCommandQueue> queue = getObject(queueHandle);
        if (!queue) return 0;

        id<MTLCommandBuffer> commandBuffer = [queue commandBuffer];
        // Command buffers are autoreleased, we retain for the frame duration
        return storeObject(commandBuffer);
    }
}

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createRenderPassDescriptor
  (JNIEnv *env, jclass clazz, jlong colorTextureHandle, jfloat clearR, jfloat clearG, jfloat clearB, jfloat clearA)
{
    @autoreleasepool {
        id<MTLTexture> colorTexture = getObject(colorTextureHandle);
        if (!colorTexture) return 0;

        MTLRenderPassDescriptor* descriptor = [[MTLRenderPassDescriptor alloc] init];

        // Configure color attachment
        descriptor.colorAttachments[0].texture = colorTexture;
        descriptor.colorAttachments[0].loadAction = MTLLoadActionClear;
        descriptor.colorAttachments[0].storeAction = MTLStoreActionStore;
        descriptor.colorAttachments[0].clearColor = MTLClearColorMake(clearR, clearG, clearB, clearA);

        return storeObject(descriptor);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyRenderPassDescriptor
  (JNIEnv *env, jclass clazz, jlong descriptorHandle)
{
    releaseObject(descriptorHandle);
}

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createRenderCommandEncoder
  (JNIEnv *env, jclass clazz, jlong commandBufferHandle, jlong descriptorHandle)
{
    @autoreleasepool {
        id<MTLCommandBuffer> commandBuffer = getObject(commandBufferHandle);
        MTLRenderPassDescriptor* descriptor = getObject(descriptorHandle);

        if (!commandBuffer || !descriptor) return 0;

        id<MTLRenderCommandEncoder> encoder = [commandBuffer renderCommandEncoderWithDescriptor:descriptor];
        // Encoders are autoreleased but we need them for the frame
        return storeObject(encoder);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_endEncoding
  (JNIEnv *env, jclass clazz, jlong encoderHandle)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (encoder) {
            [encoder endEncoding];
        }
        releaseObject(encoderHandle);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_commitCommandBuffer
  (JNIEnv *env, jclass clazz, jlong commandBufferHandle)
{
    @autoreleasepool {
        id<MTLCommandBuffer> commandBuffer = getObject(commandBufferHandle);
        if (commandBuffer) {
            [commandBuffer commit];
        }
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_waitUntilCompleted
  (JNIEnv *env, jclass clazz, jlong commandBufferHandle)
{
    @autoreleasepool {
        id<MTLCommandBuffer> commandBuffer = getObject(commandBufferHandle);
        if (commandBuffer) {
            [commandBuffer waitUntilCompleted];
        }
        releaseObject(commandBufferHandle);
    }
}

// ============================================================================
// Buffer Operations
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createBuffer
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jint sizeInBytes, jint options)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device || sizeInBytes <= 0) return 0;

        // Use shared storage mode for CPU/GPU access
        MTLResourceOptions resourceOptions = MTLResourceStorageModeShared;
        if (options != 0) {
            resourceOptions = (MTLResourceOptions)options;
        }

        id<MTLBuffer> buffer = [device newBufferWithLength:sizeInBytes options:resourceOptions];
        return storeObject(buffer);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyBuffer
  (JNIEnv *env, jclass clazz, jlong bufferHandle)
{
    releaseObject(bufferHandle);
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_uploadBufferData
  (JNIEnv *env, jclass clazz, jlong bufferHandle, jfloatArray data, jint offset, jint count)
{
    @autoreleasepool {
        id<MTLBuffer> buffer = getObject(bufferHandle);
        if (!buffer || !data) return;

        jfloat* floatData = env->GetFloatArrayElements(data, NULL);
        if (!floatData) return;

        size_t copySize = count * sizeof(float);
        if (copySize <= buffer.length) {
            memcpy((char*)[buffer contents], floatData + offset, copySize);
        }

        env->ReleaseFloatArrayElements(data, floatData, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_uploadBufferDataDirect
  (JNIEnv *env, jclass clazz, jlong bufferHandle, jobject data, jint offsetBytes, jint lengthBytes)
{
    @autoreleasepool {
        id<MTLBuffer> buffer = getObject(bufferHandle);
        if (!buffer || !data) return;

        void* directBuffer = env->GetDirectBufferAddress(data);
        if (!directBuffer) return;

        if ((size_t)lengthBytes <= buffer.length) {
            memcpy([buffer contents], (char*)directBuffer + offsetBytes, lengthBytes);
        }
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setVertexBuffer
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jlong bufferHandle, jint offset, jint index)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        id<MTLBuffer> buffer = getObject(bufferHandle);

        if (encoder && buffer) {
            [encoder setVertexBuffer:buffer offset:offset atIndex:index];
        }
    }
}

// ============================================================================
// Draw Calls
// ============================================================================

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_drawPrimitives
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jint primitiveType, jint vertexStart, jint vertexCount)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder || vertexCount <= 0) return;

        [encoder drawPrimitives:(MTLPrimitiveType)primitiveType
                    vertexStart:vertexStart
                    vertexCount:vertexCount];
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_drawIndexedPrimitives
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jint primitiveType, jint indexCount,
   jint indexType, jlong indexBufferHandle, jint indexOffset)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        id<MTLBuffer> indexBuffer = getObject(indexBufferHandle);

        if (!encoder || !indexBuffer || indexCount <= 0) return;

        MTLIndexType mtlIndexType = (indexType == 0) ? MTLIndexTypeUInt16 : MTLIndexTypeUInt32;

        [encoder drawIndexedPrimitives:(MTLPrimitiveType)primitiveType
                            indexCount:indexCount
                             indexType:mtlIndexType
                           indexBuffer:indexBuffer
                     indexBufferOffset:indexOffset];
    }
}

// ============================================================================
// Shader/Library Operations
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createLibraryFromSource
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jstring mslSource)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device || !mslSource) return 0;

        const char* sourceStr = env->GetStringUTFChars(mslSource, NULL);
        if (!sourceStr) return 0;

        NSString* source = [NSString stringWithUTF8String:sourceStr];
        env->ReleaseStringUTFChars(mslSource, sourceStr);

        NSError* error = nil;
        MTLCompileOptions* options = [[MTLCompileOptions alloc] init];
        options.languageVersion = MTLLanguageVersion2_4;

        id<MTLLibrary> library = [device newLibraryWithSource:source options:options error:&error];

        if (error) {
            NSLog(@"Metal shader compilation error: %@", error.localizedDescription);
            return 0;
        }

        return storeObject(library);
    }
}

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createLibraryFromMetallib
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jbyteArray metallibData)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device || !metallibData) return 0;

        jsize length = env->GetArrayLength(metallibData);
        jbyte* bytes = env->GetByteArrayElements(metallibData, NULL);
        if (!bytes) return 0;

        NSData* data = [NSData dataWithBytes:bytes length:length];
        env->ReleaseByteArrayElements(metallibData, bytes, JNI_ABORT);

        NSError* error = nil;
        dispatch_data_t dispatchData = dispatch_data_create(data.bytes, data.length, nil, DISPATCH_DATA_DESTRUCTOR_DEFAULT);
        id<MTLLibrary> library = [device newLibraryWithData:dispatchData error:&error];

        if (error) {
            NSLog(@"Metal library loading error: %@", error.localizedDescription);
            return 0;
        }

        return storeObject(library);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyLibrary
  (JNIEnv *env, jclass clazz, jlong libraryHandle)
{
    releaseObject(libraryHandle);
}

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createFunction
  (JNIEnv *env, jclass clazz, jlong libraryHandle, jstring functionName)
{
    @autoreleasepool {
        id<MTLLibrary> library = getObject(libraryHandle);
        if (!library || !functionName) return 0;

        const char* nameStr = env->GetStringUTFChars(functionName, NULL);
        if (!nameStr) return 0;

        NSString* name = [NSString stringWithUTF8String:nameStr];
        env->ReleaseStringUTFChars(functionName, nameStr);

        id<MTLFunction> function = [library newFunctionWithName:name];
        return storeObject(function);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyFunction
  (JNIEnv *env, jclass clazz, jlong functionHandle)
{
    releaseObject(functionHandle);
}

// ============================================================================
// Pipeline State
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createPipelineState
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jlong vertexFunctionHandle, jlong fragmentFunctionHandle,
   jint pixelFormat, jint blendMode, jintArray attributeFormats, jintArray attributeOffsets, jint stride)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        id<MTLFunction> vertexFunction = getObject(vertexFunctionHandle);
        id<MTLFunction> fragmentFunction = getObject(fragmentFunctionHandle);

        if (!device || !vertexFunction || !fragmentFunction) return 0;

        MTLRenderPipelineDescriptor* descriptor = [[MTLRenderPipelineDescriptor alloc] init];
        descriptor.vertexFunction = vertexFunction;
        descriptor.fragmentFunction = fragmentFunction;
        descriptor.colorAttachments[0].pixelFormat = (MTLPixelFormat)pixelFormat;

        // Configure blending
        MTLRenderPipelineColorAttachmentDescriptor* colorAttachment = descriptor.colorAttachments[0];
        switch (blendMode) {
            case 0: // NONE
                colorAttachment.blendingEnabled = NO;
                break;
            case 1: // ALPHA
                colorAttachment.blendingEnabled = YES;
                colorAttachment.sourceRGBBlendFactor = MTLBlendFactorSourceAlpha;
                colorAttachment.destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
                colorAttachment.rgbBlendOperation = MTLBlendOperationAdd;
                colorAttachment.sourceAlphaBlendFactor = MTLBlendFactorOne;
                colorAttachment.destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
                colorAttachment.alphaBlendOperation = MTLBlendOperationAdd;
                break;
            case 2: // ADDITIVE
                colorAttachment.blendingEnabled = YES;
                colorAttachment.sourceRGBBlendFactor = MTLBlendFactorSourceAlpha;
                colorAttachment.destinationRGBBlendFactor = MTLBlendFactorOne;
                colorAttachment.rgbBlendOperation = MTLBlendOperationAdd;
                colorAttachment.sourceAlphaBlendFactor = MTLBlendFactorOne;
                colorAttachment.destinationAlphaBlendFactor = MTLBlendFactorOne;
                colorAttachment.alphaBlendOperation = MTLBlendOperationAdd;
                break;
            case 3: // MULTIPLY
                colorAttachment.blendingEnabled = YES;
                colorAttachment.sourceRGBBlendFactor = MTLBlendFactorDestinationColor;
                colorAttachment.destinationRGBBlendFactor = MTLBlendFactorZero;
                colorAttachment.rgbBlendOperation = MTLBlendOperationAdd;
                colorAttachment.sourceAlphaBlendFactor = MTLBlendFactorDestinationAlpha;
                colorAttachment.destinationAlphaBlendFactor = MTLBlendFactorZero;
                colorAttachment.alphaBlendOperation = MTLBlendOperationAdd;
                break;
            default:
                colorAttachment.blendingEnabled = NO;
                break;
        }

        // Configure vertex descriptor
        if (attributeFormats && attributeOffsets) {
            MTLVertexDescriptor* vertexDescriptor = [[MTLVertexDescriptor alloc] init];

            jint* formats = env->GetIntArrayElements(attributeFormats, NULL);
            jint* offsets = env->GetIntArrayElements(attributeOffsets, NULL);
            jsize attrCount = env->GetArrayLength(attributeFormats);

            for (int i = 0; i < attrCount; i++) {
                vertexDescriptor.attributes[i].format = (MTLVertexFormat)formats[i];
                vertexDescriptor.attributes[i].offset = offsets[i];
                vertexDescriptor.attributes[i].bufferIndex = 0;
            }
            vertexDescriptor.layouts[0].stride = stride;
            vertexDescriptor.layouts[0].stepFunction = MTLVertexStepFunctionPerVertex;

            env->ReleaseIntArrayElements(attributeFormats, formats, JNI_ABORT);
            env->ReleaseIntArrayElements(attributeOffsets, offsets, JNI_ABORT);

            descriptor.vertexDescriptor = vertexDescriptor;
        }

        NSError* error = nil;
        id<MTLRenderPipelineState> pipelineState = [device newRenderPipelineStateWithDescriptor:descriptor error:&error];

        if (error) {
            NSLog(@"Metal pipeline creation error: %@", error.localizedDescription);
            return 0;
        }

        return storeObject(pipelineState);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyPipelineState
  (JNIEnv *env, jclass clazz, jlong pipelineStateHandle)
{
    releaseObject(pipelineStateHandle);
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setRenderPipelineState
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jlong pipelineStateHandle)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        id<MTLRenderPipelineState> pipelineState = getObject(pipelineStateHandle);

        if (encoder && pipelineState) {
            [encoder setRenderPipelineState:pipelineState];
        }
    }
}

// ============================================================================
// Uniforms
// ============================================================================

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setVertexBytes
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jfloatArray data, jint count, jint index)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder || !data) return;

        jfloat* floatData = env->GetFloatArrayElements(data, NULL);
        if (!floatData) return;

        [encoder setVertexBytes:floatData length:count * sizeof(float) atIndex:index];

        env->ReleaseFloatArrayElements(data, floatData, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setFragmentBytes
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jfloatArray data, jint count, jint index)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder || !data) return;

        jfloat* floatData = env->GetFloatArrayElements(data, NULL);
        if (!floatData) return;

        [encoder setFragmentBytes:floatData length:count * sizeof(float) atIndex:index];

        env->ReleaseFloatArrayElements(data, floatData, JNI_ABORT);
    }
}

// ============================================================================
// Texture Operations
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createTexture
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jint width, jint height, jint format, jint usage)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device || width <= 0 || height <= 0) return 0;

        MTLTextureDescriptor* descriptor = [[MTLTextureDescriptor alloc] init];
        descriptor.textureType = MTLTextureType2D;
        descriptor.pixelFormat = (MTLPixelFormat)format;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.usage = (MTLTextureUsage)usage;
        descriptor.storageMode = MTLStorageModeShared;

        id<MTLTexture> texture = [device newTextureWithDescriptor:descriptor];
        return storeObject(texture);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroyTexture
  (JNIEnv *env, jclass clazz, jlong textureHandle)
{
    releaseObject(textureHandle);
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_uploadTextureData
  (JNIEnv *env, jclass clazz, jlong textureHandle, jbyteArray data, jint width, jint height, jint bytesPerRow)
{
    @autoreleasepool {
        id<MTLTexture> texture = getObject(textureHandle);
        if (!texture || !data) return;

        jbyte* bytes = env->GetByteArrayElements(data, NULL);
        if (!bytes) return;

        MTLRegion region = MTLRegionMake2D(0, 0, width, height);
        [texture replaceRegion:region mipmapLevel:0 withBytes:bytes bytesPerRow:bytesPerRow];

        env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setFragmentTexture
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jlong textureHandle, jint index)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        id<MTLTexture> texture = getObject(textureHandle);

        if (encoder && texture) {
            [encoder setFragmentTexture:texture atIndex:index];
        }
    }
}

// ============================================================================
// Sampler Operations
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createSampler
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jint minFilter, jint magFilter, jint addressMode)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device) return 0;

        MTLSamplerDescriptor* descriptor = [[MTLSamplerDescriptor alloc] init];
        descriptor.minFilter = (minFilter == 0) ? MTLSamplerMinMagFilterNearest : MTLSamplerMinMagFilterLinear;
        descriptor.magFilter = (magFilter == 0) ? MTLSamplerMinMagFilterNearest : MTLSamplerMinMagFilterLinear;

        MTLSamplerAddressMode mtlAddressMode;
        switch (addressMode) {
            case 0: mtlAddressMode = MTLSamplerAddressModeClampToEdge; break;
            case 1: mtlAddressMode = MTLSamplerAddressModeRepeat; break;
            case 2: mtlAddressMode = MTLSamplerAddressModeMirrorRepeat; break;
            default: mtlAddressMode = MTLSamplerAddressModeClampToEdge; break;
        }
        descriptor.sAddressMode = mtlAddressMode;
        descriptor.tAddressMode = mtlAddressMode;

        id<MTLSamplerState> sampler = [device newSamplerStateWithDescriptor:descriptor];
        return storeObject(sampler);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_destroySampler
  (JNIEnv *env, jclass clazz, jlong samplerHandle)
{
    releaseObject(samplerHandle);
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setFragmentSamplerState
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jlong samplerHandle, jint index)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        id<MTLSamplerState> sampler = getObject(samplerHandle);

        if (encoder && sampler) {
            [encoder setFragmentSamplerState:sampler atIndex:index];
        }
    }
}

// ============================================================================
// Framebuffer / Pixel Readback
// ============================================================================

JNIEXPORT jlong JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_createOffscreenTexture
  (JNIEnv *env, jclass clazz, jlong deviceHandle, jint width, jint height)
{
    @autoreleasepool {
        id<MTLDevice> device = getObject(deviceHandle);
        if (!device || width <= 0 || height <= 0) return 0;

        MTLTextureDescriptor* descriptor = [[MTLTextureDescriptor alloc] init];
        descriptor.textureType = MTLTextureType2D;
        descriptor.pixelFormat = MTLPixelFormatBGRA8Unorm;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
        descriptor.storageMode = MTLStorageModeShared; // For CPU readback

        id<MTLTexture> texture = [device newTextureWithDescriptor:descriptor];
        return storeObject(texture);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_readPixels
  (JNIEnv *env, jclass clazz, jlong textureHandle, jintArray pixels, jint width, jint height)
{
    @autoreleasepool {
        id<MTLTexture> texture = getObject(textureHandle);
        if (!texture || !pixels) return;

        jint* pixelData = env->GetIntArrayElements(pixels, NULL);
        if (!pixelData) return;

        NSUInteger bytesPerRow = width * 4;
        MTLRegion region = MTLRegionMake2D(0, 0, width, height);

        [texture getBytes:pixelData bytesPerRow:bytesPerRow fromRegion:region mipmapLevel:0];

        // Convert BGRA to ARGB for Java BufferedImage (TYPE_INT_ARGB)
        for (int i = 0; i < width * height; i++) {
            uint32_t pixel = pixelData[i];
            uint8_t b = (pixel >> 0) & 0xFF;
            uint8_t g = (pixel >> 8) & 0xFF;
            uint8_t r = (pixel >> 16) & 0xFF;
            uint8_t a = (pixel >> 24) & 0xFF;
            pixelData[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        env->ReleaseIntArrayElements(pixels, pixelData, 0);
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_synchronizeTexture
  (JNIEnv *env, jclass clazz, jlong commandBufferHandle, jlong textureHandle)
{
    @autoreleasepool {
        id<MTLCommandBuffer> commandBuffer = getObject(commandBufferHandle);
        id<MTLTexture> texture = getObject(textureHandle);

        if (!commandBuffer || !texture) return;

        // Create a blit encoder to synchronize the texture
        id<MTLBlitCommandEncoder> blitEncoder = [commandBuffer blitCommandEncoder];
        if (blitEncoder) {
            // On shared storage mode, synchronization is automatic
            // This is a no-op but kept for API completeness
            [blitEncoder endEncoding];
        }
    }
}

// ============================================================================
// Render State
// ============================================================================

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setViewport
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jdouble x, jdouble y, jdouble width, jdouble height,
   jdouble znear, jdouble zfar)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder) return;

        MTLViewport viewport;
        viewport.originX = x;
        viewport.originY = y;
        viewport.width = width;
        viewport.height = height;
        viewport.znear = znear;
        viewport.zfar = zfar;

        [encoder setViewport:viewport];
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setScissorRect
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jint x, jint y, jint width, jint height)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder) return;

        MTLScissorRect scissor;
        scissor.x = x;
        scissor.y = y;
        scissor.width = width;
        scissor.height = height;

        [encoder setScissorRect:scissor];
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setCullMode
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jint cullMode)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder) return;

        MTLCullMode mode;
        switch (cullMode) {
            case 0: mode = MTLCullModeNone; break;
            case 1: mode = MTLCullModeFront; break;
            case 2: mode = MTLCullModeBack; break;
            default: mode = MTLCullModeNone; break;
        }

        [encoder setCullMode:mode];
    }
}

JNIEXPORT void JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_setFrontFacing
  (JNIEnv *env, jclass clazz, jlong encoderHandle, jint winding)
{
    @autoreleasepool {
        id<MTLRenderCommandEncoder> encoder = getObject(encoderHandle);
        if (!encoder) return;

        MTLWinding mtlWinding = (winding == 0) ? MTLWindingClockwise : MTLWindingCounterClockwise;
        [encoder setFrontFacingWinding:mtlWinding];
    }
}

// ============================================================================
// Utility
// ============================================================================

JNIEXPORT jboolean JNICALL Java_com_apokalypsix_chartx_render_backend_metal_MetalNative_isMetalSupported
  (JNIEnv *env, jclass clazz)
{
    @autoreleasepool {
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        return device != nil ? JNI_TRUE : JNI_FALSE;
    }
}

#ifdef __cplusplus
}
#endif
