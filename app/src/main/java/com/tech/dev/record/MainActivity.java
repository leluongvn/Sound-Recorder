package com.tech.dev.record;


import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


/*
 * This is the main screen activity of audio recorder app from where user
 * can record an audio,can play the audio recorded previously and can nevigate to
 * the audio recorded saved list.
 * */

public class MainActivity extends AppCompatActivity {

    ImageButton buttonStart, buttonStop, buttonList, buttonCancel, buttonPlaySeek;
    private static final String TAG = "MainActivity";
    ImageButton playBig, pauseBig;
    private AdView mBannerAd;
    Chronometer chronometer;
    SeekBar mSeekBar;
    String mAudioSavePath = null;
    TextView textView, txtStatus;
    MediaRecorder mediaRecorder;
    Random random;
    boolean playerPause = false;
    boolean isRecording = false;
    long timeWhenStopped = 0;
    boolean recorderStop = false;
    boolean recorderPause = false;
    String previousAudioPath = null;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer = null;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    private Handler mHandler = new Handler();
    private boolean isPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponent();
//        showBannerAd();
        if (!checkPermission()) {
            requestPermission();
        }
        pref = getPreferences(MODE_PRIVATE);
        editor = pref.edit();

        if (mAudioSavePath == null) {
            mAudioSavePath = pref.getString("LastPlayedPath", null);
        }

        buttonPlaySeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setEnabled(true);
                onPlay(isPlaying);
                isPlaying = !isPlaying;

            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                    mHandler.removeCallbacks(mRunnable);
                    long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                    long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition()) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
                    chronometer.setText(String.format("00:%02d:%02d", minutes, seconds));

                    updateSeekBar();

                } else if (mediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress);
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(mRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mediaPlayer.seekTo(seekBar.getProgress());
                    long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                    long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition()) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
                    chronometer.setText(String.format("00:%02d:%02d", minutes, seconds));
                    updateSeekBar();
                }
            }
        });


        playBig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playerPause && !recorderPause) {
                    playerPause = false;
                    playBig.setVisibility(View.INVISIBLE);
                    pauseBig.setVisibility(View.VISIBLE);
                    long timeWhenStopped = 0;
                    int length = mediaPlayer.getCurrentPosition();
                    mediaPlayer.seekTo(length);
                    mediaPlayer.start();
                    timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
                    chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                    chronometer.start();
                    txtStatus.setText("Playing");
                    txtStatus.clearAnimation();
                } else if (recorderPause) {
                    chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                    try {
//                        mediaRecorder.resume();
                        recorderPause = false;
                        chronometer.start();
                    } catch (Exception e) {
                    }
                    pauseBig.setVisibility(View.VISIBLE);
                    playBig.setVisibility(View.INVISIBLE);
                    txtStatus.clearAnimation();
                    txtStatus.setText("Recording Resumed");
                }
            }
        });
        pauseBig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (mediaRecorder != null && !recorderStop) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
                        chronometer.stop();
                        try {
                            mediaRecorder.pause();
                            recorderPause = true;
                            chronometer.stop();
                        } catch (Exception e) {
                        }
                        pauseBig.setVisibility(View.INVISIBLE);
                        playBig.setVisibility(View.VISIBLE);
                        txtStatus.setText("Recording Paused");
                        Animation anim = new AlphaAnimation(0.0f, 1.0f);
                        anim.setDuration(50); //You can manage the time of the blink with this parameter
                        anim.setStartOffset(20);
                        anim.setRepeatMode(Animation.REVERSE);
                        anim.setRepeatCount(Animation.INFINITE);
                        txtStatus.startAnimation(anim);
                        playerPause = true;
                    } else {
                        Toast.makeText(MainActivity.this, "Pause feature works on API level 24 or higher", Toast.LENGTH_SHORT).show();
                    }

                }

            }
        });


        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startRecording();
            }
        });


        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSeekBar.setEnabled(true);
                isRecording = false;
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.setText("00:00:00");
                buttonStop.setVisibility(View.INVISIBLE);
                buttonStart.setVisibility(View.VISIBLE);
                pauseBig.setVisibility(View.GONE);
                playBig.setVisibility(View.GONE);
                buttonPlaySeek.setVisibility(View.VISIBLE);
                buttonCancel.setEnabled(true);
                isRecording = false;
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                } catch (Exception e) {
                }
                ;
                txtStatus.setText("Recording stopped");
                txtStatus.clearAnimation();
                recorderStop = true;

            }
        });


        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    mSeekBar.setEnabled(true);
                    try {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        mediaRecorder = null;
                    } catch (Exception e) {
                    }

                    editor.putString("LastPlayedPath", previousAudioPath);
                    editor.commit();
                    File notToSave = new File(mAudioSavePath);
                    notToSave.delete();
                    getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.DATA + "=?", new String[]{mAudioSavePath});
                    chronometer.stop();
                    txtStatus.setText("Recording Cancelled");
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.setText("00:00:00");
                    buttonStop.setVisibility(View.INVISIBLE);
                    buttonStart.setVisibility(View.VISIBLE);
                    playBig.setVisibility(View.GONE);
                    pauseBig.setVisibility(View.GONE);
                    buttonPlaySeek.setVisibility(View.VISIBLE);
                    buttonCancel.setEnabled(false);
                    isRecording = false;
                    mAudioSavePath = previousAudioPath;
                   /* if (mediaRecorder!=null)
                    {
                        try{
                            mediaRecorder.stop();
                            mediaRecorder.release();
                        }catch (Exception e){}
                    }*/
                }
            }
        });
///

        //// sub
        buttonList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    try {
                        stopPlaying();
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    } catch (Exception e) {
                    }
                    final File currentAudio = new File(mAudioSavePath);
                    scanFile(currentAudio);
                }
                if (mediaRecorder != null) {
                    try {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        mediaRecorder = null;
                    } catch (Exception e) {
                    }


                }

                buttonStop.setVisibility(View.INVISIBLE);
                buttonStart.setVisibility(View.VISIBLE);
                playerPause = false;
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.setText("00:00:00");
                pauseBig.setVisibility(View.INVISIBLE);
                playBig.setVisibility(View.INVISIBLE);
                buttonPlaySeek.setVisibility(View.VISIBLE);

                // check sub
                SharedPreferences prefs = getSharedPreferences(
                        "PremiumApp",
                        MODE_PRIVATE
                );
                boolean isPremium = prefs.getBoolean("premium", false);
                if (isPremium) {
                    Intent intent = new Intent(MainActivity.this, RecordList.class);
                    startActivity(intent);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("You want buy premium ?");
                    builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainActivity.this, SubActivity.class);
                            startActivity(intent);
                        }
                    });
                    builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.show();

                }

            }
        });

    }

    /*
     * Scanning the file for adding new file and deleting file
     * */


    public void scanFile(final File scanFile) {
        MediaScannerConnection.scanFile(MainActivity.this, new String[]{scanFile.getPath()},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {

                    }

                });
    }
    /*
     * Initlaization of components
     * */

    public void initComponent() {
        mBannerAd = (AdView) findViewById(R.id.banner_AdView);
        buttonPlaySeek = (ImageButton) findViewById(R.id.play_seek);
        buttonStart = (ImageButton) findViewById(R.id.record);
        buttonStop = (ImageButton) findViewById(R.id.stop_recording);
        buttonCancel = (ImageButton) findViewById(R.id.cancel_recording);
        buttonList = (ImageButton) findViewById(R.id.list_button);
        textView = (TextView) findViewById(R.id.time);
        mSeekBar = (SeekBar) findViewById(R.id.main_seek_bar);
        mSeekBar.setEnabled(false);
        playBig = (ImageButton) findViewById(R.id.play_main_big);
        pauseBig = (ImageButton) findViewById(R.id.pause_main_big);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        txtStatus = (TextView) findViewById(R.id.txt_status);
        buttonStop.setVisibility(View.INVISIBLE);
        pauseBig.setVisibility(View.INVISIBLE);
        playBig.setVisibility(View.INVISIBLE);
        buttonCancel.setEnabled(false);
        random = new Random();
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer cArg) {
                long time = SystemClock.elapsedRealtime() - cArg.getBase();
                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                String hh = h < 10 ? "0" + h : h + "";
                String mm = m < 10 ? "0" + m : m + "";
                String ss = s < 10 ? "0" + s : s + "";
                cArg.setText(hh + ":" + mm + ":" + ss);
            }
        });
        chronometer.setText("00:00:00");


    }

    /*
     * Start recording
     * */

    public void startRecording() {
        if (checkPermission()) {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                } catch (Exception e) {
                }

            }
            mSeekBar.setProgress(0);
            mSeekBar.setEnabled(false);
            buttonCancel.setEnabled(true);
            buttonPlaySeek.setVisibility(View.INVISIBLE);
            buttonStop.setVisibility(View.VISIBLE);
            buttonStart.setVisibility(View.INVISIBLE);
            pauseBig.setVisibility(View.VISIBLE);
            playBig.setVisibility(View.INVISIBLE);
            File audioStorage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "voices");
            if (!audioStorage.exists()) {
                audioStorage.mkdir();
            }
            isRecording = true;
            mAudioSavePath =
                    Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "voices/" +
                            CreateRandomAudioFileName(5) + "AudioRecord.wav";
            final File currentAudio = new File(mAudioSavePath);
            scanFile(currentAudio);
            previousAudioPath = pref.getString("LastPlayedPath", null);
            editor.putString("LastPlayedPath", mAudioSavePath);
            editor.commit();
            MediaRecorderReady();
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                } catch (Exception e) {
                }
            }
            try {

                mediaRecorder.prepare();
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
                mediaRecorder.start();
            } catch (IllegalStateException e) {

                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();
            }


            txtStatus.setText("Recording in progress");
        } else {
            requestPermission();
        }
    }


    /*
     * Random name generation for audio file
     * */

    public String CreateRandomAudioFileName(int string) {
        StringBuilder stringBuilder = new StringBuilder(string);
        int i = 0;
        while (i < string) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++;
        }
        return stringBuilder.toString();
    }

    /*
     * MediaRecorder configuration e.g format of audio e.t.c
     * */

    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(mAudioSavePath);
    }

    /*
     * Pop up notification for requesting media permission on marshmallow versions and above
     * */

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
            }
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
            }
        }
    }

    private void onPlay(boolean isPlaying) {
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if (mediaPlayer == null) {
                startPlaying(); //start from beginning
            } else {
                resumePlaying(); //resume the currently paused MediaPlayer
            }

        } else {
            //pause the MediaPlayer
            pausePlaying();
        }
    }

    private void startPlaying() {
        buttonPlaySeek.setImageResource(R.drawable.pause_icon_big);
        mediaPlayer = new MediaPlayer();


        if (mAudioSavePath != null) {

            try {
                mediaPlayer.setDataSource(mAudioSavePath);
                mediaPlayer.prepare();
                mSeekBar.setMax(mediaPlayer.getDuration());

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        txtStatus.setText("Audio Playing");
                        mediaPlayer.start();
                    }
                });
            } catch (IOException e) {
            }
        } else {
            txtStatus.setText("No Records Found");
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                txtStatus.setText("Audio Played");
                stopPlaying();
            }
        });

        updateSeekBar();

        //keep screen on while playing audio
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void prepareMediaPlayerFromPoint(int progress) {
        //set mediaPlayer to start from middle of the audio file
        if (mAudioSavePath == null) {
            mAudioSavePath = pref.getString("LastPlayedPath", null);
        } else if (mAudioSavePath != null) {
            mediaPlayer = new MediaPlayer();

            try {
                mediaPlayer.setDataSource(mAudioSavePath);
                mediaPlayer.prepare();
                mSeekBar.setMax(mediaPlayer.getDuration());
                mediaPlayer.seekTo(progress);

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopPlaying();
                    }
                });

            } catch (IOException e) {
            }

            //keep screen on while playing audio
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            txtStatus.setText("No Records Found");
        }
    }

    private void pausePlaying() {
        buttonPlaySeek.setImageResource(R.drawable.play_icon_big);
        mHandler.removeCallbacks(mRunnable);
        try {
            mediaPlayer.pause();
            txtStatus.setText("Audio Paused");
        } catch (Exception e) {
        }

    }

    private void resumePlaying() {
        buttonPlaySeek.setImageResource(R.drawable.pause_icon_big);
        mHandler.removeCallbacks(mRunnable);
        mediaPlayer.start();
        txtStatus.setText("Audio Resumed");
        updateSeekBar();
    }

    private void stopPlaying() {
        buttonPlaySeek.setImageResource(R.drawable.play_icon_big);
        mHandler.removeCallbacks(mRunnable);
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;

        mSeekBar.setProgress(mSeekBar.getMax());
        isPlaying = !isPlaying;

        /* mCurrentProgressTextView.setText(mFileLengthTextView.getText());*/
        mSeekBar.setProgress(mSeekBar.getMax());

        //allow the screen to turn off again once audio is finished playing
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //updating mSeekBar
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {

                int mCurrentPosition = mediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);
                long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition());
                long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition()) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
                chronometer.setText(String.format("00:%02d:%02d", minutes, seconds));
                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        mHandler.postDelayed(mRunnable, 1000);
    }

    //    private void showBannerAd() {
//        AdRequest adRequest = new AdRequest.Builder()
//                .addTestDevice("754DB6521943676637AE86202C5ACE52")
//                .build();
//        mBannerAd.loadAd(adRequest);
//
//    }


}
