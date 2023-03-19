#pragma once

#include <string>
#include <list>
#include <map>
#include <utility>
#include <vector>
#include <filesystem>
#include <algorithm>

struct CgroupInfo {
    std::string controller;
    std::string cgroup_name;

    CgroupInfo(const std::string &controller, const std::string &group)
            : controller(controller), cgroup_name(group) {
        if (controller.empty() || group.empty())
            throw std::invalid_argument("Invalid arguments for CgroupInfo.");

    }

};

std::map<std::string, std::vector<std::filesystem::path>> FindCgroupControllers();

std::filesystem::path GetControllerPath(const std::string &controller);

std::filesystem::path GetCgroupPath(const CgroupInfo &info, bool ignoreExist = false);

void CreateCgroup(const CgroupInfo &info);

void RemoveCGroup(const CgroupInfo &info);

void KillCgroupMembers(const CgroupInfo &info);

void WriteCgroupProperty(const CgroupInfo &info, const std::string &property, int64_t val, bool overwrite);

void WriteCgroupProperty(const CgroupInfo &info, const std::string &property, const std::string &val, bool overwrite);

int64_t ReadCgroupPropertyAsInt64(const CgroupInfo &info, const std::string &property);

std::vector<int64_t> ReadCgroupPropertyAsArray(const CgroupInfo &info, const std::string &property);

std::map<std::string, int64_t> ReadCgroupPropertyAsMap(const CgroupInfo &info, const std::string &property);
