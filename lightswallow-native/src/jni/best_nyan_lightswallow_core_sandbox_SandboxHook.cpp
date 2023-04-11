#include "../../lib/jni/best_nyan_lightswallow_core_sandbox_SandboxHook.h"
#include "../../lib/jni/JniUtils.h"
#include "../../lib/cgroup.h"

#include <iostream>
#include <cstring>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

JNIEXPORT jintArray JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_startSandbox
        (JNIEnv *env, jobject obj, jobject paramSrcObj) {

    std::cout << "Loading sandbox params..." << std::endl;
    jobject paramsGlobalObj = env->NewGlobalRef(paramSrcObj);
    SandboxParameter params = TransParamFromJavaObj(env, paramsGlobalObj);
    env->DeleteGlobalRef(paramsGlobalObj);

    std::cout << "Starting sandbox..." << std::endl;

    try {
        pid_t pid;
        void *execParam = StartSandbox(env, obj, params, pid);

        RuntimeTransfer rt{pid, execParam};
        const int bufferSize = sizeof(rt) * 2 + 50;
        int byteBuffer[bufferSize];
        memcpy(byteBuffer, &rt, sizeof(rt));

        jintArray bufArr = env->NewIntArray(bufferSize);
        env->SetIntArrayRegion(bufArr, 0, bufferSize, byteBuffer);

        dup2(STDIN_FILENO, STDIN_FILENO);
        dup2(STDOUT_FILENO, STDOUT_FILENO);
        dup2(STDERR_FILENO, STDERR_FILENO);

        return bufArr;
    }
    catch (std::exception &ex) {
        std::cerr << "Something wrong happened when starting." << std::endl;
        std::cerr << ex.what() << std::endl;
        throwJavaException(env, ex.what());
    }
    catch (...) {
        throwJavaException(env, "Something unexpected happened while starting sandbox.");
    }
    return nullptr;
}

JNIEXPORT jstring JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_waitForProcess
        (JNIEnv *env, jobject, jintArray bufArr) {

    int *buf = env->GetIntArrayElements(bufArr, nullptr);
    RuntimeTransfer rt{};
    memcpy(&rt, buf, sizeof(RuntimeTransfer));

    ExecResult er = WaitForProcess(rt.pid, rt.execParam);

    std::stringstream ss;
    ss << er.code << ";" << er.status;
    return env->NewStringUTF(ss.str().c_str());
}

JNIEXPORT void JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_destroyEnvironment
        (JNIEnv *env, jobject obj, jstring cgroupName, jboolean removeCgroup) {
    DestroySandbox(TransString(env, cgroupName), TransBool(removeCgroup));
}

JNIEXPORT jlong JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_getCgroupPropertyAsLong
        (JNIEnv *env, jobject obj, jstring controller, jstring cgroupName, jstring property) {
    CgroupInfo cgroupInfo = CgroupInfo(TransString(env, controller), TransString(env, cgroupName));
    int64_t x = ReadCgroupPropertyAsInt64(cgroupInfo, TransString(env, property));
    return x;
}

JNIEXPORT jlong JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_readTimeUsage
        (JNIEnv *env, jobject obj, jstring cgroupName) {
    CgroupInfo cgroupInfo = CgroupInfo("cpuacct", TransString(env, cgroupName));
    int64_t timeUsage = ReadCgroupPropertyAsInt64(cgroupInfo, "cpuacct.usage");
    return timeUsage;
}

JNIEXPORT jlong JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_readMemoryUsage
        (JNIEnv *env, jobject obj, jstring cgroupName) {
    CgroupInfo cgroupInfo = CgroupInfo("memory", TransString(env, cgroupName));
    int64_t maxUsageBytes = ReadCgroupPropertyAsInt64(cgroupInfo, "memory.memsw.max_usage_in_bytes");
    std::map<string, int64_t> stat = ReadCgroupPropertyAsMap(cgroupInfo, "memory.stat");
    int64_t cacheUsage = stat["cache"];
    return maxUsageBytes - cacheUsage;
}

JNIEXPORT jboolean JNICALL Java_best_nyan_lightswallow_core_sandbox_SandboxHook_createNamedPipe
        (JNIEnv *env, jobject obj, jstring pipeName) {
    return mkfifo(TransString(env, pipeName).c_str(), 0777) == 0;
}

std::vector<MountPair> TransMountPairs(JNIEnv *env, const jobject &obj) {
    jobject listObj = GetFieldForListObject(env, obj, "mounts");
    std::vector<jobject> objList = TransObjectList(env, listObj);
    std::vector<MountPair> result;
    for (auto &mpObj: objList) {
        MountPair mp;
        mp.sourcePath = TransString(env, GetFieldForString(env, mpObj, "sourcePath"));
        mp.targetPath = TransString(env, GetFieldForString(env, mpObj, "targetPath"));
        mp.readonly = TransBool(GetFieldForBoolean(env, mpObj, "readonly"));
        result.push_back(mp);
    }
    return result;
}

SandboxParameter TransParamFromJavaObj(JNIEnv *env, const jobject &obj) {
    SandboxParameter params;
    params.name = TransString(env, GetFieldForString(env, obj, "name"));
    params.executable = TransString(env, GetFieldForString(env, obj, "executable"));
    params.parameters = TransStringList(env, GetFieldForListObject(env, obj, "parameters"));
    params.hostname = TransString(env, GetFieldForString(env, obj, "hostname"));
    params.chrootPath = fs::path(TransString(env, GetFieldForString(env, obj, "chrootPath")));
    params.chdirPath = fs::path(TransString(env, GetFieldForString(env, obj, "chdirPath")));
    params.memoryLimit = TransLong(GetFieldForLong(env, obj, "memoryLimit"));
    params.processLimit = TransInt(GetFieldForInt(env, obj, "processLimit"));
    params.mounts = TransMountPairs(env, obj);
    params.userUid = TransInt(GetFieldForInt(env, obj, "userUid"));
    params.userGid = TransInt(GetFieldForInt(env, obj, "userGid"));
    params.stackSize = TransLong(GetFieldForLong(env, obj, "stackSize"));
    params.cpuCoreCnt = TransInt(GetFieldForInt(env, obj, "cpuCoreCnt"));
    params.environments = TransStringList(env, GetFieldForListObject(env, obj, "environments"));
    params.redirectIOBeforeChroot = TransBool(GetFieldForBoolean(env, obj, "redirectIOBeforeChroot"));
    params.mountProc = TransBool(GetFieldForBoolean(env,obj,"mountProc"));
    params.redirectStdin = TransString(env, GetFieldForString(env, obj, "stdin"));
    params.redirectStdout = TransString(env, GetFieldForString(env, obj, "stdout"));
    params.redirectStderr = TransString(env, GetFieldForString(env, obj, "stderr"));

    if (params.stackSize <= 0)
        params.stackSize = -2;

    return params;
}

void CallbackContainerPid(JNIEnv *env, const jobject &obj, const pid_t &pid) {
    jclass clz = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(clz, "callbackContainerPid", "(I)V");
    env->CallVoidMethod(obj, mid, pid);
}

void throwJavaException(JNIEnv *env, const char *msg) {
    jclass c = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(c, msg);
}
