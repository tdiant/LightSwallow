#include "SandboxExec.h"
#include "main.cpp"
#include "cgroups.h"

#include <sched.h>
#include <unistd.h>
#include <sys/resource.h>
#include <sys/ptrace.h>
#include <wait.h>
#include <dirent.h>
#include <cstdlib>
#include <fcntl.h>
#include <sys/procfs.h>
#include <set>

pid_t child_pid;
long used_time, used_mem;

int _ExecCmd();

void _AlarmChild()
{
    kill(child_pid, SIGALRM);
}

int PressExecute()
{
    if (getuid() != 0)
    {
        return ERROR_SET_UID_FAILED;
    }

    if (opendir(dir.c_str()) == nullptr)
    {
        return ERROR_DIR_CANNOT_OPEN;
    }

    if (chroot(dir.c_str()) != 0 || chdir("/") != 0)
    {
        return ERROR_CHROOT_FAILED;
    }

    pid_t pid = fork();
    child_pid = pid;

    if (pid == 0)
    {
        //子进程
        int result = _ExecCmd();
        exit(result);
    } else if (pid > 0)
    {
        //父进程
        signal(SIGALRM, (void (*)(int)) _AlarmChild);
        alarm(MAX_WAIT_SEC);

        int runtimeStatus;
        rusage ru{};
        used_time = 0;
        used_mem = 0;

        int runtimeResult = STOP_SIG_FINISHED;

        while (wait4(pid, &runtimeStatus, 0, &ru) > 0)
        {
            user_regs_struct regs{};
            ptrace(PTRACE_GETREGS, pid, 0, &regs);

            //检查系统调用
            if (regs.orig_rax == SYS_execve && regs.rdi != 0)
                kill(pid, SIGSYS);
            if (SYSTEM_CALL_BLACKLIST.find((int) regs.orig_rax) != SYSTEM_CALL_BLACKLIST.end())
                kill(pid, SIGSYS);

            //时空计算
            used_mem = ru.ru_minflt * getpagesize() / 1024;
            used_time = ru.ru_utime.tv_sec * 1000 + ru.ru_utime.tv_usec / 1000
                        + ru.ru_stime.tv_sec * 1000 + ru.ru_stime.tv_usec / 1000;
            if (used_mem > memory_limit)
                kill(pid, SIGUSR1);
            if (used_time > time_limit * 1.15)
                kill(pid, SIGXCPU);

            int stopSignal, checkSignal = -1;
            if (WIFSTOPPED(runtimeStatus) && (stopSignal = WSTOPSIG(runtimeStatus)) != SIGTRAP)
            {
                checkSignal = stopSignal;
            } else if (WIFSIGNALED(runtimeStatus))
            {
                checkSignal = WTERMSIG(runtimeStatus);
            } else if (WIFEXITED(runtimeStatus))
            {
                if (used_time > time_limit * 1.15)
                {
                    runtimeResult = STOP_SIG_TIME_LIMIT_EXCEEDED;
                } else if (WEXITSTATUS(runtimeStatus) != 0)
                {
                    runtimeResult = STOP_SIG_GENERAL_RUNTIME_ERROR;
                } else
                {
                    runtimeResult = STOP_SIG_FINISHED;
                }
                PressExit();
            } else
            {
                ptrace(PTRACE_SYSCALL, pid, 0, 0);
            }

            if (checkSignal != -1)
            {
                switch (checkSignal)
                {
                    case SIGUSR1:
                        runtimeResult = STOP_SIG_MEM_LIMIT_EXCEEDED;
                        break;
                    case SIGALRM:
                        runtimeResult = STOP_SIG_TIME_LIMIT_EXCEEDED;
                        break;
                    case SIGSEGV:
                        runtimeResult = STOP_SIG_SEGMENT_ERROR;
                        break;
                    case SIGSYS:
                        runtimeResult = STOP_SIG_UNEXPECTED_QUIT;
                        break;
                    default:
                        runtimeStatus = STOP_SIG_ILLEGAL_SYS_CALL;
                }
                PressExit();
            }
        }

        alarm(0);
        return runtimeResult;
    } else
    {
        //创建失败
        PressExit();
        exit(ERROR_RUN_FORK_FAILED);
    }
}


int _ExecCmd()
{
    int sin = 0, sout = 0;
    if (input != "_")
    {
        sin = open(input.c_str(), O_RDONLY, 0644);
        if (sin == -1)
            return ERROR_REDIRECT_STDIO_FAILED;
    }
    if (output != "_")
    {
        sout = open(output.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
        if (sout == -1)
            return ERROR_REDIRECT_STDIO_FAILED;
    }

    InitLimit();

    if (input != "_")
        dup2(sin, fileno(stdin));
    if (output != "_")
        dup2(sout, fileno(stdout));

    if (setuid(233) == -1)
        return ERROR_SET_UID_FAILED;

    ptrace(PTRACE_TRACEME, 0, 0, 0);
    if (execvp(cmd[0], cmd) == -1)
        return ERROR_CREATE_PROCESS_FAILED;

    if (input != "_")
        close(sin);
    if (output != "_")
        close(sout);
}

void PressExit()
{
    //todo
}

void InitLimit()
{
    if (cpu_limit >= 0)
    {
        cpu_set_t t;
        CPU_ZERO(&t);
        CPU_SET(cpu_limit, &t);
        if (sched_setaffinity(0, sizeof(cpu_set_t), &t) == -1)
        {
            exit(ERROR_LIMIT_RUNNABLE_FAILED);
        }
    }

    if (time_limit >= 0)
    {
        rlimit time_rlimit{};
        time_rlimit.rlim_cur = time_limit / 1000 + 2;
        time_rlimit.rlim_max = time_limit / 1000 + 2;
        setrlimit(RLIMIT_CPU, &time_rlimit);
    }

    if (output_size_limit >= 0)
    {
        rlimit output_rlimit{};
        output_rlimit.rlim_cur = output_size_limit << 10;
        output_rlimit.rlim_max = output_size_limit << 10;
        setrlimit(RLIMIT_FSIZE, &output_rlimit);
    }

    if (child_proc_limit > 0)
    {
        rlimit proc_rlimit{};
        proc_rlimit.rlim_cur = child_proc_limit;
        proc_rlimit.rlim_max = child_proc_limit;
        setrlimit(RLIMIT_NPROC, &proc_rlimit);
    }

    //Init cgroup
    CreateCgroup(cgroup_name);
    CgroupCpusetCpus(cgroup_name, 1);

}
