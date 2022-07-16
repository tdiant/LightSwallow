#include <iostream>
#include <unistd.h>
#include <getopt.h>
#include <set>
#include <sys/syscall.h>
#include "file_utils.h"
#include "SandboxExec.h"
#include "cgroups.h"

using namespace std;

const int MAX_WAIT_SEC = 20;

static std::set<int> SYSTEM_CALL_BLACKLIST = {
        SYS_fork, SYS_vfork, SYS_kill, SYS_bind, SYS_accept,
        SYS_listen, SYS_chroot, SYS_chdir, SYS_mount, SYS_mknod,
        SYS_alarm, SYS_ptrace, SYS_pipe, SYS_dup2, SYS_pause,
        SYS_reboot, SYS_shutdown, SYS_setuid, SYS_unlink
};

const int ERROR_INVALID_ARGUMENT = -1;
const int ERROR_LIMIT_RUNNABLE_FAILED = -2;
const int ERROR_REDIRECT_STDIO_FAILED = -3;
const int ERROR_SET_UID_FAILED = -4;
const int ERROR_CREATE_PROCESS_FAILED = -5;
const int ERROR_RUN_FORK_FAILED = -6;
const int ERROR_DIR_CANNOT_OPEN = -7;
const int ERROR_CHROOT_FAILED = -8;
const int STOP_SIG_FINISHED = 0;
const int STOP_SIG_TIME_LIMIT_EXCEEDED = -101;
const int STOP_SIG_MEM_LIMIT_EXCEEDED = -102;
const int STOP_SIG_SEGMENT_ERROR = -103;
const int STOP_SIG_UNEXPECTED_QUIT = -104;
const int STOP_SIG_ILLEGAL_SYS_CALL = -105;
const int STOP_SIG_GENERAL_RUNTIME_ERROR = -106;

const char s_options[] = "e:c:t:m:d:i:o:u:g:";
const option options[] = {
        {"exec",              required_argument, nullptr, 'e'},
        {"cpu_limit",         required_argument, nullptr, 'c'},
        {"time",              required_argument, nullptr, 't'},
        {"memory",            required_argument, nullptr, 'm'},
        {"dir",               required_argument, nullptr, 'd'},
        {"input",             required_argument, nullptr, 'i'},
        {"output",            required_argument, nullptr, 'o'},
        {"child_proc_limit",  required_argument, nullptr, 'p'},
        {"output_size_limit", required_argument, nullptr, 'a'},
        {"mount",             required_argument, nullptr, 'u'},
        {"cgroup",            required_argument, nullptr, 'g'}
};

char *cmd[64] = {};
int cpu_limit = 0;
long time_limit = 1000;
long memory_limit = 256 * 1024 * 1024;
int child_proc_limit = 1;
long output_size_limit = -1;
string dir = ".";
string input = "_";
string output = "_";
int mount_cnt = 0;
string mount_paths[256] = {};
string cgroup_name = "test_cgroup";

void LoadArguments(int argc, char *argv[]);

void LoadArgumentsForMounts(char *arg_str);

int main(int argc, char *argv[])
{
    CgroupSysInit();
//    LoadArguments(argc, argv);
    input = "test";
    PressExecute();
}

void LoadArguments(int argc, char *argv[])
{
    int ret;
    int idx = 0, cnt = 0;
    while ((ret = getopt_long(argc, argv, s_options, options, &idx)) != EOF)
    {
        switch (ret)
        {
            case 'e':
                SplitStringBy(cmd, optarg, "__");
                break;
            case 'c':
                cpu_limit = strtol(optarg, nullptr, 10);
                break;
            case 't':
                time_limit = strtol(optarg, nullptr, 10);
                break;
            case 'm':
                memory_limit = strtol(optarg, nullptr, 10);
                break;
            case 'd':
                dir = optarg;
                break;
            case 'i':
                input = optarg;
                break;
            case 'o':
                output = optarg;
                break;
            case 'p':
                child_proc_limit = strtol(optarg, nullptr, 10);
                break;
            case 'a':
                output_size_limit = strtol(optarg, nullptr, 10);
                break;
            case 'u':
                LoadArgumentsForMounts(optarg);
                break;
            case 'g':
                cgroup_name = optarg;
                break;
            default:
                exit(ERROR_INVALID_ARGUMENT);
        }
        cnt++;
    }

    if (cnt < 8)
        exit(ERROR_INVALID_ARGUMENT);
}

void LoadArgumentsForMounts(char *arg_str)
{
    char *mountsCharArgs[] = {};
    int cnt = SplitStringBy(mountsCharArgs, arg_str, ";");
    for (int i = 0; i < cnt; ++i)
    {
        mount_paths[i] = mountsCharArgs[i];
        ReplaceSubstring(mount_paths[i], "__");
    }
    mount_cnt = cnt;
}
