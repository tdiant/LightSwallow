#pragma once

#include <filesystem>
#include <fstream>
#include <map>
#include <vector>

using namespace std;
using namespace std::filesystem;

void WriteFile(const path &p, const string &val, bool overwrite);

void WriteFile(const path &p, int64_t val, bool overwrite);

int64_t ReadInt64(const path &p);

void ReadToArray64(const path &p, vector<int64_t> &v);

void ReadToMapInt64(const path &p, map<string, int64_t> mp);

string ltos(long l);

int SplitStringBy(char **str_arr, char *str, char *sep_str);

void ReplaceSubstring(string &src, const string &target);
