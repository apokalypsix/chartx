// DX12Native.cpp - JNI implementation for DirectX 12 backend
// This file implements all native methods declared in DX12Native.java

#include "DX12Common.h"
#include <cstring>

// JNI class path
#define JNI_CLASS "com/edgefound/chartx/render/backend/dx12/DX12Native"

// Forward declarations
static ID3D12RootSignature* CreateRootSignature(ID3D12Device* device);
static ID3D12DescriptorHeap* CreateRTVHeap(ID3D12Device* device, UINT numDescriptors);
static ID3D12DescriptorHeap* CreateSRVHeap(ID3D12Device* device, UINT numDescriptors);

extern "C" {

// =============================================================================
// Availability check
// =============================================================================

JNIEXPORT jboolean JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_isAvailable
  (JNIEnv* env, jclass clazz) {
    // Try to create a DX12 device to check availability
    ComPtr<IDXGIFactory4> factory;
    if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) {
        return JNI_FALSE;
    }

    ComPtr<IDXGIAdapter1> adapter;
    for (UINT i = 0; factory->EnumAdapters1(i, &adapter) != DXGI_ERROR_NOT_FOUND; i++) {
        DXGI_ADAPTER_DESC1 desc;
        adapter->GetDesc1(&desc);
        if (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) continue;

        ComPtr<ID3D12Device> device;
        if (SUCCEEDED(D3D12CreateDevice(adapter.Get(), D3D_FEATURE_LEVEL_12_0, IID_PPV_ARGS(&device)))) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

// =============================================================================
// Device management
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createDevice
  (JNIEnv* env, jclass clazz) {

#ifdef _DEBUG
    // Enable debug layer in debug builds
    ComPtr<ID3D12Debug> debugController;
    if (SUCCEEDED(D3D12GetDebugInterface(IID_PPV_ARGS(&debugController)))) {
        debugController->EnableDebugLayer();
    }
#endif

    ComPtr<IDXGIFactory4> factory;
    if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) {
        return 0;
    }

    ComPtr<IDXGIAdapter1> adapter;
    for (UINT i = 0; factory->EnumAdapters1(i, &adapter) != DXGI_ERROR_NOT_FOUND; i++) {
        DXGI_ADAPTER_DESC1 desc;
        adapter->GetDesc1(&desc);
        if (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) continue;

        ID3D12Device* device = nullptr;
        if (SUCCEEDED(D3D12CreateDevice(adapter.Get(), D3D_FEATURE_LEVEL_12_0, IID_PPV_ARGS(&device)))) {
            return STORE_HANDLE(device);
        }
    }
    return 0;
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyDevice
  (JNIEnv* env, jclass clazz, jlong device) {
    RELEASE_HANDLE(device);
}

// =============================================================================
// Command queue
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createCommandQueue
  (JNIEnv* env, jclass clazz, jlong deviceHandle) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    D3D12_COMMAND_QUEUE_DESC desc = {};
    desc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    desc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
    desc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
    desc.NodeMask = 0;

    ID3D12CommandQueue* queue = nullptr;
    if (FAILED(device->CreateCommandQueue(&desc, IID_PPV_ARGS(&queue)))) {
        return 0;
    }
    return STORE_HANDLE(queue);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyCommandQueue
  (JNIEnv* env, jclass clazz, jlong queue) {
    RELEASE_HANDLE(queue);
}

// =============================================================================
// Command allocator
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createCommandAllocator
  (JNIEnv* env, jclass clazz, jlong deviceHandle) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    ID3D12CommandAllocator* allocator = nullptr;
    if (FAILED(device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&allocator)))) {
        return 0;
    }
    return STORE_HANDLE(allocator);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyCommandAllocator
  (JNIEnv* env, jclass clazz, jlong allocator) {
    RELEASE_HANDLE(allocator);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_resetCommandAllocator
  (JNIEnv* env, jclass clazz, jlong allocatorHandle) {
    ID3D12CommandAllocator* allocator = GET_HANDLE(ID3D12CommandAllocator, allocatorHandle);
    if (allocator) {
        allocator->Reset();
    }
}

// =============================================================================
// Command list
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createCommandList
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jlong allocatorHandle) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    ID3D12CommandAllocator* allocator = GET_HANDLE(ID3D12CommandAllocator, allocatorHandle);
    if (!device || !allocator) return 0;

    ID3D12GraphicsCommandList* commandList = nullptr;
    if (FAILED(device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, allocator, nullptr, IID_PPV_ARGS(&commandList)))) {
        return 0;
    }
    // Close it immediately - will be reset when needed
    commandList->Close();
    return STORE_HANDLE(commandList);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyCommandList
  (JNIEnv* env, jclass clazz, jlong commandList) {
    RELEASE_HANDLE(commandList);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_resetCommandList
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong allocatorHandle, jlong pipelineStateHandle) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12CommandAllocator* allocator = GET_HANDLE(ID3D12CommandAllocator, allocatorHandle);
    ID3D12PipelineState* pso = pipelineStateHandle ? GET_HANDLE(ID3D12PipelineState, pipelineStateHandle) : nullptr;

    if (commandList && allocator) {
        commandList->Reset(allocator, pso);
    }
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_closeCommandList
  (JNIEnv* env, jclass clazz, jlong commandListHandle) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    if (commandList) {
        commandList->Close();
    }
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_executeCommandList
  (JNIEnv* env, jclass clazz, jlong queueHandle, jlong commandListHandle) {
    ID3D12CommandQueue* queue = GET_HANDLE(ID3D12CommandQueue, queueHandle);
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);

    if (queue && commandList) {
        ID3D12CommandList* lists[] = { commandList };
        queue->ExecuteCommandLists(1, lists);
    }
}

// =============================================================================
// Fence (synchronization)
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createFence
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jlong initialValue) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    ID3D12Fence* fence = nullptr;
    if (FAILED(device->CreateFence(initialValue, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence)))) {
        return 0;
    }
    return STORE_HANDLE(fence);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyFence
  (JNIEnv* env, jclass clazz, jlong fence) {
    RELEASE_HANDLE(fence);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_signalFence
  (JNIEnv* env, jclass clazz, jlong queueHandle, jlong fenceHandle, jlong value) {
    ID3D12CommandQueue* queue = GET_HANDLE(ID3D12CommandQueue, queueHandle);
    ID3D12Fence* fence = GET_HANDLE(ID3D12Fence, fenceHandle);

    if (queue && fence) {
        queue->Signal(fence, value);
    }
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_waitForFence
  (JNIEnv* env, jclass clazz, jlong fenceHandle, jlong value) {
    ID3D12Fence* fence = GET_HANDLE(ID3D12Fence, fenceHandle);
    if (!fence) return;

    if (fence->GetCompletedValue() < value) {
        HANDLE event = CreateEvent(nullptr, FALSE, FALSE, nullptr);
        if (event) {
            fence->SetEventOnCompletion(value, event);
            WaitForSingleObject(event, INFINITE);
            CloseHandle(event);
        }
    }
}

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_getFenceCompletedValue
  (JNIEnv* env, jclass clazz, jlong fenceHandle) {
    ID3D12Fence* fence = GET_HANDLE(ID3D12Fence, fenceHandle);
    return fence ? fence->GetCompletedValue() : 0;
}

// =============================================================================
// Root signature
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createRootSignature
  (JNIEnv* env, jclass clazz, jlong deviceHandle) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    ID3D12RootSignature* rootSig = CreateRootSignature(device);
    return STORE_HANDLE(rootSig);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyRootSignature
  (JNIEnv* env, jclass clazz, jlong rootSig) {
    RELEASE_HANDLE(rootSig);
}

static ID3D12RootSignature* CreateRootSignature(ID3D12Device* device) {
    // Root parameters:
    // [0] 32-bit constants: mat4 (16 floats) + vec4 (4 floats) = 20 floats
    // [1] Descriptor table for SRV (texture)
    // [2] Static sampler

    D3D12_ROOT_PARAMETER rootParams[2] = {};

    // Root constants for uniforms
    rootParams[0].ParameterType = D3D12_ROOT_PARAMETER_TYPE_32BIT_CONSTANTS;
    rootParams[0].Constants.ShaderRegister = 0;
    rootParams[0].Constants.RegisterSpace = 0;
    rootParams[0].Constants.Num32BitValues = 20; // mat4 + vec4
    rootParams[0].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    // Descriptor table for texture SRV
    D3D12_DESCRIPTOR_RANGE srvRange = {};
    srvRange.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    srvRange.NumDescriptors = 1;
    srvRange.BaseShaderRegister = 0;
    srvRange.RegisterSpace = 0;
    srvRange.OffsetInDescriptorsFromTableStart = D3D12_DESCRIPTOR_RANGE_OFFSET_APPEND;

    rootParams[1].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    rootParams[1].DescriptorTable.NumDescriptorRanges = 1;
    rootParams[1].DescriptorTable.pDescriptorRanges = &srvRange;
    rootParams[1].ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

    // Static sampler
    D3D12_STATIC_SAMPLER_DESC sampler = {};
    sampler.Filter = D3D12_FILTER_MIN_MAG_MIP_LINEAR;
    sampler.AddressU = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    sampler.AddressV = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    sampler.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    sampler.MipLODBias = 0;
    sampler.MaxAnisotropy = 1;
    sampler.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
    sampler.BorderColor = D3D12_STATIC_BORDER_COLOR_TRANSPARENT_BLACK;
    sampler.MinLOD = 0.0f;
    sampler.MaxLOD = D3D12_FLOAT32_MAX;
    sampler.ShaderRegister = 0;
    sampler.RegisterSpace = 0;
    sampler.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

    D3D12_ROOT_SIGNATURE_DESC rootSigDesc = {};
    rootSigDesc.NumParameters = 2;
    rootSigDesc.pParameters = rootParams;
    rootSigDesc.NumStaticSamplers = 1;
    rootSigDesc.pStaticSamplers = &sampler;
    rootSigDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;

    ComPtr<ID3DBlob> signature;
    ComPtr<ID3DBlob> error;
    if (FAILED(D3D12SerializeRootSignature(&rootSigDesc, D3D_ROOT_SIGNATURE_VERSION_1, &signature, &error))) {
        return nullptr;
    }

    ID3D12RootSignature* rootSig = nullptr;
    device->CreateRootSignature(0, signature->GetBufferPointer(), signature->GetBufferSize(), IID_PPV_ARGS(&rootSig));
    return rootSig;
}

// =============================================================================
// RTV Heap (render target views)
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createRTVHeap
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jint numDescriptors) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    ID3D12DescriptorHeap* heap = CreateRTVHeap(device, numDescriptors);
    return STORE_HANDLE(heap);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyDescriptorHeap
  (JNIEnv* env, jclass clazz, jlong heap) {
    RELEASE_HANDLE(heap);
}

static ID3D12DescriptorHeap* CreateRTVHeap(ID3D12Device* device, UINT numDescriptors) {
    D3D12_DESCRIPTOR_HEAP_DESC desc = {};
    desc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    desc.NumDescriptors = numDescriptors;
    desc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;

    ID3D12DescriptorHeap* heap = nullptr;
    device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&heap));
    return heap;
}

// =============================================================================
// SRV Heap (shader resource views - for textures)
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createSRVHeap
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jint numDescriptors) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    ID3D12DescriptorHeap* heap = CreateSRVHeap(device, numDescriptors);
    return STORE_HANDLE(heap);
}

static ID3D12DescriptorHeap* CreateSRVHeap(ID3D12Device* device, UINT numDescriptors) {
    D3D12_DESCRIPTOR_HEAP_DESC desc = {};
    desc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    desc.NumDescriptors = numDescriptors;
    desc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;

    ID3D12DescriptorHeap* heap = nullptr;
    device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&heap));
    return heap;
}

// =============================================================================
// Render target
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createRenderTarget
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jint width, jint height) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    D3D12_HEAP_PROPERTIES heapProps = {};
    heapProps.Type = D3D12_HEAP_TYPE_DEFAULT;

    D3D12_RESOURCE_DESC desc = {};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = width;
    desc.Height = height;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.Flags = D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;

    D3D12_CLEAR_VALUE clearValue = {};
    clearValue.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    clearValue.Color[0] = 0.0f;
    clearValue.Color[1] = 0.0f;
    clearValue.Color[2] = 0.0f;
    clearValue.Color[3] = 1.0f;

    ID3D12Resource* renderTarget = nullptr;
    if (FAILED(device->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE, &desc,
            D3D12_RESOURCE_STATE_RENDER_TARGET, &clearValue, IID_PPV_ARGS(&renderTarget)))) {
        return 0;
    }
    return STORE_HANDLE(renderTarget);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyResource
  (JNIEnv* env, jclass clazz, jlong resource) {
    RELEASE_HANDLE(resource);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createRTV
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jlong resourceHandle, jlong rtvHeapHandle, jint index) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    ID3D12Resource* resource = GET_HANDLE(ID3D12Resource, resourceHandle);
    ID3D12DescriptorHeap* heap = GET_HANDLE(ID3D12DescriptorHeap, rtvHeapHandle);

    if (!device || !resource || !heap) return;

    UINT rtvDescriptorSize = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    D3D12_CPU_DESCRIPTOR_HANDLE handle = heap->GetCPUDescriptorHandleForHeapStart();
    handle.ptr += index * rtvDescriptorSize;

    device->CreateRenderTargetView(resource, nullptr, handle);
}

// =============================================================================
// Readback buffer (for copying render target to CPU)
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createReadbackBuffer
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jlong size) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    D3D12_HEAP_PROPERTIES heapProps = {};
    heapProps.Type = D3D12_HEAP_TYPE_READBACK;

    D3D12_RESOURCE_DESC desc = {};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = size;
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = DXGI_FORMAT_UNKNOWN;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;

    ID3D12Resource* buffer = nullptr;
    if (FAILED(device->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE, &desc,
            D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&buffer)))) {
        return 0;
    }
    return STORE_HANDLE(buffer);
}

// =============================================================================
// Resource barriers
// =============================================================================

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_resourceBarrier
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong resourceHandle, jint stateBefore, jint stateAfter) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12Resource* resource = GET_HANDLE(ID3D12Resource, resourceHandle);

    if (!commandList || !resource) return;

    D3D12_RESOURCE_BARRIER barrier = {};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrier.Flags = D3D12_RESOURCE_BARRIER_FLAG_NONE;
    barrier.Transition.pResource = resource;
    barrier.Transition.StateBefore = static_cast<D3D12_RESOURCE_STATES>(stateBefore);
    barrier.Transition.StateAfter = static_cast<D3D12_RESOURCE_STATES>(stateAfter);
    barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;

    commandList->ResourceBarrier(1, &barrier);
}

// =============================================================================
// Render commands
// =============================================================================

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setRenderTarget
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong deviceHandle, jlong rtvHeapHandle, jint index) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    ID3D12DescriptorHeap* heap = GET_HANDLE(ID3D12DescriptorHeap, rtvHeapHandle);

    if (!commandList || !device || !heap) return;

    UINT rtvDescriptorSize = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    D3D12_CPU_DESCRIPTOR_HANDLE handle = heap->GetCPUDescriptorHandleForHeapStart();
    handle.ptr += index * rtvDescriptorSize;

    commandList->OMSetRenderTargets(1, &handle, FALSE, nullptr);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_clearRenderTarget
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong deviceHandle, jlong rtvHeapHandle, jint index,
   jfloat r, jfloat g, jfloat b, jfloat a) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    ID3D12DescriptorHeap* heap = GET_HANDLE(ID3D12DescriptorHeap, rtvHeapHandle);

    if (!commandList || !device || !heap) return;

    UINT rtvDescriptorSize = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    D3D12_CPU_DESCRIPTOR_HANDLE handle = heap->GetCPUDescriptorHandleForHeapStart();
    handle.ptr += index * rtvDescriptorSize;

    float clearColor[] = { r, g, b, a };
    commandList->ClearRenderTargetView(handle, clearColor, 0, nullptr);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setViewport
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jint x, jint y, jint width, jint height) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    if (!commandList) return;

    D3D12_VIEWPORT viewport = {};
    viewport.TopLeftX = static_cast<float>(x);
    viewport.TopLeftY = static_cast<float>(y);
    viewport.Width = static_cast<float>(width);
    viewport.Height = static_cast<float>(height);
    viewport.MinDepth = 0.0f;
    viewport.MaxDepth = 1.0f;

    commandList->RSSetViewports(1, &viewport);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setScissorRect
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jint x, jint y, jint width, jint height) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    if (!commandList) return;

    D3D12_RECT rect = {};
    rect.left = x;
    rect.top = y;
    rect.right = x + width;
    rect.bottom = y + height;

    commandList->RSSetScissorRects(1, &rect);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setRootSignature
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong rootSigHandle) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12RootSignature* rootSig = GET_HANDLE(ID3D12RootSignature, rootSigHandle);

    if (commandList && rootSig) {
        commandList->SetGraphicsRootSignature(rootSig);
    }
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setGraphicsRoot32BitConstants
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jint rootParameterIndex, jfloatArray values, jint offset) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    if (!commandList) return;

    jsize len = env->GetArrayLength(values);
    jfloat* data = env->GetFloatArrayElements(values, nullptr);

    commandList->SetGraphicsRoot32BitConstants(rootParameterIndex, len, data, offset);

    env->ReleaseFloatArrayElements(values, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setPrimitiveTopology
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jint topology) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    if (commandList) {
        commandList->IASetPrimitiveTopology(static_cast<D3D12_PRIMITIVE_TOPOLOGY>(topology));
    }
}

// =============================================================================
// Pipeline state
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createPipelineState
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jlong rootSigHandle,
   jbyteArray vsBytecode, jbyteArray psBytecode,
   jint topologyType, jint blendMode,
   jintArray formats, jintArray offsets, jint stride) {

    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    ID3D12RootSignature* rootSig = GET_HANDLE(ID3D12RootSignature, rootSigHandle);
    if (!device || !rootSig) return 0;

    // Get shader bytecode
    jsize vsLen = env->GetArrayLength(vsBytecode);
    jsize psLen = env->GetArrayLength(psBytecode);
    jbyte* vsData = env->GetByteArrayElements(vsBytecode, nullptr);
    jbyte* psData = env->GetByteArrayElements(psBytecode, nullptr);

    // Get vertex attribute info
    jsize numAttrs = env->GetArrayLength(formats);
    jint* formatsData = env->GetIntArrayElements(formats, nullptr);
    jint* offsetsData = env->GetIntArrayElements(offsets, nullptr);

    // Build input layout
    std::vector<D3D12_INPUT_ELEMENT_DESC> inputLayout(numAttrs);
    const char* semantics[] = { "POSITION", "COLOR", "TEXCOORD" };

    for (int i = 0; i < numAttrs; i++) {
        inputLayout[i].SemanticName = semantics[i < 3 ? i : 2];
        inputLayout[i].SemanticIndex = i < 3 ? 0 : i - 2;
        inputLayout[i].Format = static_cast<DXGI_FORMAT>(formatsData[i]);
        inputLayout[i].InputSlot = 0;
        inputLayout[i].AlignedByteOffset = offsetsData[i];
        inputLayout[i].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
        inputLayout[i].InstanceDataStepRate = 0;
    }

    // Create PSO descriptor
    D3D12_GRAPHICS_PIPELINE_STATE_DESC psoDesc = {};
    psoDesc.pRootSignature = rootSig;
    psoDesc.VS = { vsData, static_cast<SIZE_T>(vsLen) };
    psoDesc.PS = { psData, static_cast<SIZE_T>(psLen) };
    psoDesc.InputLayout = { inputLayout.data(), static_cast<UINT>(numAttrs) };
    psoDesc.PrimitiveTopologyType = static_cast<D3D12_PRIMITIVE_TOPOLOGY_TYPE>(topologyType);

    // Blend state
    psoDesc.BlendState.AlphaToCoverageEnable = FALSE;
    psoDesc.BlendState.IndependentBlendEnable = FALSE;
    psoDesc.BlendState.RenderTarget[0] = GetBlendDesc(blendMode);

    // Rasterizer state
    psoDesc.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    psoDesc.RasterizerState.CullMode = D3D12_CULL_MODE_NONE;
    psoDesc.RasterizerState.FrontCounterClockwise = FALSE;
    psoDesc.RasterizerState.DepthBias = D3D12_DEFAULT_DEPTH_BIAS;
    psoDesc.RasterizerState.DepthBiasClamp = D3D12_DEFAULT_DEPTH_BIAS_CLAMP;
    psoDesc.RasterizerState.SlopeScaledDepthBias = D3D12_DEFAULT_SLOPE_SCALED_DEPTH_BIAS;
    psoDesc.RasterizerState.DepthClipEnable = TRUE;
    psoDesc.RasterizerState.MultisampleEnable = FALSE;
    psoDesc.RasterizerState.AntialiasedLineEnable = FALSE;
    psoDesc.RasterizerState.ForcedSampleCount = 0;
    psoDesc.RasterizerState.ConservativeRaster = D3D12_CONSERVATIVE_RASTERIZATION_MODE_OFF;

    // Depth/stencil state (disabled for 2D rendering)
    psoDesc.DepthStencilState.DepthEnable = FALSE;
    psoDesc.DepthStencilState.StencilEnable = FALSE;

    // Sample desc
    psoDesc.SampleMask = UINT_MAX;
    psoDesc.SampleDesc.Count = 1;
    psoDesc.SampleDesc.Quality = 0;

    // Render targets
    psoDesc.NumRenderTargets = 1;
    psoDesc.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    psoDesc.DSVFormat = DXGI_FORMAT_UNKNOWN;

    // Create PSO
    ID3D12PipelineState* pso = nullptr;
    HRESULT hr = device->CreateGraphicsPipelineState(&psoDesc, IID_PPV_ARGS(&pso));

    // Cleanup
    env->ReleaseByteArrayElements(vsBytecode, vsData, JNI_ABORT);
    env->ReleaseByteArrayElements(psBytecode, psData, JNI_ABORT);
    env->ReleaseIntArrayElements(formats, formatsData, JNI_ABORT);
    env->ReleaseIntArrayElements(offsets, offsetsData, JNI_ABORT);

    if (FAILED(hr)) return 0;
    return STORE_HANDLE(pso);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyPipelineState
  (JNIEnv* env, jclass clazz, jlong pso) {
    RELEASE_HANDLE(pso);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setPipelineState
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong psoHandle) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12PipelineState* pso = GET_HANDLE(ID3D12PipelineState, psoHandle);

    if (commandList && pso) {
        commandList->SetPipelineState(pso);
    }
}

// =============================================================================
// Shader compilation
// =============================================================================

JNIEXPORT jbyteArray JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_compileShader
  (JNIEnv* env, jclass clazz, jstring source, jstring entryPoint, jstring target) {

    const char* srcStr = env->GetStringUTFChars(source, nullptr);
    const char* entryStr = env->GetStringUTFChars(entryPoint, nullptr);
    const char* targetStr = env->GetStringUTFChars(target, nullptr);

    ComPtr<ID3DBlob> shaderBlob;
    ComPtr<ID3DBlob> errorBlob;

    UINT compileFlags = 0;
#ifdef _DEBUG
    compileFlags = D3DCOMPILE_DEBUG | D3DCOMPILE_SKIP_OPTIMIZATION;
#endif

    HRESULT hr = D3DCompile(srcStr, strlen(srcStr), nullptr, nullptr, nullptr,
                            entryStr, targetStr, compileFlags, 0, &shaderBlob, &errorBlob);

    env->ReleaseStringUTFChars(source, srcStr);
    env->ReleaseStringUTFChars(entryPoint, entryStr);
    env->ReleaseStringUTFChars(target, targetStr);

    if (FAILED(hr)) {
        if (errorBlob) {
            // Could log error message here
        }
        return nullptr;
    }

    // Return bytecode as byte array
    jsize size = static_cast<jsize>(shaderBlob->GetBufferSize());
    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, static_cast<jbyte*>(shaderBlob->GetBufferPointer()));
    return result;
}

// =============================================================================
// Buffer operations
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createBuffer
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jlong size, jint usageFlags) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    D3D12_HEAP_PROPERTIES heapProps = {};
    heapProps.Type = D3D12_HEAP_TYPE_UPLOAD; // CPU-visible for frequent updates

    D3D12_RESOURCE_DESC desc = {};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = size;
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = DXGI_FORMAT_UNKNOWN;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;

    ID3D12Resource* buffer = nullptr;
    if (FAILED(device->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE, &desc,
            D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&buffer)))) {
        return 0;
    }
    return STORE_HANDLE(buffer);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyBuffer
  (JNIEnv* env, jclass clazz, jlong buffer) {
    RELEASE_HANDLE(buffer);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_uploadBufferData
  (JNIEnv* env, jclass clazz, jlong bufferHandle, jfloatArray data, jint offset, jint count) {
    ID3D12Resource* buffer = GET_HANDLE(ID3D12Resource, bufferHandle);
    if (!buffer) return;

    jfloat* floatData = env->GetFloatArrayElements(data, nullptr);

    void* mapped = nullptr;
    D3D12_RANGE readRange = { 0, 0 }; // We're not reading
    if (SUCCEEDED(buffer->Map(0, &readRange, &mapped))) {
        memcpy(static_cast<char*>(mapped) + offset * sizeof(float), floatData, count * sizeof(float));
        buffer->Unmap(0, nullptr);
    }

    env->ReleaseFloatArrayElements(data, floatData, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setVertexBuffer
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong bufferHandle, jint stride, jint size) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12Resource* buffer = GET_HANDLE(ID3D12Resource, bufferHandle);

    if (!commandList || !buffer) return;

    D3D12_VERTEX_BUFFER_VIEW view = {};
    view.BufferLocation = buffer->GetGPUVirtualAddress();
    view.SizeInBytes = size;
    view.StrideInBytes = stride;

    commandList->IASetVertexBuffers(0, 1, &view);
}

// =============================================================================
// Draw calls
// =============================================================================

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_drawInstanced
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jint vertexCount, jint instanceCount,
   jint startVertex, jint startInstance) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    if (commandList) {
        commandList->DrawInstanced(vertexCount, instanceCount, startVertex, startInstance);
    }
}

// =============================================================================
// Texture operations
// =============================================================================

JNIEXPORT jlong JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_createTexture
  (JNIEnv* env, jclass clazz, jlong deviceHandle, jint width, jint height, jint format) {
    ID3D12Device* device = GET_HANDLE(ID3D12Device, deviceHandle);
    if (!device) return 0;

    D3D12_HEAP_PROPERTIES heapProps = {};
    heapProps.Type = D3D12_HEAP_TYPE_DEFAULT;

    D3D12_RESOURCE_DESC desc = {};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = width;
    desc.Height = height;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = static_cast<DXGI_FORMAT>(format);
    desc.SampleDesc.Count = 1;

    ID3D12Resource* texture = nullptr;
    if (FAILED(device->CreateCommittedResource(&heapProps, D3D12_HEAP_FLAG_NONE, &desc,
            D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&texture)))) {
        return 0;
    }
    return STORE_HANDLE(texture);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_destroyTexture
  (JNIEnv* env, jclass clazz, jlong texture) {
    RELEASE_HANDLE(texture);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_uploadTextureData
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong textureHandle,
   jbyteArray data, jint width, jint height, jint bytesPerRow) {
    // This requires an upload buffer and copy operation
    // Simplified implementation - full version would use a staging buffer
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12Resource* texture = GET_HANDLE(ID3D12Resource, textureHandle);

    if (!commandList || !texture) return;

    // Full texture upload implementation would go here
    // This involves creating an upload buffer, mapping it, copying data,
    // then using CopyTextureRegion to copy to the final texture
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_setTexture
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong textureHandle, jint slot) {
    // Texture binding requires setting up descriptor tables
    // Implementation depends on root signature layout
}

// =============================================================================
// Copy operations (for readback)
// =============================================================================

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_copyTextureToBuffer
  (JNIEnv* env, jclass clazz, jlong commandListHandle, jlong textureHandle, jlong bufferHandle,
   jint width, jint height) {
    ID3D12GraphicsCommandList* commandList = GET_HANDLE(ID3D12GraphicsCommandList, commandListHandle);
    ID3D12Resource* texture = GET_HANDLE(ID3D12Resource, textureHandle);
    ID3D12Resource* buffer = GET_HANDLE(ID3D12Resource, bufferHandle);

    if (!commandList || !texture || !buffer) return;

    D3D12_TEXTURE_COPY_LOCATION srcLoc = {};
    srcLoc.pResource = texture;
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    srcLoc.SubresourceIndex = 0;

    D3D12_TEXTURE_COPY_LOCATION dstLoc = {};
    dstLoc.pResource = buffer;
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    dstLoc.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    dstLoc.PlacedFootprint.Footprint.Width = width;
    dstLoc.PlacedFootprint.Footprint.Height = height;
    dstLoc.PlacedFootprint.Footprint.Depth = 1;
    dstLoc.PlacedFootprint.Footprint.RowPitch = ((width * 4) + 255) & ~255; // Aligned to 256

    commandList->CopyTextureRegion(&dstLoc, 0, 0, 0, &srcLoc, nullptr);
}

JNIEXPORT void JNICALL Java_com_edgefound_chartx_render_backend_dx12_DX12Native_readBufferData
  (JNIEnv* env, jclass clazz, jlong bufferHandle, jintArray pixels, jint width, jint height) {
    ID3D12Resource* buffer = GET_HANDLE(ID3D12Resource, bufferHandle);
    if (!buffer) return;

    void* mapped = nullptr;
    D3D12_RANGE readRange = { 0, static_cast<SIZE_T>(width * height * 4) };
    if (SUCCEEDED(buffer->Map(0, &readRange, &mapped))) {
        jint* pixelData = env->GetIntArrayElements(pixels, nullptr);

        // Copy row by row (handle pitch alignment)
        UINT rowPitch = ((width * 4) + 255) & ~255;
        for (int y = 0; y < height; y++) {
            memcpy(pixelData + y * width, static_cast<char*>(mapped) + y * rowPitch, width * 4);
        }

        env->ReleaseIntArrayElements(pixels, pixelData, 0);

        D3D12_RANGE writeRange = { 0, 0 }; // We didn't write
        buffer->Unmap(0, &writeRange);
    }
}

} // extern "C"
