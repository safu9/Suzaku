//
// Created by safu9 on 2017/05/17.
//

#include <jni.h>
#include <android/log.h>
#include <fileref.h>
#include <tpropertymap.h>
#include <mpegfile.h>
#include <attachedpictureframe.h>
#include <id3v2tag.h>
#include <mp4tag.h>
#include <mp4file.h>


#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "HelloJni", __VA_ARGS__)

// static jclass myClass;
// static jfieldID fieldID;

// Handle Utils

jfieldID getHandleField(JNIEnv *env, jobject obj, const char* name)
{
    jclass c = env->GetObjectClass(obj);
    return env->GetFieldID(c, name, "J");    // J is the type signature for long:
}

template <typename T>
T *getHandle(JNIEnv *env, jobject obj, const char* name)
{
    jlong handle = env->GetLongField(obj, getHandleField(env, obj, name));
    return reinterpret_cast<T *>(handle);
}

template <typename T>
void setHandle(JNIEnv *env, jobject obj, const char* name, T *t)
{
    jlong handle = reinterpret_cast<jlong>(t);
    env->SetLongField(obj, getHandleField(env, obj, name), handle);
}

// Writing


jbyteArray as_byte_array(JNIEnv* env, signed char* buf, int len) {
    jbyteArray array = env->NewByteArray (len);
    env->SetByteArrayRegion (array, 0, len, buf);
    return array;
}


void releaseFile(JNIEnv *env, jobject instance)
{
    TagLib::FileRef* file = getHandle<TagLib::FileRef>(env, instance, "fileRefHandle");
    delete file;
    setHandle<TagLib::FileRef>(env, instance, "fileRefHandle", 0);
    TagLib::PropertyMap* tags = getHandle<TagLib::PropertyMap>(env, instance, "tagsHandle");
    delete tags;
    setHandle<TagLib::PropertyMap>(env, instance, "tagsHandle", 0);
}

TagLib::String getTag(JNIEnv *env, jobject instance, const char* key)
{
    TagLib::PropertyMap* tags = getHandle<TagLib::PropertyMap>(env, instance, "tagsHandle");
    if(!tags){
        TagLib::FileRef* file = getHandle<TagLib::FileRef>(env, instance, "fileRefHandle");
        tags = new TagLib::PropertyMap(file->file()->properties());
        setHandle<TagLib::PropertyMap>(env, instance, "tagsHandle", tags);
    }

    TagLib::PropertyMap::ConstIterator i = tags->find(key);
    if(i == tags->end()){
        return TagLib::String::null;
    }

    if(i->second.size() <= 0){
        return TagLib::String::null;
    }

    TagLib::String str = *(i->second.begin());
    return str;
}

extern "C" {

/*
JNIEXPORT void JNICALL
Java_com_citrus_suzaku_TagLibHelper_init(JNIEnv *env, jobject instance)
{
    if (!myClass) {
        myClass = env->GetObjectClass(instance);
     //   checkException(env);
    }

    if (!fieldID) {
        fieldID = env->GetFieldID(myClass, "mHandle", "J");
     //   checkException(env);
    }
}
*/

JNIEXPORT void JNICALL
Java_com_citrus_suzaku_TagLibHelper_setFile(JNIEnv *env, jobject instance, jstring path_)
{
    const char *path = env->GetStringUTFChars(path_, 0);

    TagLib::FileRef* file = getHandle<TagLib::FileRef>(env, instance, "fileRefHandle");
    if(file){
        releaseFile(env, instance);
    }
    file = new TagLib::FileRef(path);
    setHandle<TagLib::FileRef>(env, instance, "fileRefHandle", file);

    TagLib::Tag* tag = file->tag();
    setHandle<TagLib::Tag>(env, instance, "tagHandle", tag);

    env->ReleaseStringUTFChars(path_, path);
}


JNIEXPORT void JNICALL
Java_com_citrus_suzaku_TagLibHelper_dumpTags(JNIEnv *env, jobject instance, jclass type)
{

    TagLib::FileRef* file = getHandle<TagLib::FileRef>(env, instance, "fileRefHandle");
    if(!file){
        return;
    }

    if(file->isNull()){
        LOGI("-- TAG IS NULL --");
        return;
    }

    if (file->tag()) {

        TagLib::Tag *tag = file->tag();

        LOGI("-- TAG (basic) --");
        LOGI("title - %s", tag->title().toCString(true));
        LOGI("artist - %s", tag->artist().toCString(true));
        LOGI("album - %s", tag->album().toCString(true));
        LOGI("year - %d", tag->year());
        LOGI("comment - %s", tag->comment().toCString(true));
        LOGI("track - %d", tag->track());
        LOGI("genre - %s", tag->genre().toCString(true));

        TagLib::PropertyMap tags = file->file()->properties();

        LOGI("-- TAG (properties) --");
        for (TagLib::PropertyMap::ConstIterator i = tags.begin(); i != tags.end(); ++i) {
            for (TagLib::StringList::ConstIterator j = i->second.begin(); j != i->second.end(); ++j) {
                LOGI("%s - %s", i->first.toCString(true), j->toCString(true));
            }
        }

        LOGI("-- TAG (unsupported) --");
        TagLib::StringList untags = tags.unsupportedData();
        for (TagLib::StringList::ConstIterator k = untags.begin(); k != untags.end(); ++k) {
            LOGI("%s", k->toCString(true));
        }
    }

    if (file->audioProperties()) {

        TagLib::AudioProperties *properties = file->audioProperties();

        int seconds = properties->length() % 60;
        int minutes = (properties->length() - seconds) / 60;

        LOGI("-- AUDIO --");
        LOGI("bitrate - %d", properties->bitrate());
        LOGI("sampleRate - %d", properties->sampleRate());
        LOGI("channels - %d", properties->channels());
        LOGI("length - %d:%02d", minutes, seconds);
    }
}


JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getTitle(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return env->NewStringUTF(tag->title().toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getTitleSort(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "TITLESORT").toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getArtist(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return env->NewStringUTF(tag->artist().toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getArtistSort(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "ARTISTSORT").toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getAlbum(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return env->NewStringUTF(tag->album().toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getAlbumSort(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "ALBUMSORT").toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getAlbumArtist(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "ALBUMARTIST").toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getAlbumArtistSort(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "ALBUMARTISTSORT").toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getGenre(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return env->NewStringUTF(tag->genre().toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getComposer(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "COMPOSER").toCString(true));
}

JNIEXPORT jint JNICALL
Java_com_citrus_suzaku_TagLibHelper_getYear(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return tag->year();
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getLyrics(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "LYRICS").toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getComment(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return env->NewStringUTF(tag->comment().toCString(true));
}

JNIEXPORT jstring JNICALL
Java_com_citrus_suzaku_TagLibHelper_getGroup(JNIEnv *env, jobject instance)
{
    return env->NewStringUTF(getTag(env, instance, "CONTENTGROUP").toCString(true));
}

JNIEXPORT jint JNICALL
Java_com_citrus_suzaku_TagLibHelper_getTrackNumber(JNIEnv *env, jobject instance)
{
    TagLib::Tag* tag = getHandle<TagLib::Tag>(env, instance, "tagHandle");
    if(!tag){
        return NULL;
    }

    return tag->track();
}

JNIEXPORT jint JNICALL
Java_com_citrus_suzaku_TagLibHelper_getDiscNumber(JNIEnv *env, jobject instance)
{
    TagLib::String disc = getTag(env, instance, "DISCNUMBER");
    return disc.split("/").begin()->toInt();
}

JNIEXPORT jboolean JNICALL
Java_com_citrus_suzaku_TagLibHelper_getCompilation(JNIEnv *env, jobject instance)
{
    TagLib::String comp = getTag(env, instance, "COMPILATION");
    return (comp.toInt() > 0)? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_citrus_suzaku_TagLibHelper_getArtwork(JNIEnv *env, jobject instance)
{
    TagLib::PropertyMap* tags = getHandle<TagLib::PropertyMap>(env, instance, "tagsHandle");
    if(!tags){
        TagLib::FileRef* file = getHandle<TagLib::FileRef>(env, instance, "fileRefHandle");
        tags = new TagLib::PropertyMap(file->file()->properties());
        setHandle<TagLib::PropertyMap>(env, instance, "tagsHandle", tags);
    }

    TagLib::FileRef* file = getHandle<TagLib::FileRef>(env, instance, "fileRefHandle");
    TagLib::StringList untags = tags->unsupportedData();

    if(untags.contains("APIC")){            // ID3v2 MP3

        TagLib::File* ffile = file->file();
        TagLib::MPEG::File *mfile = dynamic_cast<TagLib::MPEG::File*>(ffile);
        if(mfile == NULL){
            return NULL;
        }

        TagLib::ID3v2::Tag *t = mfile->ID3v2Tag();

        TagLib::ID3v2::FrameList frames = t->frameList("APIC");

        TagLib::ID3v2::AttachedPictureFrame *frame = static_cast<TagLib::ID3v2::AttachedPictureFrame *>(frames.front());
        TagLib::ByteVector artwork = frame->picture();

        return as_byte_array(env, (signed char*)artwork.data(), artwork.size());

    }else if(untags.contains("covr")){      // MP4 M4A
        TagLib::File* ffile = file->file();
        TagLib::MP4::File *mfile = dynamic_cast<TagLib::MP4::File*>(ffile);
        if(mfile == NULL){
            return NULL;
        }

        TagLib::MP4::Tag *t = mfile->tag();

        TagLib::MP4::ItemListMap itemsListMap = t->itemListMap();
        auto i = itemsListMap.find("covr");
        TagLib::MP4::CoverArtList list = i->second.toCoverArtList();
        TagLib::ByteVector artwork = list.begin()->data();

        return as_byte_array(env, (signed char*)artwork.data(), artwork.size());
    }

    return NULL;
}

JNIEXPORT void JNICALL
Java_com_citrus_suzaku_TagLibHelper_release(JNIEnv *env, jobject instance)
{
    releaseFile(env, instance);
}

}