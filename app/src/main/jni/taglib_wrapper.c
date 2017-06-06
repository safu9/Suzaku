#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getTitle(JNIEnv *env, jclass type) {

    return (*env)->NewStringUTF(env, "test!");
}