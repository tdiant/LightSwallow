#include "../lib/cgroup.h"
#include "../lib/utils.h"
#include "../lib/file_utils.h"

#include <fmt/format.h>
#include <mntent.h>
#include <csignal>
#include <iostream>

using std::string;
using std::vector;
using std::map;
using std::ifstream;
using std::ofstream;
using std::unique_ptr;
using std::invalid_argument;
using std::runtime_error;
using std::make_unique;
using fmt::format;
namespace fs = std::filesystem;

map<string, vector<fs::path>> FindCgroupControllers() {
    map<string, vector<fs::path>> controller_mounts;
    char buf[4 * FILENAME_MAX];

    ifstream proc_cgroup("/proc/cgroups");
    proc_cgroup.ignore(std::numeric_limits<std::streamsize>::max(), '\n'); //ignore the first line

    vector<string> controllers;
    string subsys_name;
    int hierarchy, num_cgroups, enabled;
    while (proc_cgroup >> subsys_name >> hierarchy >> num_cgroups >> enabled) {
        controllers.emplace_back(subsys_name);
    }

    unique_ptr<FILE, decltype(fclose) *> proc_mnt(CHECKNULL(fopen("/proc/mounts", "re")), fclose);
    unique_ptr<mntent> tmp_ent = make_unique<mntent>();
    mntent *ent;
    while ((ent = getmntent_r(proc_mnt.get(), tmp_ent.get(), buf, sizeof(buf))) != nullptr) {
        if (strcmp(ent->mnt_type, "cgroup") != 0)
            continue;
        for (auto &controller: controllers) {
            char *mntopt = hasmntopt(ent, controller.c_str());
            if (!mntopt) continue;
            controller_mounts[controller].emplace_back(string(ent->mnt_dir));
        }
    }
    return controller_mounts;
}

const map<string, vector<fs::path>> cgroup_mnt = FindCgroupControllers();

std::filesystem::path GetControllerPath(const std::string &controller) {
    auto mnt_paths = cgroup_mnt.find(controller);
    if (mnt_paths == cgroup_mnt.end())
        throw invalid_argument((format("Controller {} does not exist!", controller)));
    return (mnt_paths->second)[0];
}

std::filesystem::path GetCgroupPath(const CgroupInfo &info, bool ignoreExist) {
    fs::path cgroup_path = GetControllerPath(info.controller) / info.cgroup_name;
    if (!ignoreExist) {
        if (!fs::exists(cgroup_path) || !fs::is_directory(cgroup_path))
            throw runtime_error(format("Path {}/{} is not valid.", info.controller, info.cgroup_name));
    }
    return cgroup_path;
}

void CreateCgroup(const CgroupInfo &info) {
    auto cgroup_path = GetCgroupPath(info, true);

    if (!fs::exists(cgroup_path))
        fs::create_directories(cgroup_path);
    else if (!fs::is_directory(cgroup_path))
        throw runtime_error(format(
                "Path {}/{} is exists, but not a directory as expected.",
                info.controller, info.cgroup_name)
        );
}

void RemoveCGroup(const CgroupInfo &info) {
    KillCgroupMembers(info);
    auto cgroup_path = GetCgroupPath(info);
    rmdir(cgroup_path.c_str());
}

void KillCgroupMembers(const CgroupInfo &info) {
    auto v = ReadCgroupPropertyAsArray(info, "tasks");
    for (auto &item: v)
        ENSURE(kill((int) item, SIGKILL));
}

void WriteCgroupProperty(const CgroupInfo &info, const std::string &property, int64_t val, bool overwrite) {
    auto cgroup_path = GetCgroupPath(info);
    return WriteFile(cgroup_path / property, val, overwrite);
}

void WriteCgroupProperty(const CgroupInfo &info, const std::string &property, const std::string &val, bool overwrite) {
    auto cgroup_path = GetCgroupPath(info);
    return WriteFile(cgroup_path / property, val, overwrite);
}

int64_t ReadCgroupPropertyAsInt64(const CgroupInfo &info, const std::string &property) {
    auto cgroup_path = GetCgroupPath(info);
    return ReadInt64(cgroup_path / property);
}

std::vector<int64_t> ReadCgroupPropertyAsArray(const CgroupInfo &info, const std::string &property) {
    auto cgroup_path = GetCgroupPath(info);
    vector<int64_t> val;
    ReadToArray64(cgroup_path / property, val);
    return val;
}

std::map<std::string, int64_t> ReadCgroupPropertyAsMap(const CgroupInfo &info, const std::string &property) {
    auto cgroup_path = GetCgroupPath(info);
    map<string, int64_t> res;

    ifstream ifs;
    ifs.exceptions(std::ios::badbit);
    ifs.open(cgroup_path / property);

    while (ifs) {
        string name;
        int64_t val;
        ifs >> name >> val;
        res.insert(pair<string, int64_t>(name, val));
    }

    return res;
}
