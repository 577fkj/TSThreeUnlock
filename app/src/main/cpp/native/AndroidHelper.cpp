//
// Created by fkjfk on 2022/10/9.
//

#include "AndroidHelper.h"
#include "jni.h"

jobject g_context = 0;
JavaVM* g_jvm = nullptr;
using namespace std;

JNIEnv *AndroidHelper::GetEnv()
{
    if(g_jvm == nullptr)
        return nullptr;

    int status;
    JNIEnv *env = nullptr;
    status = g_jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    if(status < 0)
    {
        status = g_jvm->AttachCurrentThread(&env, nullptr);
        if(status < 0)
        {
            return nullptr;
        }
    }
    return env;
}

jobject AndroidHelper::getGlobalAppContext(JNIEnv *env)
{
    if(env == nullptr)
        return nullptr;

    if(g_context != nullptr)
        return g_context;

    //获取Activity Thread的实例对象
    jclass activityThread = env->FindClass("android/app/ActivityThread");
    jmethodID currentActivityThread = env->GetStaticMethodID(activityThread, "currentActivityThread", "()Landroid/app/ActivityThread;");
    if(currentActivityThread == nullptr)
        return nullptr;

    jobject at = env->CallStaticObjectMethod(activityThread, currentActivityThread);
    if(at == nullptr)
        return nullptr;

    //获取Application，也就是全局的Context
    jmethodID getApplication = env->GetMethodID(activityThread, "getApplication", "()Landroid/app/Application;");
    if(getApplication == nullptr)
        return nullptr;

    g_context = env->CallObjectMethod(at, getApplication);
    return g_context;
}

void AndroidHelper::init(JavaVM* jvm)
{
    g_jvm = jvm;
}

void AndroidHelper::showToast(char *str) {
    JNIEnv* env;
    env = GetEnv();
    if (env == nullptr) return;

    auto context = getGlobalAppContext(env);
    auto toastClazz = env->FindClass("android/widget/Toast");
    auto method = env->GetStaticMethodID(toastClazz, "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;");
    auto toastBuild = env->CallStaticObjectMethod(toastClazz, method, context, env->NewStringUTF(str), 1);
    auto buildClazz = env->GetObjectClass(toastBuild);
    auto showMethod = env->GetMethodID(buildClazz, "show", "()V");
    env->CallVoidMethod(toastBuild, showMethod);
}