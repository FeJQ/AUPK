
#ifndef ART_RUNTIME_AUPK_H_
#define ART_RUNTIME_AUPK_H_

#include "art_method.h"
#include "thread.h"
#include <string>
#include <thread>
#include <jni.h>

using namespace std;



namespace art
{
    //void register_android_app_Aupk(JNIEnv *env);



    class Aupk
    {
    public:

       
        static void aupkFakeInvoke(ArtMethod *artMethod);

        static uint8_t *getCodeItemEnd(const uint8_t **pData);
        static char *base64Encode(char *str, long str_len, long *outlen);
        static bool getProcessName(char *szProcName);

        static void mapToFile();
        static void dumpClassName(const DexFile *dexFile, const char *feature);
        static void dumpMethod(ArtMethod *artMethod, const char *feature);
        static void dumpDexFile(const DexFile *dexFile, const char *feature);

        static void setThread(Thread *thread);
        static void setMethod(ArtMethod *method);
        static bool isFakeInvoke(Thread *thread, ArtMethod *method);
        static void register_android_app_Aupk(JNIEnv *env);
        
    private:
        static Thread *aupkThread;
        static ArtMethod *aupkArtMethod;
    };

}

#endif // ART_RUNTIME_AUPK_H_
