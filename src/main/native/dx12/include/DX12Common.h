#pragma once

// Windows and DirectX 12 headers
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <d3dcompiler.h>
#include <wrl/client.h>

// Standard library
#include <unordered_map>
#include <mutex>
#include <string>
#include <vector>

// JNI
#include <jni.h>

// Use Microsoft::WRL::ComPtr for COM object management
using Microsoft::WRL::ComPtr;

// Global handle management for COM objects
// Maps jlong handles to IUnknown* pointers
class HandleManager {
public:
    static HandleManager& instance() {
        static HandleManager inst;
        return inst;
    }

    template<typename T>
    jlong store(T* obj) {
        if (!obj) return 0;
        obj->AddRef();
        std::lock_guard<std::mutex> lock(mutex_);
        jlong handle = nextHandle_++;
        objects_[handle] = obj;
        return handle;
    }

    template<typename T>
    T* get(jlong handle) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = objects_.find(handle);
        return (it != objects_.end()) ? static_cast<T*>(it->second) : nullptr;
    }

    void release(jlong handle) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = objects_.find(handle);
        if (it != objects_.end()) {
            it->second->Release();
            objects_.erase(it);
        }
    }

private:
    HandleManager() : nextHandle_(1) {}
    std::unordered_map<jlong, IUnknown*> objects_;
    std::mutex mutex_;
    jlong nextHandle_;
};

// Convenience macros
#define STORE_HANDLE(obj) HandleManager::instance().store(obj)
#define GET_HANDLE(type, handle) HandleManager::instance().get<type>(handle)
#define RELEASE_HANDLE(handle) HandleManager::instance().release(handle)

// Error checking
inline void ThrowIfFailed(HRESULT hr, const char* msg) {
    if (FAILED(hr)) {
        throw std::runtime_error(std::string(msg) + " HRESULT: " + std::to_string(hr));
    }
}

// Blend mode constants (matching Java enum)
enum BlendModeInt {
    BLEND_NONE = 0,
    BLEND_ALPHA = 1,
    BLEND_ADDITIVE = 2,
    BLEND_MULTIPLY = 3,
    BLEND_PREMULTIPLIED_ALPHA = 4
};

// Helper to configure blend state
inline D3D12_RENDER_TARGET_BLEND_DESC GetBlendDesc(int blendMode) {
    D3D12_RENDER_TARGET_BLEND_DESC desc = {};
    desc.LogicOpEnable = FALSE;
    desc.LogicOp = D3D12_LOGIC_OP_NOOP;
    desc.RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;

    switch (blendMode) {
        case BLEND_NONE:
            desc.BlendEnable = FALSE;
            break;
        case BLEND_ALPHA:
            desc.BlendEnable = TRUE;
            desc.SrcBlend = D3D12_BLEND_SRC_ALPHA;
            desc.DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
            desc.BlendOp = D3D12_BLEND_OP_ADD;
            desc.SrcBlendAlpha = D3D12_BLEND_ONE;
            desc.DestBlendAlpha = D3D12_BLEND_INV_SRC_ALPHA;
            desc.BlendOpAlpha = D3D12_BLEND_OP_ADD;
            break;
        case BLEND_ADDITIVE:
            desc.BlendEnable = TRUE;
            desc.SrcBlend = D3D12_BLEND_SRC_ALPHA;
            desc.DestBlend = D3D12_BLEND_ONE;
            desc.BlendOp = D3D12_BLEND_OP_ADD;
            desc.SrcBlendAlpha = D3D12_BLEND_ONE;
            desc.DestBlendAlpha = D3D12_BLEND_ONE;
            desc.BlendOpAlpha = D3D12_BLEND_OP_ADD;
            break;
        case BLEND_MULTIPLY:
            desc.BlendEnable = TRUE;
            desc.SrcBlend = D3D12_BLEND_DEST_COLOR;
            desc.DestBlend = D3D12_BLEND_ZERO;
            desc.BlendOp = D3D12_BLEND_OP_ADD;
            desc.SrcBlendAlpha = D3D12_BLEND_DEST_ALPHA;
            desc.DestBlendAlpha = D3D12_BLEND_ZERO;
            desc.BlendOpAlpha = D3D12_BLEND_OP_ADD;
            break;
        case BLEND_PREMULTIPLIED_ALPHA:
            desc.BlendEnable = TRUE;
            desc.SrcBlend = D3D12_BLEND_ONE;
            desc.DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
            desc.BlendOp = D3D12_BLEND_OP_ADD;
            desc.SrcBlendAlpha = D3D12_BLEND_ONE;
            desc.DestBlendAlpha = D3D12_BLEND_INV_SRC_ALPHA;
            desc.BlendOpAlpha = D3D12_BLEND_OP_ADD;
            break;
    }
    return desc;
}
