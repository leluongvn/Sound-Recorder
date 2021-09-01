package com.tech.dev.record;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Custom Cursor adapter used for listview population and view items clicked actions
 */

public class RecordsCursorAdapter extends CursorAdapter {

    AppCompatActivity context;
    FragmentManager fragmentManager;
    private static final String TAG = "RecordsCursorAdapter";

    public RecordsCursorAdapter(AppCompatActivity context, Cursor c, FragmentManager fragmentManager) {
        super(context, c, 0);
        this.fragmentManager = fragmentManager;
        this.context = context;
    }

    String AudioPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "voices/";

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.single_list_item, parent, false);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void bindView(View view, final Context context, Cursor mCursor) {
        TextView name = (TextView) view.findViewById(R.id.audio_name);
        TextView details = (TextView) view.findViewById(R.id.audio_details);
        ImageButton option = (ImageButton) view.findViewById(R.id.option);
        final ImageButton play = (ImageButton) view.findViewById(R.id.list_play);
        final String displayName = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
        final long durationMs = Long.parseLong(mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
        Date date = new Date(durationMs);
        DateFormat formatter = new SimpleDateFormat("mm:ss");
        String dateFormatted = formatter.format(date);
        long size = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
        String sizeKb = String.valueOf(size / 1024);
        name.setText(displayName);
        details.setText("duration: " + dateFormatted + " size: " + sizeKb + "kb");

        //start playing an audio related to item clicked on list of audios with in dialog fragment (PlayerDialog)
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String currentAudioPath = AudioPath + displayName;
                PlayerDialog dialog = PlayerDialog.newInstance(displayName, durationMs, currentAudioPath);
                dialog.show(fragmentManager, "Path");
            }
        });
        //start playing an audio on play button of list clicked with in dialog fragment (PlayerDialog)
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String currentAudioPath = AudioPath + displayName;
                PlayerDialog dialog = PlayerDialog.newInstance(displayName, durationMs, currentAudioPath);
                dialog.show(fragmentManager, "Path");
            }
        });

        //selecting delete or edit audio name by option icon on listview clicked

        option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_remove_list:
                                deleteDialog(displayName);
                                break;
                            case R.id.action_edit:
                                int lenght = displayName.length();

                                String subName = displayName.substring(0, lenght - 4);
                                renameDialog(subName);
                                break;
                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.option_menu);
                popupMenu.show();

            }
        });

    }


    //dialog show from option menu Rename item clicked for renaming the audio name
    public void renameDialog(final String currentName) {

        Log.e("currentName:", currentName);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Edit Name");
        LayoutInflater inflater = context.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dailog_view, null);
        alertDialog.setView(dialogView);
        final EditText input = (EditText) dialogView.findViewById(R.id.dailog_title);
        input.setText(currentName);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pathfile = Environment.getExternalStorageDirectory() + File.separator + "voices/";
                ContentValues values = new ContentValues();
                File sdCard = new File(pathfile);
                final File from = new File(sdCard, currentName + ".wav");
                final File to = new File(sdCard, input.getText().toString() + ".wav");
                context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.DATA + "=?", new String[]{String.valueOf(from)});
                from.renameTo(to);
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, input.getText().toString() + ".wav");
                context.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values, MediaStore.Audio.Media.DATA + "=?", new String[]{pathfile + currentName + ".wav"});
                MediaScannerConnection.scanFile(context, new String[]{to.getPath()},
                        null, new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.e("path:", "Scan is completed" + String.valueOf(to));
                            }
                        });

            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        alertDialog.show();
    }

    //dialog show to remove the audio file selected from the list through option menu icon
    public void deleteDialog(final String currentAudio) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Delete");
        alertDialog.setMessage(currentAudio);
        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pathfile = Environment.getExternalStorageDirectory() + File.separator + "voices/";
                context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.DATA + "=?", new String[]{pathfile + currentAudio});
                final File f = new File(pathfile + currentAudio);
                f.delete();
                MediaScannerConnection.scanFile(context, new String[]{f.getPath()},
                        null, new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.e("path:", "Scan is completed" + String.valueOf(f));
                            }
                        });
            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

}
