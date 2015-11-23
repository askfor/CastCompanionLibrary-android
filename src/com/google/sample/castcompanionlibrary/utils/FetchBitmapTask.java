/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
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

package com.google.sample.castcompanionlibrary.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.bumptech.glide.Glide;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * An AsyncTask to fetch an image over HTTP and scale it to a desired size. Clients need to extend
 * this; it is recommended to start the execution by calling {@code start} rather than
 * {@code execute} to handle a uniform treatment of ThreadPool across various versions of Android.
 */
public abstract class FetchBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
    private int mPreferredWidth;
    private int mPreferredHeight;
    private Context mContext;

    /**
     * Constructs a new FetchBitmapTask that will do scaling.
     *
     * @param preferredWidth The preferred image width.
     * @param preferredHeight The preferred image height.
     */
    public FetchBitmapTask(Context context, int preferredWidth, int preferredHeight) {
        mPreferredWidth = preferredWidth;
        mPreferredHeight = preferredHeight;
        mContext = context;
    }

    /**
     * Constructs a new FetchBitmapTask. No scaling will be performed if you use this constructor.
     */
    public FetchBitmapTask(Context context) {
        this(context, 0, 0);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        mPreferredWidth = width;
        mPreferredHeight = height;
    }

    @Override
    protected Bitmap doInBackground(Uri... uris) {
        if (uris.length != 1 || uris[0] == null) {
            return null;
        }

        Bitmap bitmap = null;
        URL url = null;
        try {
            url = new URL(uris[0].toString());
        } catch (MalformedURLException e) {
            return null;
        }

        int w, h;
        if((mPreferredWidth > 0) && (mPreferredHeight > 0)) {
            w = mPreferredWidth;
            h = mPreferredHeight;
        }else{
            w = -1;
            h = -1;
        }
        try {
            Bitmap img = Glide.with(mContext).load(url).asBitmap().into(w, h).get();
            return img;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Executes the task. This is a simple wrapper for {@code execute())} method and is introduced
     * to get around the size of thread pool in various versions of Android. Since {@code execute()}
     * is {@code final}, we need to introduce another method.
     * @param params
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void start(Uri...params) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            execute(params);
        }
    }

    /*
     * Scales the bitmap to the preferred width and height.
     *
     * @param bitmap The bitmap to scale.
     * @return The scaled bitmap.
     */
    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate deltas.
        int dw = width - mPreferredWidth;
        int dh = height - mPreferredHeight;

        if ((dw == 0) && (dh == 0)) {
            return bitmap;
        }

        float scaleFactor = 0.0f;
        if ((dw > 0) || (dh > 0)) {
            // Icon is too big; scale down.
            float scaleWidth = (float) mPreferredWidth / width;
            float scaleHeight = (float) mPreferredHeight / height;
            scaleFactor = Math.min(scaleHeight, scaleWidth);
        } else {
            // Icon is too small; scale up.
            float scaleWidth = width / (float) mPreferredWidth;
            float scaleHeight = height / (float) mPreferredHeight;
            scaleFactor = Math.min(scaleHeight, scaleWidth);
        }

        int finalWidth = (int) ((width * scaleFactor) + 0.5f);
        int finalHeight = (int) ((height * scaleFactor) + 0.5f);

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, false);
    }

}
