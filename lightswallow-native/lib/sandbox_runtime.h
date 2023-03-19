#pragma once

#include "jni.h"
#include "pipe.h"
#include "semaphore.h"

#include <string>
#include <filesystem>
#include <vector>

using std::string;
using std::vector;
using std::unique_ptr;
namespace fs = std::filesystem;

enum RunStatus {
    EXITED = 0, // App exited normally.
    SIGNALED = 01, // App is killed by some signal.
};

struct MountPair {
    fs::path sourcePath;
    fs::path targetPath;
    bool readonly;
};

struct ExecResult {
    RunStatus status;
    int code;
};

struct SandboxParameter {
    string name;
    string executable;
    vector<string> parameters;
    string hostname;
    fs::path chrootPath;
    fs::path chdirPath;
    int64_t memoryLimit = -1;
    int processLimit = -1;
    vector<MountPair> mounts;
    int userUid = -1;
    int userGid = -1;
    int64_t stackSize = -1;
    int cpuCoreCnt = -1;
    vector<string> environments;
    bool redirectIOBeforeChroot;
    bool mountProc = true;
    string redirectStdin;
    string redirectStdout;
    string redirectStderr;
};

struct ExecutionParameter {
    const SandboxParameter &sandboxParameter;
    PosixPipe pipe;
    PosixSemaphore semaphore1, semaphore2;

    ExecutionParameter(const SandboxParameter &param, int pipeOptions)
            : sandboxParameter(param),
              semaphore1(true, 0),
              semaphore2(true, 0),
              pipe(pipeOptions) {}
};

void *StartSandbox(JNIEnv *env, jobject jobj, const SandboxParameter &parameter, pid_t &pid);

ExecResult WaitForProcess(pid_t pid, void *executionParameter);

void DestroySandbox(const string &cgroupName, bool removeCgroup);
