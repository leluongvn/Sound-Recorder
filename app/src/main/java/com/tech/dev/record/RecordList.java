package com.tech.dev.record;


import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

/*
* Populating the list view through cursor adapter
* */

public class RecordList extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final int AUDIO_LOADER=1;
    ListView mListView;
    RecordsCursorAdapter recordsCursorAdapter;
    private static final String TAG = "RecordList";
    FragmentManager fragmentManager=getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);
        mListView = (ListView) findViewById(R.id.recording_list);
        recordsCursorAdapter=new RecordsCursorAdapter(this,null,fragmentManager);
        mListView.setAdapter(recordsCursorAdapter);
        TextView empty= (TextView) findViewById(R.id.empty);
        recordsCursorAdapter.notifyDataSetChanged();
        mListView.setEmptyView(empty);
        empty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(RecordList.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        String AudioPath= Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "voices/" ;
        String selection = MediaStore.Audio.Media.DATA +" LIKE '%/voices/%'";
        String[] projection=new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.SIZE};
        Cursor audioCursor=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null);
        if (audioCursor!=null) {
            if (audioCursor.moveToFirst()) {
                do {
                    Log.e(TAG,""+audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                    long duration=audioCursor.getLong(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    String name=audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                    File file=new File(AudioPath+name);
                    if (duration==0) {
                        scanFile(file);
                    }
                }while (audioCursor.moveToNext());
            }
        }
        getSupportLoaderManager().initLoader(AUDIO_LOADER,null,this);
    }

    public void scanFile(final File scanFile){
        MediaScannerConnection.scanFile(RecordList.this, new String[]{scanFile.getPath()},
                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.e(TAG,"scaned");
                    }

                });
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String selection = MediaStore.Audio.Media.DATA +" LIKE '%/voices/%'";

        return new CursorLoader(this,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                selection,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor mCursor) {
    recordsCursorAdapter.swapCursor(mCursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    recordsCursorAdapter.swapCursor(null);
    }
}
