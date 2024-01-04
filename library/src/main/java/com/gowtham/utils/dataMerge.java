package com.gowtham.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class dataMerge {
    ArrayList<String> complexCommand = new ArrayList<>();

        public int execute(Context context,List<String> Videos, String output){

         execFFmpegBinary("-f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -t 0.1 "+ new File(context.getExternalFilesDir(null),"i.wav"));
            for(int i=0;i<Videos.size();i++){
                complexCommand.add(" -i ");
                complexCommand.add(Videos.get(i));
            }
            complexCommand.add(" -i ");
            //[2:a]
            complexCommand.add(new File(context.getExternalFilesDir(null),"i.wav").getAbsolutePath());
            complexCommand.add(" -filter_complex ");
            for(int i=0;i<Videos.size();i++){

                complexCommand.add("["+i+":v]setpts=PTS-STARTPTS,setdar=1:1,scale=1280x720,fps=24,format=yuv420p[video"+i+"];");
                if (videoHasAudio(Videos.get(i))) complexCommand.add("["+i+":a]aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[audio"+i+"];");
                else complexCommand.add("["+Videos.size()+":a]volume=0[audio"+i+"];");

            }
            for(int i=0;i<Videos.size();i++){
                complexCommand.add( "[video"+i+"][audio"+i+"]");
            }



                complexCommand.add( "concat=n="+Videos.size()+":v=1:a=1[outv][outa] -map [outv] -map [outa] -vsync 0 "+output);



            Log.d("fdfdsfsd", String.join("", complexCommand));
            return execFFmpegBinary(String.join("", complexCommand));
        }
        private int execFFmpegBinary(String command) {

            int result = FFmpeg.execute(command);
            return result;
        }
    public  boolean videoHasAudio(String videoPath) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(videoPath);

        try {
            String hasAudio = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            media.release();
            System.out.println("videoHasAudio = " + hasAudio);

            if ("yes".equalsIgnoreCase(hasAudio)) {
                return true;
            } else {
                return false;
            }
        } catch (RuntimeException | IOException e) {
            return false;
        }
    }


}
