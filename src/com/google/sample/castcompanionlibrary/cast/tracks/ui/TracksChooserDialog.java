/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.castcompanionlibrary.cast.tracks.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaTrack;
import com.google.sample.castcompanionlibrary.R;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

/**
 * A dialog to show the available tracks (Text and Audio) to allow selection of tracks.
 */
public class TracksChooserDialog extends DialogFragment {

    private static final String TAG = "TracksChooserDialog";
    private VideoCastManager mCastManager;
    private long[] mActiveTracks = null;
    private MediaInfo mMediaInfo;
    private TracksListAdapter mTextAdapter;
    private TracksListAdapter mAudioVideoAdapter;
    private List<MediaTrack> mTextTracks = new ArrayList<MediaTrack>();
    private List<MediaTrack> mAudioTracks = new ArrayList<MediaTrack>();
    private static final long TEXT_TRACK_NONE_ID = -1;
    private int mSelectedTextPosition = 0;
    private int mSelectedAudioPosition = -1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.custom_tracks_dialog_layout, null);
        setupView(view);

        builder.setView(view)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        List<MediaTrack> selectedTracks = new ArrayList<MediaTrack>();
                        MediaTrack textTrack = mTextAdapter.getSelectedTrack();
                        if (textTrack.getId() != TEXT_TRACK_NONE_ID) {
                            selectedTracks.add(textTrack);
                        }
                        MediaTrack audioVideoTrack = mAudioVideoAdapter.getSelectedTrack();
                        if (null != audioVideoTrack) {
                            selectedTracks.add(audioVideoTrack);
                        }
                        mCastManager.notifyTracksSelectedListeners(selectedTracks);
                        TracksChooserDialog.this.getDialog().cancel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TracksChooserDialog.this.getDialog().cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        TracksChooserDialog.this.getDialog().cancel();
                    }
                });

        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle mediaWrapper = getArguments().getBundle(VideoCastManager.EXTRA_MEDIA);
        mMediaInfo = Utils.toMediaInfo(mediaWrapper);
        try {
            mCastManager = VideoCastManager.getInstance();
            mActiveTracks = mCastManager.getActiveTrackIds();
            List<MediaTrack> allTracks = mMediaInfo.getMediaTracks();
            if (allTracks == null || allTracks.isEmpty()) {
                Utils.showToast(getActivity(), R.string.caption_no_tracks_available);
                dismiss();
            }
        } catch (CastException e) {
            LOGE(TAG, "Failed to get an instance of VideoCatManager", e);
        }
    }

    /**
     * This is to get around the following bug:
     * https://code.google.com/p/android/issues/detail?id=17423
     */
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private void setupView(View view) {
        ListView listView1 = (ListView) view.findViewById(R.id.listview1);
        ListView listView2 = (ListView) view.findViewById(R.id.listview2);
        TextView textEmptyMessageView = (TextView) view.findViewById(R.id.text_empty_message);
        TextView audioEmptyMessageView = (TextView) view.findViewById(R.id.audio_empty_message);
        partitionTracks();

        mTextAdapter = new TracksListAdapter(getActivity(), R.layout.tracks_row_layout,
                mTextTracks, mSelectedTextPosition);
        mAudioVideoAdapter = new TracksListAdapter(getActivity(), R.layout.tracks_row_layout,
                mAudioTracks, mSelectedAudioPosition);

        listView1.setAdapter(mTextAdapter);
        listView2.setAdapter(mAudioVideoAdapter);

        TabHost tabs = (TabHost) view.findViewById(R.id.tabhost);
        tabs.setup();

        // create tab 1
        TabHost.TabSpec tab1 = tabs.newTabSpec("tab1");
        if (mTextTracks == null || mTextTracks.isEmpty()) {
            listView1.setVisibility(View.INVISIBLE);
            tab1.setContent(R.id.text_empty_message);
        } else {
            textEmptyMessageView.setVisibility(View.INVISIBLE);
            tab1.setContent(R.id.listview1);
        }
        tab1.setIndicator(getString(R.string.caption_subtitles));
        tabs.addTab(tab1);

        // create tab 2
        TabHost.TabSpec tab2 = tabs.newTabSpec("tab2");
        if (mAudioTracks == null || mAudioTracks.isEmpty()) {
            listView2.setVisibility(View.INVISIBLE);
            tab2.setContent(R.id.audio_empty_message);
        } else {
            audioEmptyMessageView.setVisibility(View.INVISIBLE);
            tab2.setContent(R.id.listview2);
        }
        tab2.setIndicator(getString(R.string.caption_audio));
        tabs.addTab(tab2);
    }

    private MediaTrack buildNoneTrack() {
        return new MediaTrack.Builder(TEXT_TRACK_NONE_ID,
                MediaTrack.TYPE_TEXT)
                .setName(getString(R.string.none))
                .setSubtype(MediaTrack.SUBTYPE_CAPTIONS)
                .setContentId("").build();
    }

    private void partitionTracks() {
        List<MediaTrack> allTracks = mMediaInfo.getMediaTracks();
        mAudioTracks.clear();
        mTextTracks.clear();
        mTextTracks.add(buildNoneTrack());
        mSelectedTextPosition = 0;
        mSelectedAudioPosition = -1;
        if (allTracks != null) {
            int textPosition = 1; /* start from 1 since we have a NONE selection at the beginning */
            int audioPosition = 0;
            for (MediaTrack track : allTracks) {
                switch (track.getType()) {
                    case MediaTrack.TYPE_TEXT:
                        mTextTracks.add(track);
                        if (mActiveTracks != null) {
                            for(int i=0; i < mActiveTracks.length; i++) {
                                if (mActiveTracks[i] == track.getId()) {
                                    mSelectedTextPosition = textPosition;
                                }
                            }
                        }
                        textPosition++;
                        break;
                    case MediaTrack.TYPE_AUDIO:
                        mAudioTracks.add(track);
                        if (mActiveTracks != null) {
                            for(int i=0; i < mActiveTracks.length; i++) {
                                if (mActiveTracks[i] == track.getId()) {
                                    mSelectedAudioPosition = audioPosition;
                                }
                            }
                        }
                        audioPosition++;
                        break;
                }
            }
        }
    }

    /**
     * Call this static method to create a new instance of the dialog.
     */
    public static TracksChooserDialog newInstance(MediaInfo mediaInfo) {
        TracksChooserDialog fragment = new TracksChooserDialog();
        Bundle bundle = new Bundle();
        bundle.putBundle(VideoCastManager.EXTRA_MEDIA, Utils.fromMediaInfo(mediaInfo));
        fragment.setArguments(bundle);
        return fragment;
    }
}
