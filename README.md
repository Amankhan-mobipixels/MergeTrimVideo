# TrimVideo

//add maven in your project level gradle

````
allprojects {

	repositories {
		...
		maven { url 'https://jitpack.io' 
		}
	}
}
````
// add dependency in module level gradle

````
dependencies:
{
implementation 'com.github.Amankhan-mobipixels:TrimVideo:v0.0.2'
}

````
if you found any issue related to 2 files found with path 'lib/arm64-v8a/libavcodec.so'
add
````
android {
 packagingOptions {
        pickFirst 'lib/arm64-v8a/libavcodec.so'
        pickFirst 'lib/arm64-v8a/libavdevice.so'
        pickFirst 'lib/arm64-v8a/libavfilter.so'
        pickFirst 'lib/arm64-v8a/libavformat.so'
        pickFirst 'lib/arm64-v8a/libavutil.so'
        pickFirst 'lib/arm64-v8a/libswresample.so'
        pickFirst 'lib/arm64-v8a/libswscale.so'

        pickFirst 'lib/armeabi-v7a/libavcodec.so'
        pickFirst 'lib/armeabi-v7a/libavdevice.so'
        pickFirst 'lib/armeabi-v7a/libavfilter.so'
        pickFirst 'lib/armeabi-v7a/libavformat.so'
        pickFirst 'lib/armeabi-v7a/libavutil.so'
        pickFirst 'lib/armeabi-v7a/libswresample.so'
        pickFirst 'lib/armeabi-v7a/libswscale.so'
    }
}

````
if application crashes during video trimming
add
````
defaultConfig {
ndk {
            abiFilters "armeabi","armeabi-v7a", "x86", "x86_64", "mips"
        }
}
````
How to use:

        TrimVideo.activity(inputpath,outputpath).start(this)
