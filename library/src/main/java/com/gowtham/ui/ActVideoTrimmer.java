package com.gowtham.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.gson.Gson;
import com.gowtham.library.R;
import com.gowtham.ui.seekbar.widgets.CrystalRangeSeekbar;
import com.gowtham.ui.seekbar.widgets.CrystalSeekbar;
import com.gowtham.utils.CompressOption;
import com.gowtham.utils.TrimVideo;
import com.gowtham.utils.TrimVideoOptions;
import com.gowtham.utils.TrimmerUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.Executors;


public class ActVideoTrimmer extends AppCompatActivity {

    private static final int PER_REQ_CODE = 115;
    private StyledPlayerView playerView;
    private ExoPlayer videoPlayer;
    private ImageView imagePlayPause;
    private ImageView[] imageViews;
    private long totalDuration;
    private Dialog dialog;
    private String local;
    private Uri uri;
    private TextView txtStartDuration, txtEndDuration;
    private CrystalRangeSeekbar seekbar;
    private long lastMinValue = 0;
    private long lastMaxValue = 0;
    private MenuItem menuDone;
    private CrystalSeekbar seekbarController;
    private boolean isValidVideo = true, isVideoEnded;
    private Handler seekHandler;
    private Bundle bundle;
    private ProgressBar progressBar;
    private TrimVideoOptions trimVideoOptions;
    private long currentDuration, lastClickedTime;
    Runnable updateSeekbar = new Runnable() {
        @Override
        public void run() {
            try {
                currentDuration = videoPlayer.getCurrentPosition() / 1000;
                if (!videoPlayer.getPlayWhenReady())
                    return;
                if (currentDuration <= lastMaxValue)
                    seekbarController.setMinStartValue((int) currentDuration).apply();
                else
                    videoPlayer.setPlayWhenReady(false);
            } finally {
                seekHandler.postDelayed(updateSeekbar, 1000);
            }
        }
    };
    private CompressOption compressOption;
    private String outputPath;
    private int trimType;
    private long fixedGap, minGap, minFromGap, maxToGap;
    private boolean hidePlayerSeek, isAccurateCut, showFileLocationAlert;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_video_trimmer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bundle = getIntent().getExtras();
        Gson gson = new Gson();
        String videoOption = bundle.getString(TrimVideo.TRIM_VIDEO_OPTION);
        trimVideoOptions = gson.fromJson(videoOption, TrimVideoOptions.class);
        setUpToolBar(getSupportActionBar(), trimVideoOptions.title);
        toolbar.setNavigationOnClickListener(v -> finish());
    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        playerView = findViewById(R.id.player_view_lib);
        imagePlayPause = findViewById(R.id.image_play_pause);
        seekbar = findViewById(R.id.range_seek_bar);
        txtStartDuration = findViewById(R.id.txt_start_duration);
        txtEndDuration = findViewById(R.id.txt_end_duration);
        seekbarController = findViewById(R.id.seekbar_controller);
        progressBar = findViewById(R.id.progress_circular);
        ImageView imageOne = findViewById(R.id.image_one);
        ImageView imageTwo = findViewById(R.id.image_two);
        ImageView imageThree = findViewById(R.id.image_three);
        ImageView imageFour = findViewById(R.id.image_four);
        ImageView imageFive = findViewById(R.id.image_five);
        ImageView imageSix = findViewById(R.id.image_six);
        ImageView imageSeven = findViewById(R.id.image_seven);
        ImageView imageEight = findViewById(R.id.image_eight);
        imageViews = new ImageView[]{imageOne, imageTwo, imageThree,
                imageFour, imageFive, imageSix, imageSeven, imageEight};
        seekHandler = new Handler();
        initPlayer();
        if (checkStoragePermission())
            setDataInView();
    }

    private void setUpToolBar(ActionBar actionBar, String title) {
        try {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(title != null ? title : getString(R.string.toolbar_text));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * SettingUp exoplayer
     **/
    private void initPlayer() {
        try {
            videoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setPlayer(videoPlayer);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build();
                videoPlayer.setAudioAttributes(audioAttributes, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDataInView() {
        try {
            Runnable fileUriRunnable = () -> {
                uri = Uri.parse(bundle.getString(TrimVideo.TRIM_VIDEO_URI));
//              String path = FileUtils.getPath(ActVideoTrimmer.this, uri);
//                String path=FileUtils.getRealPath(ActVideoTrimmer.this,uri);
//                uri = Uri.parse(path);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    totalDuration = TrimmerUtils.getDuration(ActVideoTrimmer.this, uri);
                    imagePlayPause.setOnClickListener(v ->
                            onVideoClicked());
                    Objects.requireNonNull(playerView.getVideoSurfaceView()).setOnClickListener(v ->
                            onVideoClicked());
                    initTrimData();
                    buildMediaSource(uri);
                    loadThumbnails();
                    setUpSeekBar();
                });
            };
            Executors.newSingleThreadExecutor().execute(fileUriRunnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTrimData() {
        try {
            assert trimVideoOptions != null;
            trimType = TrimmerUtils.getTrimType(trimVideoOptions.trimType);
            fileName = trimVideoOptions.fileName;
            hidePlayerSeek = trimVideoOptions.hideSeekBar;
            isAccurateCut = trimVideoOptions.accurateCut;
            local = trimVideoOptions.local;
            compressOption = trimVideoOptions.compressOption;
            showFileLocationAlert = trimVideoOptions.showFileLocationAlert;
            fixedGap = trimVideoOptions.fixedDuration;
            fixedGap = fixedGap != 0 ? fixedGap : totalDuration;
            minGap = trimVideoOptions.minDuration;
            minGap = minGap != 0 ? minGap : totalDuration;
            if (trimType == 3) {
                minFromGap = trimVideoOptions.minToMax[0];
                maxToGap = trimVideoOptions.minToMax[1];
                minFromGap = minFromGap != 0 ? minFromGap : totalDuration;
                maxToGap = maxToGap != 0 ? maxToGap : totalDuration;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void onVideoClicked() {
        try {
            if (isVideoEnded) {
                seekTo(lastMinValue);
                videoPlayer.setPlayWhenReady(true);
                return;
            }
            if ((currentDuration - lastMaxValue) > 0)
                seekTo(lastMinValue);
            videoPlayer.setPlayWhenReady(!videoPlayer.getPlayWhenReady());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seekTo(long sec) {
        if (videoPlayer != null)
            videoPlayer.seekTo(sec * 1000);
    }

    private void buildMediaSource(Uri mUri) {
        try {
            DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mUri));
            videoPlayer.addMediaSource(mediaSource);
            videoPlayer.prepare();
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    imagePlayPause.setVisibility(playWhenReady ? View.GONE :
                            View.VISIBLE);
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    switch (state) {
                        case Player.STATE_ENDED:

                            imagePlayPause.setVisibility(View.VISIBLE);
                            isVideoEnded = true;
                            break;
                        case Player.STATE_READY:
                            isVideoEnded = false;
                            imagePlayPause.setVisibility(View.GONE);
                            startProgress();
                            break;
                        default:
                            break;
                        case Player.STATE_BUFFERING:

                            break;
                        case Player.STATE_IDLE:
                            break;
                    }
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     *  loading thumbnails
     * */
    private void loadThumbnails() {
        try {
            long diff = totalDuration / 8;
            int sec = 1;
            for (ImageView img : imageViews) {
                long interval = (diff * sec) * 1000000;
                RequestOptions options = new RequestOptions().frame(interval);
                Glide.with(this)
                        .load(bundle.getString(TrimVideo.TRIM_VIDEO_URI))
                        .apply(options)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(img);
                if (sec < totalDuration)
                    sec++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpSeekBar() {
        seekbar.setVisibility(View.VISIBLE);
        txtStartDuration.setVisibility(View.VISIBLE);
        txtEndDuration.setVisibility(View.VISIBLE);

        seekbarController.setMaxValue(totalDuration).apply();
        seekbar.setMaxValue(totalDuration).apply();
        seekbar.setMaxStartValue((float) totalDuration).apply();
        if (trimType == 1) {
            seekbar.setFixGap(fixedGap).apply();
            lastMaxValue = totalDuration;
        } else if (trimType == 2) {
            seekbar.setMaxStartValue((float) minGap);
            seekbar.setGap(minGap).apply();
            lastMaxValue = totalDuration;
        } else if (trimType == 3) {
            seekbar.setMaxStartValue((float) maxToGap);
            seekbar.setGap(minFromGap).apply();
            lastMaxValue = maxToGap;
        } else {
            seekbar.setGap(2).apply();
            lastMaxValue = totalDuration;
        }
        if (hidePlayerSeek)
            seekbarController.setVisibility(View.GONE);

        seekbar.setOnRangeSeekbarFinalValueListener((minValue, maxValue) -> {
            if (!hidePlayerSeek)
                seekbarController.setVisibility(View.VISIBLE);
        });

        seekbar.setOnRangeSeekbarChangeListener((minValue, maxValue) -> {
            long minVal = (long) minValue;
            long maxVal = (long) maxValue;
            if (lastMinValue != minVal) {
                seekTo((long) minValue);
                if (!hidePlayerSeek)
                    seekbarController.setVisibility(View.INVISIBLE);
            }
            lastMinValue = minVal;
            lastMaxValue = maxVal;
            txtStartDuration.setText(TrimmerUtils.formatSeconds(minVal));
            txtEndDuration.setText(TrimmerUtils.formatSeconds(maxVal));
            if (trimType == 3)
                setDoneColor(minVal, maxVal);
        });

        seekbarController.setOnSeekbarFinalValueListener(value -> {
            long value1 = (long) value;
            if (value1 < lastMaxValue && value1 > lastMinValue) {
                seekTo(value1);
                return;
            }
            if (value1 > lastMaxValue)
                seekbarController.setMinStartValue((int) lastMaxValue).apply();
            else if (value1 < lastMinValue) {
                seekbarController.setMinStartValue((int) lastMinValue).apply();
                if (videoPlayer.getPlayWhenReady())
                    seekTo(lastMinValue);
            }
        });
    }

    /**
     * will be called whenever seekBar range changes
     * it checks max duration is exceed or not.
     * and disabling and enabling done menuItem
     *
     * @param minVal left thumb value of seekBar
     * @param maxVal right thumb value of seekBar
     */
    private void setDoneColor(long minVal, long maxVal) {
        try {
            if (menuDone == null)
                return;
            //changed value is less than maxDuration
            if ((maxVal - minVal) <= maxToGap) {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhite)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = true;
            } else {
                menuDone.getIcon().setColorFilter(
                        new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorWhiteLt)
                                , PorterDuff.Mode.SRC_IN)
                );
                isValidVideo = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PER_REQ_CODE) {
            if (isPermissionOk(grantResults))
                setDataInView();
            else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayer != null)
            videoPlayer.release();
        deleteFile("temp_file");
        stopRepeatingTask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuDone = menu.findItem(R.id.action_done);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            //preventing multiple clicks
            if (SystemClock.elapsedRealtime() - lastClickedTime < 800)
                return true;
            lastClickedTime = SystemClock.elapsedRealtime();
          showDialogs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isOnline(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo mobileNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (mobileNetworkInfo != null && mobileNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                } else return wifiNetworkInfo != null && wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED;
            }
        } else {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR");
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI");
                        return true;
                    } else return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                }
            }
        }
        return false;
    }

    private void showDialogs() {
    Dialog nameDialog = new Dialog(this, androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert);
        nameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nameDialog.setCancelable(false);
        nameDialog.setContentView(R.layout.namedialog);
        EditText name = nameDialog.findViewById(R.id.name);
        Button submit = nameDialog.findViewById(R.id.submit);
        Button cancel = nameDialog.findViewById(R.id.cancel);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!name.getText().toString().isEmpty()){
                        File fileOutput = new File(bundle.getString(TrimVideo.TRIM_Output));

                        if (!fileOutput.exists()){
                            fileOutput.mkdir();
                        }
                        trimVideo(fileOutput.getAbsolutePath() +"/"+ name.getText().toString() + bundle.getString(TrimVideo.Extension));
                        nameDialog.dismiss();

//                        ProgressDialog progressDialog = new ProgressDialog(ActVideoTrimmer.this);
//                        progressDialog.setTitle("Ad Loading");
//                        progressDialog.setCancelable(false);
//                        progressDialog.show();

//                        loadShowInterstitialAd(ActVideoTrimmer.this, getString(R.string.Id_Interstitial),(loaded, failed, showed, dismissed) -> {
//                            if (showed){
//                                progressDialog.cancel();
//                                String time = String.valueOf((Calendar.getInstance().getTimeInMillis()));
//                                trimVideo(bundle.getString(TrimVideo.TRIM_Output)+name.getText().toString()+"_"+time+bundle.getString(TrimVideo.Extension));
//                                nameDialog.dismiss();
//                            }
//                            if (failed){
//                                progressDialog.cancel();
//                                String time = String.valueOf((Calendar.getInstance().getTimeInMillis()));
//                                trimVideo(bundle.getString(TrimVideo.TRIM_Output)+name.getText().toString()+"_"+time+bundle.getString(TrimVideo.Extension));
//                                nameDialog.dismiss();
//                            }
//                            return null;
//                        });
                }
                else{
                    Toast.makeText(ActVideoTrimmer.this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                }

            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { nameDialog.dismiss(); }
        });
        nameDialog.show();
    }
    private void trimVideo(String outPAth) {
        if (!new File(outPAth).exists()) {
            if (isValidVideo) {
                //not exceed given maxDuration if has given
                outputPath = outPAth;

                videoPlayer.setPlayWhenReady(false);
                showProcessingDialog();
                String[] complexCommand;
                if (compressOption != null && !bundle.getString(TrimVideo.Extension).equals(".aac")) {
                    complexCommand = getCompressionCmd();
                    Log.d("fdfd", "compression");
                } else if (!isAccurateCut) {
                    //no changes in video quality
                    //faster trimming command and given duration will be accurate
                    complexCommand = getAccurateCmd();
                    Log.d("fdfd", "fastest");
                } else {
                    Log.d("fdfd", "delay");

                    //no changes in video quality
                    //fastest trimming command however, result duration
                    //will be low accurate(2-3 secs)
                    complexCommand = new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                            "-i", String.valueOf(uri),
                            "-t",
                            TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue),
                            "-async", "1", "-strict", "-2", "-c", "copy", outputPath};
                }
                execFFmpegBinary(complexCommand, true);
            } else
                Toast.makeText(this, getString(R.string.txt_smaller) + " " + TrimmerUtils.getLimitedTimeFormatted(maxToGap), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, getString(R.string.file_name_exist), Toast.LENGTH_SHORT).show();
        }
    }

//    private String getFileName() {
//        String path = getExternalFilesDir("TrimmedVideo").getPath();
//        Calendar calender = Calendar.getInstance();
//        String fileDateTime = calender.get(Calendar.YEAR) + "_" +
//                calender.get(Calendar.MONTH) + "_" +
//                calender.get(Calendar.DAY_OF_MONTH) + "_" +
//                calender.get(Calendar.HOUR_OF_DAY) + "_" +
//                calender.get(Calendar.MINUTE) + "_" +
//                calender.get(Calendar.SECOND);
//        String fName = "trimmed_video_";
//        if (fileName != null && !fileName.isEmpty())
//            fName = fileName;
//        File newFile = new File(path + File.separator +
//                (fName) + fileDateTime + "." + TrimmerUtils.getFileExtension(this, uri));
//        return String.valueOf(newFile);
//    }

    private String[] getCompressionCmd() {
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(String.valueOf(uri));
        String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        int w = TrimmerUtils.clearNull(width).isEmpty() ? 0 : Integer.parseInt(width);
        int h = Integer.parseInt(height);
        int rotation = TrimmerUtils.getVideoRotation(this, uri);
        if (rotation == 90 || rotation == 270) {
            int temp = w;
            w = h;
            h = temp;
        }
        //Default compression option
        if (compressOption.getWidth() != 0 || compressOption.getHeight() != 0
                || !compressOption.getBitRate().equals("0k")) {
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                    "-i", String.valueOf(uri), "-s", compressOption.getWidth() + "x" +
                    compressOption.getHeight(),
                    "-r", String.valueOf(compressOption.getFrameRate()),
                    "-vcodec", "mpeg4", "-b:v",
                    compressOption.getBitRate(), "-b:a", "48000", "-ac", "2", "-ar",
                    "22050", "-t",
                    TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        }
        //Dividing high resolution video by 2(ex: taken with camera)
        else if (w >= 800) {
            w = w / 2;
            h = Integer.parseInt(height) / 2;
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                    "-i", String.valueOf(uri),
                    "-s", w + "x" + h, "-r", "30",
                    "-vcodec", "mpeg4", "-b:v",
                    "1M", "-b:a", "48000", "-ac", "2", "-ar", "22050",
                    "-t",
                    TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        } else {
            return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                    "-i", String.valueOf(uri), "-s", w + "x" + h, "-r",
                    "30", "-vcodec", "mpeg4", "-b:v",
                    "400K", "-b:a", "48000", "-ac", "2", "-ar", "22050",
                    "-t",
                    TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
        }
    }

    private void execFFmpegBinary(final String[] command, boolean retry) {
        Log.d("dsfjh", outputPath);
        try {
            new Thread(() -> {
                int result = FFmpeg.execute(command);
                if (result == 0) {
                    dialog.dismiss();
                    runOnUiThread(() -> {
                        Toast.makeText(ActVideoTrimmer.this, "Successful", Toast.LENGTH_SHORT).show();
//                        Intent intent = (new Intent(this, VideoPlayer.class));
//                        intent.putExtra("videoData", outputPath);
//                        startActivity(intent);
                    });
                } else if (result == 255) {
                    if (dialog.isShowing())
                        dialog.dismiss();
                } else {
                    // Failed case:
                    // line 489 command fails on some devices in
                    // that case retrying with accurateCmt as alternative command
//                    if (retry && !isAccurateCut && compressOption == null)
                    if (retry)
                    {
                        Log.d("fdfd", String.valueOf(isAccurateCut));
                        File newFile = new File(outputPath);
                        if (newFile.exists())
                            newFile.delete();
                        execFFmpegBinary(getAccurateCmd(), false);
                    } else {
                        if (dialog.isShowing())
                            dialog.dismiss();
                        runOnUiThread(() ->
                                Toast.makeText(ActVideoTrimmer.this, "Failed to trim", Toast.LENGTH_SHORT).show());
                    }
                }
            }).start();


        } catch (Exception e) {
            Log.d("fdfd", String.valueOf(e.getMessage()));
            e.printStackTrace();
        }
    }


    private String[] getAccurateCmd() {
        return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue)
                , "-i", String.valueOf(uri), "-t",
                TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue),
                "-async", "1", outputPath};
    }

    private void showProcessingDialog() {
        try {
            dialog = new Dialog(this);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.alert_convert);
            TextView txtCancel = dialog.findViewById(R.id.txt_cancel);
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            txtCancel.setOnClickListener(v -> {
                dialog.dismiss();
                FFmpeg.cancel();
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            return checkPermission(
                    Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO);
        } else
            return checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

    }

    private boolean checkPermission(String... permissions) {
        boolean allPermitted = false;
        for (String permission : permissions) {
            allPermitted = (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
            if (!allPermitted)
                break;
        }
        if (allPermitted)
            return true;
        ActivityCompat.requestPermissions(this, permissions,
                PER_REQ_CODE);
        return false;
    }

    private boolean isPermissionOk(int... results) {
        boolean isAllGranted = true;
        for (int result : results) {
            if (PackageManager.PERMISSION_GRANTED != result) {
                isAllGranted = false;
                break;
            }
        }
        return isAllGranted;
    }

    void startProgress() {
        updateSeekbar.run();
    }

    void stopRepeatingTask() {
        seekHandler.removeCallbacks(updateSeekbar);
    }

}
