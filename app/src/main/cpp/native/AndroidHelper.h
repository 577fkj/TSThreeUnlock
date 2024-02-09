//
// Created by fkjfk on 2022/10/9.
//

#ifndef GENSHINPROXY_ANDROIDHELPER_H
#define GENSHINPROXY_ANDROIDHELPER_H


#include <jni.h>

class AndroidHelper {
public:
    static void init(_JavaVM *jvm);
    static jobject getGlobalAppContext(JNIEnv *env);
    static JNIEnv *GetEnv();
    static void showToast(char *str);
};


#endif //GENSHINPROXY_ANDROIDHELPER_H
