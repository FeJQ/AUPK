#include "art_method-inl.h"
#include "class_linker.h"
#include "class_linker-inl.h"
#include "jni_internal.h"
#include "mirror/class-inl.h"
#include "mirror/object-inl.h"
#include "mirror/object_array-inl.h"
#include "reflection.h"
#include "scoped_fast_native_object_access.h"
#include "well_known_classes.h"

namespace art
{
	extern "C" ArtMethod *jMethodToArtMethod(JNIEnv *env, jobject jMethod)
	{
		ScopedFastNativeObjectAccess soa(env);
		ArtMethod *method = ArtMethod::FromReflectedMethod(soa, jMethod);
		return method;
	}
}