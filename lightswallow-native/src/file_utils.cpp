#include <filesystem>
#include <fstream>
#include <map>
#include <iostream>
#include <string>
#include <cstring>

#include "../lib/file_utils.h"

using namespace std;
using namespace std::filesystem;

void WriteFile(const path &p, const string &val, bool overwrite) {
    ofstream ofs;
    ofs.exceptions(std::ios::failbit | std::ios::badbit);
    auto flags = ofstream::out | (overwrite ? ofstream::trunc : ofstream::app);
    ofs.open(p, flags);
    ofs << val << std::endl;
}

void WriteFile(const path &p, int64_t val, bool overwrite) {
    ofstream ofs;
    ofs.exceptions(std::ios::failbit | std::ios::badbit);
    auto flags = ofstream::out | (overwrite ? ofstream::trunc : ofstream::app);
    ofs.open(p, flags);
    ofs << val << std::endl;
}

int64_t ReadInt64(const path &p) {
    ifstream ifs;
    ifs.exceptions(std::ios::failbit | std::ios::badbit);
    ifs.open(p);
    int64_t val;
    ifs >> val;
    return val;
}

void ReadToArray64(const path &p, vector<int64_t> &v) {
    ifstream ifs;
    ifs.exceptions(std::ios::badbit);
    ifs.open(p);
    int64_t val;
    while (ifs >> val) {
        v.push_back(val);
    }
}

void ReadToMapInt64(const path &p, map<string, int64_t> mp) {
    ifstream ifs;
    ifs.exceptions(std::ios::badbit);
    ifs.open(p);
    while (ifs) {
        string name;
        int64_t val;
        ifs >> name >> val;
        mp.insert({name, val});
    }
}

string ltos(long l) {
    ostringstream os;
    os << l;
    string result;
    istringstream is(os.str());
    is >> result;
    return result;
}

int SplitStringBy(char **str_arr, char *str, char *sep_str) {
    int cnt = 0;
    char *s;
    s = strtok(str, sep_str);
    cnt++;
    while (s != nullptr) {
        *str_arr++ = s;
        s = strtok(nullptr, sep_str);
        cnt++;
    }
    *str_arr = nullptr;
    return cnt;
}

void ReplaceSubstring(string &src, const string &target) {
    size_t pos;
    while ((pos = src.find(target)) != std::string::npos) {
        src = src.replace(pos, 2, " ");
    }
}
