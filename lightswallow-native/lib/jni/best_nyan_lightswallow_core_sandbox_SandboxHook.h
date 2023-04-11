#include "../sandbox_runtime.h"

#include <jni.h>

#ifndef _Included_best_nyan_lightswallow_hook_SandboxHook
#define _Included_best_nyan_lightswallow_hook_SandboxHook
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jintArray JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_startSandbox
        (JNIEnv *env, jobject obj, jobject paramSrcObj);

JNIEXPORT jstring JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_waitForProcess
        (JNIEnv *env, jobject, jintArray bufArr);

JNIEXPORT void JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_destroyEnvironment
        (JNIEnv *env, jobject obj, jstring cgroupName, jboolean removeCgroup);

JNIEXPORT jlong JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_getCgroupPropertyAsLong
        (JNIEnv *env, jobject obj, jstring controller, jstring cgroupName, jstring property);

JNIEXPORT jlong JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_readTimeUsage
        (JNIEnv *env, jobject obj, jstring cgroupName);

JNIEXPORT jlong JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_readMemoryUsage
        (JNIEnv *env, jobject obj, jstring cgroupName);

SandboxParameter TransParamFromJavaObj(JNIEnv *env, const jobject &obj);

void CallbackContainerPid(JNIEnv *env, const jobject &obj, const pid_t &pid);

void throwJavaException(JNIEnv *env, const char *msg);

#ifdef __cplusplus
}
#endif
#endif
