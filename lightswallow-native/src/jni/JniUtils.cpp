#include <iostream>
#include "../../lib/jni/JniUtils.h"

jclass GetClass(JNIEnv *env, const string &clzName) {
    return env->FindClass(clzName.c_str());
}

jfieldID GottaFieldId(JNIEnv *env, jobject obj, const string &fieldName, const string &fieldSig) {
    jclass clz = env->GetObjectClass(obj);
    return env->GetFieldID(clz, fieldName.c_str(), fieldSig.c_str());
}

jint GetFieldForInt(JNIEnv *env, jobject obj, const string &fieldName) {
    return env->GetIntField(obj, GottaFieldId(env, obj, fieldName, "I"));
}

jlong GetFieldForLong(JNIEnv *env, jobject obj, const string &fieldName) {
    return env->GetLongField(obj, GottaFieldId(env, obj, fieldName, "J"));
}

jstring GetFieldForString(JNIEnv *env, jobject obj, const string &fieldName) {
    return (jstring) GetFieldForObject(env, obj, fieldName, "Ljava/lang/String;");
}

jboolean GetFieldForBoolean(JNIEnv *env, jobject obj, const string &fieldName) {
    return env->GetBooleanField(obj, GottaFieldId(env, obj, fieldName, "Z"));
}

jobject GetFieldForObject(JNIEnv *env, jobject obj, const string &fieldName, const string &fieldSig) {
    return env->GetObjectField(obj, GottaFieldId(env, obj, fieldName, fieldSig));
}

jobject GetFieldForListObject(JNIEnv *env, jobject obj, const string &fieldName) {
    return GetFieldForObject(env, obj, fieldName, "Ljava/util/List;");
}

int TransInt(jint x) {
    return (int) x;
}

long TransLong(jlong x) {
    return (long) x;
}

bool TransBool(jboolean x) {
    return (bool) (x == JNI_TRUE);
}

string TransString(JNIEnv *env, jstring str) {
    return env->GetStringUTFChars(str, nullptr);
}

vector<jobject> TransObjectList(JNIEnv *env, jobject obj) {
    jobject listObj = env->NewGlobalRef(obj);
    vector<jobject> result;
//    jclass clz = env->GetObjectClass(listObj);
    jclass clz = env->FindClass("java/util/ArrayList");

    jmethodID midSize = env->GetMethodID(clz, "size", "()I");
    int size = env->CallIntMethod(listObj, midSize);

    jmethodID midGet = env->GetMethodID(clz, "get", "(I)Ljava/lang/Object;");
    for (int i = 0; i < size; ++i) {
        jobject element = env->CallObjectMethod(listObj, midGet, (jint) i);
        result.push_back(element);
    }

    env->DeleteGlobalRef(listObj);
    return result;
}

vector<string> TransStringList(JNIEnv *env, jobject jobj) {
    vector<jobject> objList = TransObjectList(env, jobj);
    vector<string> result;
    for (auto &i: objList) {
        auto strObj = (jstring) i;
        result.push_back(TransString(env, strObj));
    }
    return result;
}
