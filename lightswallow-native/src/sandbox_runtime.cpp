#include "../lib/jni/best_nyan_lightswallow_core_sandbox_SandboxHook.h"
#include "../lib/cgroup.h"
#include "../lib/utils.h"
#include "lib/file_utils.h"

#include <sys/wait.h>
#include <fmt/format.h>
#include <fcntl.h>
#include <sched.h>
#include <unistd.h>
#include <sys/types.h>
#include <syscall.h>
#include <grp.h>
#include <sys/resource.h>
#include <sys/mount.h>
#include <cassert>
#include <iostream>

using std::vector;
using std::unique_ptr;
using std::make_unique;
using fmt::format;

const int childStackSize = 1024 * 700;

static void EnsureDirectoryExist(fs::path dir) {
    if (!fs::exists(dir)) {
        throw std::runtime_error((format("The specified path {} does not exist.", dir.string())));
    }
    if (!fs::is_directory(dir)) {
        throw std::runtime_error((format("The specified path {} exists, but is not a directory.", dir.string())));
    }
}

vector<int> needCloseFiles; //todo impl this

void RedirectIO(const SandboxParameter &param, int nullfd) {
    const string &rdStdin = param.redirectStdin,
            rdStdout = param.redirectStdout,
            rdStderr = param.redirectStderr;
    int fdInput = nullfd, fdOutput = nullfd, fdError = nullfd;
    if (!rdStdin.empty()) {
        fdInput = ENSURE(open(rdStdin.c_str(), O_RDONLY));
        needCloseFiles.push_back(fdInput);
    }
    if (!rdStdout.empty()) {
        fdOutput = ENSURE(open(rdStdout.c_str(), O_WRONLY | O_TRUNC | O_CREAT, S_IWUSR | S_IRUSR | S_IRGRP | S_IWGRP));
        needCloseFiles.push_back(fdOutput);
    }
    if (!rdStderr.empty()) {
        fdError = ENSURE(open(rdStderr.c_str(), O_WRONLY | O_TRUNC | O_CREAT, S_IWUSR | S_IRUSR | S_IRGRP | S_IWGRP));
        needCloseFiles.push_back(fdError);
    }

    ENSURE(dup2(fdInput, STDIN_FILENO));
    ENSURE(dup2(fdOutput, STDOUT_FILENO));
    ENSURE(dup2(fdError, STDERR_FILENO));
}

int ChildProcess(void *param_ptr) {

    ExecutionParameter &execParam = *reinterpret_cast<ExecutionParameter *>(param_ptr);
    SandboxParameter parameter = execParam.sandboxParameter;
    try {
        ENSURE(close(execParam.pipe[0]));

        int nullfd = ENSURE(open("/dev/null", O_RDWR));
        if (parameter.redirectIOBeforeChroot) {
            RedirectIO(parameter, nullfd);
        }

        ENSURE(mount("none", "/", nullptr, MS_REC | MS_PRIVATE, nullptr)); //let root private

        EnsureDirectoryExist(parameter.chrootPath);
        ENSURE(mount(parameter.chrootPath.string().c_str(),
                     parameter.chrootPath.string().c_str(), "", MS_BIND | MS_RDONLY | MS_REC, ""));
        ENSURE(mount("", parameter.chrootPath.string().c_str(), "", MS_BIND | MS_REMOUNT | MS_RDONLY | MS_REC, ""));

        for (MountPair &mp: parameter.mounts) {
            if (!mp.targetPath.is_absolute()) {
                throw std::invalid_argument(
                        format("The dst path {} in mounts should be absolute.", mp.targetPath.string()));
            }

            fs::path target = parameter.chrootPath / std::filesystem::relative(mp.targetPath, "/");

            EnsureDirectoryExist(mp.sourcePath);
            EnsureDirectoryExist(target);
            ENSURE(mount(mp.sourcePath.string().c_str(), target.string().c_str(), "", MS_BIND | MS_REC, ""));
            if (mp.readonly) {
                ENSURE(mount("", target.string().c_str(), "", MS_BIND | MS_REMOUNT | MS_RDONLY | MS_REC, ""));
            } else {
                ENSURE(mount("", target.string().c_str(), "", MS_BIND | MS_REMOUNT | MS_REC, ""));
            }
        }

        ENSURE(chroot(parameter.chrootPath.string().c_str()));
        ENSURE(chdir(parameter.chdirPath.string().c_str()));

        if (parameter.mountProc) {
            ENSURE(mount("proc", "/proc", "proc", 0, nullptr));
        }

        if (!parameter.redirectIOBeforeChroot) {
            RedirectIO(parameter, nullfd);
        }

        if (!parameter.hostname.empty()) {
            ENSURE(sethostname(parameter.hostname.c_str(), parameter.hostname.length()));
        }

        if (parameter.stackSize != -2) {
            rlimit64 rlim;
            rlim.rlim_max = rlim.rlim_cur = parameter.stackSize != -1 ? parameter.stackSize : RLIM64_INFINITY;
            ENSURE(setrlimit64(RLIMIT_STACK, &rlim));
        }

        rlimit rlim;
        rlim.rlim_max = rlim.rlim_cur = 0;
        ENSURE(setrlimit(RLIMIT_CORE, &rlim));

        if (parameter.userGid != -1) {
            gid_t groupList[1];
            groupList[0] = parameter.userGid;
            ENSURE(syscall(SYS_setgid, parameter.userGid));
            ENSURE(syscall(SYS_setgroups, 1, groupList));
            ENSURE(syscall(SYS_setuid, parameter.userUid));
        }

        vector<char *> params = StringToPtr(parameter.parameters),
                envi = StringToPtr(parameter.environments);

        int temp = -1;
        // Inform the parent that no exception occurred.
        ENSURE(write(execParam.pipe[1], &temp, sizeof(int)));

        // Inform our parent that we are ready to go.
        execParam.semaphore1.Post();
        // Wait for parent's reply.
        execParam.semaphore2.Wait();

        ENSURE(execvpe(parameter.executable.c_str(), &params[0], &envi[0]));

        // error will happen when execvpe return
        return 1;
    }
    catch (std::exception &err) {
        const char *errMessage = err.what();
        size_t len = strlen(errMessage);
        try {
            ENSURE(write(execParam.pipe[1], &len, sizeof(int)));
            ENSURE(write(execParam.pipe[1], errMessage, len));
            ENSURE(close(execParam.pipe[1]));
            execParam.semaphore1.Post();
            return 1;
        }
        catch (...) { assert(false); }
    }
    catch (...) { assert(false); }
    return 0;
}

void *StartSandbox(JNIEnv *env, jobject jobj, const SandboxParameter &parameter, pid_t &pid) {
    pid = -1;
    try {
        vector<char> childStack(childStackSize);
        unique_ptr<ExecutionParameter> execParam = make_unique<ExecutionParameter>(parameter, O_CLOEXEC | O_NONBLOCK);

        pid = ENSURE(clone(ChildProcess, &*childStack.end(),
                           CLONE_NEWNET | CLONE_NEWUTS | CLONE_NEWPID | CLONE_NEWNS | SIGCHLD,
                           const_cast<void *> (reinterpret_cast<const void *>(execParam.get()))));

        CallbackContainerPid(env, jobj, pid);


        CgroupInfo memCgroup("memory", parameter.name),
                cpuCgroup("cpuacct", parameter.name),
                pidCgroup("pids", parameter.name);

        try {
            for (const auto &c: {memCgroup, cpuCgroup, pidCgroup}) {
                CreateCgroup(c);
                KillCgroupMembers(c);
                WriteCgroupProperty(c, "tasks", pid, true);
            }
        } catch (std::exception &ex) {
            std::cout << ex.what() << std::endl;
        }

#define WRITE_WITH_CHECK(__where, __name, __value)                          \
    {                                                                       \
        if ((__value) >= 0)                                                 \
        {                                                                   \
            WriteCgroupProperty((__where), (__name), (__value), true);      \
        }                                                                   \
        else                                                                \
        {                                                                   \
            WriteCgroupProperty((__where), (__name), "max", true);          \
        }                                                                   \
    }

        WriteCgroupProperty(memCgroup, "memory.memsw.limit_in_bytes", -1, true);
        WriteCgroupProperty(memCgroup, "memory.limit_in_bytes", -1, true);
        WRITE_WITH_CHECK(memCgroup, "memory.limit_in_bytes", parameter.memoryLimit);
        WRITE_WITH_CHECK(memCgroup, "memory.memsw.limit_in_bytes", parameter.memoryLimit);
        WRITE_WITH_CHECK(pidCgroup, "pids.max", parameter.processLimit);

        bool waitResult = execParam->semaphore1.TimedWait(500);
        int errLen;
        ssize_t bytesRead = read(execParam->pipe[0], &errLen, sizeof(int));

        if (!waitResult || bytesRead == 0 || bytesRead == -1) {
            if (waitpid(pid, nullptr, WNOHANG) == 0) {
                throw std::runtime_error("The child process is not responding.");
            } else {
                throw std::runtime_error("The child process has exited unexpectedly.");
            }
        } else if (errLen != -1) { //-1 indicates ok
            vector<char> buf(errLen);
            ENSURE(read(execParam->pipe[0], &*buf.begin(), errLen));
            string errstr(buf.begin(), buf.end());
            throw std::runtime_error((format("The child process has reported the following error: {}", errstr)));
        }

        WriteCgroupProperty(memCgroup, "memory.memsw.max_usage_in_bytes", 0, true);
        WriteCgroupProperty(cpuCgroup, "cpuacct.usage", 0, true);

        execParam->semaphore2.Post();
        return execParam.release();
    } catch (std::exception &ex) {
        if (pid != -1) {
            (void) kill(pid, SIGKILL);
            (void) waitpid(pid, nullptr, WNOHANG);
        }
        std::rethrow_exception(std::current_exception());
    }
}

ExecResult WaitForProcess(pid_t pid, void *executionParameter) {
    std::unique_ptr<ExecutionParameter> execParam(reinterpret_cast<ExecutionParameter *>(executionParameter));

    ExecResult result;
    int status;
    ENSURE(waitpid(pid, &status, 0));

    // Try reading error message first
    int errLen, bytesRead = read(execParam->pipe[0], &errLen, sizeof(int));
    if (bytesRead > 0) {
        vector<char> buf(errLen);
        ENSURE(read(execParam->pipe[0], &*buf.begin(), errLen));
        string errstr(buf.begin(), buf.end());
        throw std::runtime_error((format("The child process has reported the following error: {}", errstr)));
    }

    if (WIFEXITED(status)) {
        result.status = EXITED;
        result.code = WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        result.status = SIGNALED;
        result.code = WTERMSIG(status);
    }
    return result;
}

void DestroySandbox(const string &cgroupName, bool removeCgroup) {
    CgroupInfo memCgroup("memory", cgroupName),
            cpuCgroup("cpuacct", cgroupName),
            pidCgroup("pids", cgroupName);

    for (const auto &c: {memCgroup, cpuCgroup, pidCgroup}) {
        KillCgroupMembers(c);
        if (removeCgroup)
            RemoveCGroup(c);
    }
}
