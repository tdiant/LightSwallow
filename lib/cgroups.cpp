#include <string>
#include <utility>
#include <vector>
#include <stdexcept>
#include <filesystem>
#include <cstring>
#include <iostream>
#include <csignal>

#include "cgroups.h"
#include "file_utils.h"

using namespace std;

void CgroupSysInit()
{
    //find cgroup path
    std::ifstream procMounts("/proc/mounts");
    string a, b, c, d, e, f;
    bool found = false;
    string cgroup_pathStr;
    while (procMounts >> a >> b >> c >> d >> e >> f)
    {
        if (strcmp(a.c_str(), "cgroup2") == 0)
        {
            found = true;
            cgroup_pathStr = b;
        }
    }
    if (!found) throw runtime_error("Cannot found cgroup2");
    cgroup_path = filesystem::path(cgroup_pathStr);

    //find controllers
    vector<string> ls;
    std::ifstream procCgroups("/proc/cgroups");
    procCgroups.ignore(numeric_limits<streamsize>::max(), '\n'); //ignore first line
    string subsys_name;
    int p, q, r;
    while (procCgroups >> subsys_name >> p >> q >> r)
        controllers.push_back(subsys_name);
}

void CreateCgroup(const string &name)
{
    if (name.empty()) throw invalid_argument("Name cannot be empty");

    //Create Dir
    auto dir = cgroup_path / name;
    if (!filesystem::exists(dir))
        filesystem::create_directories(dir);
    else
    {
        if (!filesystem::is_directory(dir))
            throw runtime_error("cgroup named " + name + " is not a directory.");
    }
}

void GetProcsInCgroup(const string &name, vector<int64_t> &v)
{
    ReadToArray64(cgroup_path / name / "cgroup.procs", v);
}

void KillAllCgroup(const string &name)
{
    vector<int64_t> v;
    GetProcsInCgroup(name, v);
    for (auto &i: v)
    {
        cout << "debug::" << i << '\n';
        kill((int) i, SIGKILL);
    }
}

void AddProcToCgroup(const string &name, int64_t pid)
{
    auto file = cgroup_path / name / "cgroup.procs";
    WriteFile(file, pid, true);
}

void RemoveCgroup(const string &name)
{
    if (name.empty()) throw invalid_argument("Name cannot be empty");

    KillAllCgroup(name);

    auto dir = cgroup_path / name;
    if (!filesystem::exists(dir)) return;
    if (!filesystem::is_directory(dir))
        throw runtime_error("cgroup named " + name + " is not a directory.");

    filesystem::remove(dir);
}

void CgroupCpusetCpus(const string &name, int num)
{
    if (num < 0 || num > 8) return;
    stringstream v;
    v << "0-" << (num - 1);
    WriteFile(cgroup_path / name / "cpuset.cpus", v.str(), true);
}

void CgroupMemoryMax(const string &name, long num)
{
    WriteFile(cgroup_path / name / "memory.max", num, false);
}

void CgroupMemoryHigh(const string &name, long num)
{
    WriteFile(cgroup_path / name / "memory.high", num, false);
}

void CgroupMemorySwapMax(const string &name, long num)
{
    WriteFile(cgroup_path / name / "memory.swap.max", num, false);
}



