# TrimVideo
//add maven in your project level gradle

allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

// add dependency in module level gradle

dependencies

{
         implementation 'com.github.Amankhan-mobipixels:TrimVideo:1.0'
}

How it works:

        TrimVideo.activity(inputpath,outputpath).start(this)
