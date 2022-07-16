// cgroup的简单实现
// 提供操作cgroup的简单工具
//
// 注：本工具基于cgroup v2实现
// https://www.kernel.org/doc/Documentation/cgroup-v2.txt
// Reference: simple-sandbox
//
// Made with ❤ by tdiant
// 2021.8.22

#pragma once

#include <string>
#include <vector>
#include <filesystem>

using namespace std;

// 系统cgroup路径
filesystem::path cgroup_path;

// 系统支持的控制器列表
vector<string> controllers; //todo 不支持v2需要更改

// 工具初始化
void CgroupSysInit();

// 创建一个cgroup
void CreateCgroup(const string &name);

// 获取cgroup里的进程PID
void GetProcsInCgroup(const string &name, vector<int64_t> &v);

// 杀死cgroup里的所有进程
void KillAllCgroup(const string &name);

// 往一个cgroup里增加进程
void AddProcToCgroup(const string &name, int64_t pid);

// 删除一个cgroup
void RemoveCgroup(const string &name);

// 限制cgroup可用CPU核心数量
void CgroupCpusetCpus(const string &name, int num);





