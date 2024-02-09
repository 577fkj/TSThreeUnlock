#include <jni.h>
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <cstdint>
#include "logging.h"
#include <dlfcn.h>
#include "navive_api.h"
#include "AndroidHelper.h"
#include "elf.h"

using namespace std;

static HookFunType hook_func = nullptr;

#define HOOK_SYMBOL(func) hook_func(func, (void*) new_##func, (void**) &orig_##func)
#define HOOK_DEF(ret, func, ...) \
  ret (*orig_##func)(__VA_ARGS__); \
  ret new_##func(__VA_ARGS__)

void dump(const string &comment, void *ptr, int len) {
    string fmt("Dump Memory\n");
    char tget[1024];
    snprintf(tget, sizeof(tget), "%s (%d)\n", comment.c_str(), len);
    fmt.append(tget);
    auto *buf = (byte *) ptr;
    int i, j;
    for (i = 0; i < len; i += 16) {
        snprintf(tget, sizeof(tget), "%08d   ", i);
        fmt.append(tget);
        for (j = 0; j < 16; j++) {
            if (j == 8) fmt.append(" ");
            if (i + j < len) {
                snprintf(tget, sizeof(tget), "%02hhx ", buf[i + j]);
                fmt.append(tget);
            } else {
                fmt.append("   ");
            }
        }
        fmt.append("  ");
        for (j = 0; j < 16; j++) {
            if (i + j < len) {
                snprintf(tget, sizeof(tget), "%c",
                         isprint(static_cast<int>(buf[i + j])) ? static_cast<char>(buf[i + j])
                                                               : '.');
                fmt.append(tget);
            }
        }
        fmt.append("\n");
    }

    const int maxSegmentLength = 1000;  // Maximum segment length for Android Logcat
    if (fmt.length() < maxSegmentLength) {
        LOGD("%s", fmt.c_str());
        return;
    }

    std::istringstream iss(fmt);
    std::string segment;

    while (std::getline(iss, segment)) {
        LOGD("%s", segment.c_str());
    }
}

unsigned char rsa_key[] = {
        0x30, 0x82, 0x1, 0x22, 0x30, 0xd, 0x6, 0x9, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0xd, 0x1, 0x1,
        0x1, 0x5, 0x0, 0x3, 0x82, 0x1, 0xf, 0x0, 0x30, 0x82, 0x1, 0xa, 0x2, 0x82, 0x1, 0x1, 0x0,
        0xc0, 0x3, 0xa4, 0x1a, 0x69, 0x53, 0x92, 0xc8, 0x26, 0x69, 0x1b, 0x1a, 0x1f, 0xac, 0x7f,
        0x5e, 0x6e, 0x65, 0x27, 0x72, 0xc2, 0x20, 0xfa, 0xf5, 0xf0, 0x12, 0x1e, 0xd, 0xa6, 0xea,
        0xdb, 0xc7, 0xf3, 0xa8, 0xfc, 0x1c, 0x98, 0x99, 0x48, 0xa1, 0xff, 0x6d, 0x3f, 0xa4, 0x3c,
        0xa2, 0x5a, 0x34, 0x40, 0x14, 0xb2, 0x34, 0xf7, 0x91, 0x7a, 0xfa, 0x55, 0x59, 0x42, 0xde,
        0xf8, 0xd9, 0x9f, 0xee, 0xa9, 0xf4, 0xf1, 0x28, 0x10, 0xa6, 0x64, 0x2, 0xc9, 0xa7, 0xf8,
        0xa1, 0x6b, 0x97, 0x8f, 0xea, 0xb8, 0x0, 0x7e, 0x5c, 0x26, 0x13, 0xd1, 0x6d, 0x1e, 0x35,
        0x94, 0xb9, 0xe6, 0x92, 0x57, 0xfc, 0x1, 0x6c, 0x84, 0x35, 0xdd, 0x21, 0x79, 0xed, 0x2f,
        0x54, 0x8c, 0x5f, 0x6a, 0xe, 0x5f, 0x6d, 0xa1, 0x73, 0x74, 0x84, 0x86, 0xce, 0x93, 0xe7,
        0x12, 0xf, 0x38, 0xd5, 0xa4, 0x17, 0x39, 0x2, 0x92, 0x4e, 0xc0, 0x72, 0xde, 0x2e, 0x98,
        0x63, 0xa5, 0x21, 0x83, 0x78, 0x59, 0xe5, 0x9f, 0x5f, 0x3c, 0x13, 0x43, 0x5e, 0xad, 0x49,
        0xc3, 0xe8, 0x9b, 0x47, 0x96, 0x9e, 0xe6, 0xce, 0x97, 0xe6, 0xa0, 0x4a, 0xbb, 0x5e, 0x9e,
        0x35, 0x87, 0x31, 0x1d, 0x74, 0x3a, 0x46, 0x98, 0xae, 0xa9, 0x19, 0x17, 0x24, 0x33, 0xec,
        0x28, 0x4, 0xce, 0xa7, 0xa8, 0xd8, 0x32, 0x5c, 0x35, 0x6b, 0xce, 0x82, 0x13, 0x53, 0xc2,
        0x5a, 0x85, 0x9e, 0x86, 0x86, 0x80, 0xe2, 0xd5, 0xeb, 0x5c, 0xab, 0xc6, 0xcc, 0xe7, 0x3b,
        0x9, 0x55, 0x46, 0xab, 0x98, 0x3c, 0x3a, 0x17, 0xee, 0xe8, 0xdf, 0xb2, 0x80, 0x9e, 0xad,
        0x3d, 0x51, 0x65, 0x52, 0x3c, 0x14, 0x2c, 0x43, 0xa3, 0xd7, 0x25, 0x2e, 0x1f, 0xd3, 0x1c,
        0x6b, 0x77, 0xb6, 0x28, 0xb3, 0xc, 0xb0, 0x6f, 0x11, 0xdd, 0x9a, 0xa, 0x51, 0xd8, 0x72,
        0xd7, 0x2, 0x3, 0x1, 0x0, 0x1
};

unsigned char old_rsa_key[] = {
        0x30, 0x82, 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01,
        0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0f, 0x00, 0x30, 0x82, 0x01, 0x0a, 0x02, 0x82, 0x01, 0x01,
        0x00, 0xa3, 0x4b, 0x74, 0xc5, 0x1e, 0x82, 0x87, 0xc9, 0xbb, 0x86, 0x1a, 0xd6, 0x80, 0xc0, 0x63,
        0x04, 0x17, 0x52, 0x68, 0xe0, 0x3f, 0xc0, 0x54, 0xd2, 0x93, 0x6e, 0xc3, 0x30, 0xa9, 0xea, 0xb1,
        0x8a, 0xf0, 0xfc, 0x08, 0x31, 0x26, 0x32, 0xb4, 0xec, 0xac, 0xb1, 0x32, 0x74, 0x58, 0xa2, 0x32,
        0xec, 0x5f, 0x7e, 0x02, 0xcb, 0x74, 0xc2, 0x98, 0xf6, 0xdc, 0x2b, 0x75, 0x7d, 0x98, 0xa0, 0xa3,
        0xc2, 0x56, 0xa4, 0x80, 0x16, 0x13, 0x39, 0xb8, 0x95, 0x29, 0x19, 0xe7, 0x17, 0x5c, 0x2e, 0xdb,
        0x45, 0xa6, 0xb0, 0xe6, 0xf4, 0xc2, 0x36, 0xd1, 0x6e, 0x16, 0x06, 0x0d, 0xb8, 0x66, 0x03, 0x07,
        0x3b, 0x4b, 0x2f, 0x2c, 0x5c, 0xd7, 0xa1, 0x8a, 0xec, 0xff, 0x3b, 0xe3, 0xd3, 0x48, 0x77, 0x30,
        0x5a, 0xf1, 0xcb, 0xdb, 0x4c, 0xee, 0x9b, 0x67, 0x26, 0x1c, 0x39, 0x82, 0x02, 0x34, 0x44, 0x6f,
        0x56, 0x65, 0x03, 0x89, 0xb2, 0x34, 0xdf, 0xed, 0x63, 0xd0, 0xd8, 0x34, 0xc9, 0x10, 0x8c, 0xea,
        0x4c, 0xaa, 0x03, 0x82, 0x21, 0x06, 0xf2, 0x5d, 0xd2, 0x4b, 0x8a, 0x73, 0xb3, 0xc2, 0xb8, 0x1e,
        0x5f, 0xbc, 0x76, 0x21, 0x73, 0x0e, 0x27, 0xb6, 0xdb, 0xc5, 0x61, 0x26, 0xbb, 0x24, 0x57, 0xbe,
        0x23, 0xa2, 0x8e, 0x9f, 0xf5, 0x85, 0x07, 0xe9, 0xc1, 0xea, 0x3a, 0xb2, 0x5c, 0x1f, 0xb9, 0xcf,
        0x62, 0x40, 0x33, 0xc1, 0x42, 0x5e, 0xfb, 0x33, 0xd7, 0x4e, 0x6c, 0xc5, 0x2b, 0xaf, 0xef, 0xf6,
        0xde, 0x9c, 0x44, 0x13, 0x68, 0xbe, 0x53, 0xc5, 0xac, 0x78, 0xfc, 0x13, 0x23, 0x0e, 0x2e, 0xc6,
        0x4c, 0x84, 0x8d, 0xec, 0x37, 0x65, 0x19, 0x15, 0x98, 0x54, 0xed, 0x4e, 0xa7, 0x69, 0xef, 0xbb,
        0x5c, 0x38, 0xe1, 0x49, 0xda, 0x19, 0xf8, 0x2a, 0x48, 0x4b, 0x90, 0xcd, 0x21, 0x49, 0xa2, 0xcb,
        0x45, 0x02, 0x03, 0x01, 0x00, 0x01
};
unsigned int old_rsa_key_size = sizeof(old_rsa_key);

HOOK_DEF(int, rsa_import, const unsigned char *in, unsigned long inlen, void *key) {
    LOGD("rsa_import call!");
//    dump("rsa_import", (void *) in, (int) inlen);
    if (inlen == 294 && memcmp(in, old_rsa_key, old_rsa_key_size) == 0) {
        LOGD("rsa_import: old key detected, replacing with new key");
        memcpy((void *) in, rsa_key, old_rsa_key_size);
    }
    return orig_rsa_import(in, inlen, key);
}

static void native_hook(const char *name, void *handle) {
    if (!name) return;
    if (strstr(name, "libts3client_android.so")) {
        LOGD("Hooking libts3client_android.so");
        void *rsa_import = ElfUtils::GetModuleOffset("libts3client_android.so", 0x663E84);
        LOGD("rsa_import: %p", rsa_import);
        HOOK_SYMBOL(rsa_import);
        LOGD("Hooked rsa_import");
    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hookFunc;
    return native_hook;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    AndroidHelper::init(vm);
    return JNI_VERSION_1_6;
}