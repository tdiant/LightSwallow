#pragma once

#include "jni.h"
#include <string>
#include <vector>

using std::string;
using std::vector;

struct RuntimeTransfer {
    pid_t pid;
    void *execParam;
};

jclass GetClass(JNIEnv *env, const string &clzName);

jint GetFieldForInt(JNIEnv *env, jobject obj, const string &fieldName);

jlong GetFieldForLong(JNIEnv *env, jobject obj, const string &fieldName);

jstring GetFieldForString(JNIEnv *env, jobject obj, const string &fieldName);

jboolean GetFieldForBoolean(JNIEnv *env, jobject obj, const string &fieldName);

jobject GetFieldForObject(JNIEnv *env, jobject obj, const string &fieldName, const string &fieldSig);

jobject GetFieldForListObject(JNIEnv *env, jobject obj, const string &fieldName);

int TransInt(jint x);

long TransLong(jlong x);

bool TransBool(jboolean x);

string TransString(JNIEnv *env, jstring str);

vector<jobject> TransObjectList(JNIEnv *env, jobject jobj);

vector<string> TransStringList(JNIEnv *env, jobject jobj);
