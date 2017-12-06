//
// Created by bulefin on 2017/10/31.
//
#include <jni.h>
#include <android/log.h>
#include <unistd.h>

#define TAG "fileManager2"
#define LOG_INFO(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)


 static JavaVM* javaVM;
static int jVm = 0;

static void test2(JNIEnv* env, jobject object, jint uid) {
    LOG_ERROR("this is test2 method uid= %d , pid= %d", getuid(), getpid());
    if (fork() == 0) {
        LOG_ERROR("fork child process uid= %d , pid= %d", getuid(), getpid());
        int n = 2;
        while (1) {
            //setuid(0);
            //setgid(0);
            LOG_ERROR("child process n=%d uid= %d , pid= %d", n, getuid(), getpid());

            sleep(2);
            n--;
            if (n == 0)
                break;
        }
    } else {

    }
}

static void set(JNIEnv* env2, jobject obj, jint uid) {
    LOG_ERROR("this is set method %d", uid);
    if (javaVM == NULL) {
        LOG_ERROR("javaVm is null ,address=%d", jVm);
        javaVM = (JavaVM*)jVm;
    }
    if (javaVM == NULL) {
        LOG_ERROR("javaVm is null 2");
        return;
    }
    JNIEnv *env = NULL;
    if (javaVM->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOG_ERROR("env is null");
        return ;
    }
    jclass javaClass = env->FindClass("com/lu/filemanager2/MainActivityTest");
    if (javaClass == NULL) {
        LOG_ERROR("java class is null");
        return ;
    }
    jmethodID methodId = env->GetStaticMethodID(javaClass, "callJava", "()V");
    env->CallStaticVoidMethod(javaClass, methodId);

}

static const JNINativeMethod methods[] = {
        {"test", "(I)V", (void*)test2},
        {"setUid", "(I)V", (void*)set}
};


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOG_ERROR("---this is test-----> JNI_OnLoad");
    JNIEnv *env = NULL;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass javaClass = env->FindClass("com/lu/filemanager2/MainActivityTest");
    if (javaClass == NULL) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(javaClass, methods, sizeof(methods) / sizeof(JNINativeMethod)) != JNI_OK) {
        return JNI_ERR;
    }

    //javaVM = vm;
    jVm = (int)vm;
    LOG_ERROR("jVm --->%d", jVm);
    return JNI_VERSION_1_6;

}

#include <stdio.h>
#include <stdlib.h>
#define SOCKET_NAME "pym_local_socket"

#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/un.h>
#include <stddef.h>

/* socket类型 */
#define SOCK_STREAM      1
#define SOCK_DGRAM       2
#define SOCK_RAW         3
#define SOCK_RDM         4
#define SOCK_SEQPACKET   5
#define SOCK_PACKET      10

/* socket命名空间(见cutils/sockets.h) */
#define ANDROID_SOCKET_NAMESPACE_ABSTRACT 0
#define ANDROID_SOCKET_NAMESPACE_RESERVED 1
#define ANDROID_SOCKET_NAMESPACE_FILESYSTEM 2

int main(){

    int socketID = socket(AF_LOCAL, SOCK_STREAM, 0);
    if(socketID < 0) {
        printf("socket(AF_LOCAL, SOCK_STREAM, 0) is failure\n");
        return 0;
    }
    struct sockaddr_un addr;
    socklen_t socklen;
    memset(&addr, 0, sizeof(addr));
    addr.sun_path[0] = 0;
    memcpy(addr.sun_path + 1, SOCKET_NAME, strlen(SOCKET_NAME));
    addr.sun_family = AF_LOCAL;
    socklen = strlen(SOCKET_NAME) + offsetof(struct sockaddr_un, sun_path) + 1;
    if (connect(socketID, (struct sockaddr *)&addr, socklen) < 0) {
        printf("connect is failure\n");
        return 0;
    }
    printf("start send msg to socketServer\n");
    char buf[] = "hello socket";
    write(socketID, buf, strlen(buf));


}


