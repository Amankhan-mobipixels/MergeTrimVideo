package com.gowtham.utils;

import static com.google.common.io.Files.getFileExtension;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.gowtham.ui.ActVideoTrimmer;

public class TrimVideo {

    public static final String TRIM_VIDEO_OPTION = "trim_video_option", TRIM_VIDEO_URI = "trim_video_uri" , TRIM_Output = "TRIM_Output",Extension="extension";

    public static ActivityBuilder activity(String input, String dirOutput) {
        return new ActivityBuilder(input,dirOutput);
    }

    public static final class ActivityBuilder {

        @Nullable
        private final String videoUri;
        private final String output;
        private final String extensions;
        private final TrimVideoOptions options;

        public ActivityBuilder(@Nullable String input,@Nullable String output) {
            this.videoUri = input;
            this.output=output;
            this.extensions = "."+getFileExtension(input);
            options = new TrimVideoOptions();
            options.trimType=TrimType.DEFAULT;
            options.compressOption = new CompressOption();
        }
        public void start(Activity activity) {
            validate();
            Intent intent = new Intent(activity,  ActVideoTrimmer.class);
            Gson gson = new Gson();
            Bundle bundle=new Bundle();
            bundle.putString(TRIM_VIDEO_URI, videoUri);
            bundle.putString(Extension, extensions);
            bundle.putString(TRIM_VIDEO_OPTION, gson.toJson(options));
            bundle.putString(TRIM_Output,output);
            intent.putExtras(bundle);
            activity.startActivity(intent);
        }

        private void validate() {
            if (videoUri == null)
                throw new NullPointerException("VideoUri cannot be null.");
            if (videoUri.isEmpty())
                throw new IllegalArgumentException("VideoUri cannot be empty");
            if (options.trimType == null)
                throw new NullPointerException("TrimType cannot be null");
            if (options.minDuration < 0)
                throw new IllegalArgumentException("Cannot set min duration to a number < 1");
            if (options.fixedDuration < 0)
                throw new IllegalArgumentException("Cannot set fixed duration to a number < 1");
            if (options.trimType==TrimType.MIN_MAX_DURATION && options.minToMax==null)
                throw new IllegalArgumentException("Used trim type is TrimType.MIN_MAX_DURATION." +
                        "Give the min and max duration");
            if (options.minToMax != null){
                if ((options.minToMax[0] < 0 || options.minToMax[1] < 0))
                    throw new IllegalArgumentException("Cannot set min to max duration to a number < 1");
                if ((options.minToMax[0] > options.minToMax[1]))
                    throw new IllegalArgumentException("Minimum duration cannot be larger than max duration");
                if ((options.minToMax[0] == options.minToMax[1]))
                    throw new IllegalArgumentException("Minimum duration cannot be same as max duration.Use Fixed duration");
            }
        }


    }


}
