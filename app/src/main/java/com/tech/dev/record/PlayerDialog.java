package com.tech.dev.record;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.IOException;

/**
 * Dialog used for playing,seek and pause an audio
 */

public class PlayerDialog extends DialogFragment {
  String name,path;
    long duration;
    private Handler mHandler = new Handler();
    private MediaPlayer mMediaPlayer = null;
    private static final String LOG_TAG = "PlaybackFragment";
    private SeekBar mSeekBar = null;
    private FloatingActionButton mPlayButton = null;
    private TextView mCurrentProgressTextView = null;
    private TextView mFileNameTextView = null;
    private TextView mFileLengthTextView = null;
    private boolean isPlaying = false;
    long minutes = 0;
    long seconds = 0;

    public static PlayerDialog newInstance(String name,long duration,String path){
        PlayerDialog playerDialog=new PlayerDialog();
        Bundle bundle=new Bundle();
        bundle.putString("Name",name);
        bundle.putLong("Duration",duration);
        bundle.putString("Path",path);
        playerDialog.setArguments(bundle);
        return playerDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        name=getArguments().getString("Name");
        path=getArguments().getString("Path");
        duration=getArguments().getLong("Duration");
        minutes= java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(duration);
        seconds= java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(duration)- java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);

    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog=super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        LayoutInflater inflater=getActivity().getLayoutInflater();
        View rootView=inflater.inflate(R.layout.dialog_fragment,null);
        mFileNameTextView= (TextView) rootView.findViewById(R.id.dialog_txt);
        mFileLengthTextView= (TextView) rootView.findViewById(R.id.dialog_start_end);
        mCurrentProgressTextView= (TextView) rootView.findViewById(R.id.dialog_start_time);
        mPlayButton = (FloatingActionButton) rootView.findViewById(R.id.fab_play);
        mSeekBar= (SeekBar) rootView.findViewById(R.id.seekbar);

        if (mMediaPlayer!=null){
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }catch (Exception e){}

        }
        onPlay(isPlaying);
        isPlaying = !isPlaying;

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mMediaPlayer != null && fromUser) {
                    mMediaPlayer.seekTo(progress);
                    mHandler.removeCallbacks(mRunnable);
                   long minutes= java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                   long seconds= java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())- java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes,seconds));

                    updateSeekBar();

                } else if (mMediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress);
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(mMediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(mRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mMediaPlayer.seekTo(seekBar.getProgress());
                    long minutes= java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds= java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())- java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes,seconds));
                    updateSeekBar();
                }
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(isPlaying);
                isPlaying = !isPlaying;
            }
        });
        mFileNameTextView.setText(name);
        mFileLengthTextView.setText(String.format("%02d:%02d", minutes,seconds));
        builder.setView(rootView);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return  builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            stopPlaying();
        }
    }

    // Play start/stop
    private void onPlay(boolean isPlaying){
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if(mMediaPlayer == null) {
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
        mPlayButton.setImageResource(R.mipmap.ic_pause);
        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaying();
            }
        });

        updateSeekBar();

        //keep screen on while playing audio
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void prepareMediaPlayerFromPoint(int progress) {
        //set mediaPlayer to start from middle of the audio file

        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(progress);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        //keep screen on while playing audio
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void pausePlaying() {
        mPlayButton.setImageResource(R.mipmap.ic_play);
        mHandler.removeCallbacks(mRunnable);
try {
    mMediaPlayer.pause();
}catch (Exception e){}

    }

    private void resumePlaying() {
        mPlayButton.setImageResource(R.mipmap.ic_pause);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.start();
        updateSeekBar();
    }

    private void stopPlaying() {
        mPlayButton.setImageResource(R.mipmap.ic_play);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;

        mSeekBar.setProgress(mSeekBar.getMax());
        isPlaying = !isPlaying;

        mCurrentProgressTextView.setText(mFileLengthTextView.getText());
        mSeekBar.setProgress(mSeekBar.getMax());

        //allow the screen to turn off again once audio is finished playing
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //updating mSeekBar
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(mMediaPlayer != null){

                int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);

                long minutes= java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition);
                long seconds= java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition)- java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
                mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes, seconds));

                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        mHandler.postDelayed(mRunnable, 1000);

    }
}
