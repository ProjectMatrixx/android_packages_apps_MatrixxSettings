/*
 * Copyright (C) 2023-2024 The risingOS Android Project
 * Copyright (C) 2024-2025 Lunaris AOSP
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

package com.matrixx.settings.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UDFPSUtils {
    private static final String TAG = "UDFPSUtils";
    private static final int BUFFER_SIZE = 8192;
    private static final int TARGET_SIZE = 512;
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    
    public static String saveUDFPSIconToInternalStorage(Context context, Uri imgUri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        
        try {
            inputStream = getInputStreamFromUri(context, imgUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to get input stream from URI");
                return null;
            }

            String extension = getFileExtension(context, imgUri);
            boolean isAnimated = extension.equalsIgnoreCase(".gif") || 
                               extension.equalsIgnoreCase(".webp");

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "UDFPS_CUSTOM_ICON_" + timeStamp + extension;

            File directory = new File("/sdcard/matrixx/udfps_custom_icon");
            if (!directory.exists() && !directory.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + directory.getAbsolutePath());
                return null;
            }

            deleteOldUDFPSIcons(directory);

            File outputFile = new File(directory, imageFileName);

            if (isAnimated) {
                outputStream = new FileOutputStream(outputFile);
                copyStream(inputStream, outputStream);
            } else {
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap from stream");
                        return null;
                    }
                    
                    bitmap = processUDFPSIcon(bitmap);
                    if (bitmap == null) {
                        Log.e(TAG, "Failed to process UDFPS icon");
                        return null;
                    }
                    
                    outputStream = new FileOutputStream(outputFile);
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)) {
                        Log.e(TAG, "Failed to compress bitmap");
                        return null;
                    }
                } finally {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            }

            Log.d(TAG, "UDFPS icon saved successfully: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving UDFPS icon: " + e.getMessage(), e);
            return null;
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
        }
    }

    private static Bitmap processUDFPSIcon(Bitmap source) {
        if (source == null) return null;

        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap scaledBitmap = null;
        
        // Scale down if needed
        if (size > TARGET_SIZE) {
            scaledBitmap = Bitmap.createScaledBitmap(source, TARGET_SIZE, TARGET_SIZE, true);
            source = scaledBitmap;
            size = TARGET_SIZE;
        }

        // Create circular crop in single pass
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        // Draw circular background
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        
        // Apply source-in blend mode for circular crop
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, (size - source.getWidth()) / 2f, 
                                (size - source.getHeight()) / 2f, paint);
        
        // Clean up intermediate scaled bitmap if created
        if (scaledBitmap != null) {
            scaledBitmap.recycle();
        }
        
        return output;
    }

    private static InputStream getInputStreamFromUri(Context context, Uri imgUri) throws IOException {
        if (imgUri.toString().startsWith("content://com.google.android.apps.photos.contentprovider")) {
            List<String> segments = imgUri.getPathSegments();
            if (segments.size() > 2) {
                String mediaUriString = URLDecoder.decode(segments.get(2), StandardCharsets.UTF_8.name());
                Uri mediaUri = Uri.parse(mediaUriString);
                return context.getContentResolver().openInputStream(mediaUri);
            } else {
                throw new FileNotFoundException("Failed to parse Google Photos content URI");
            }
        } else {
            return context.getContentResolver().openInputStream(imgUri);
        }
    }

    private static void copyStream(InputStream input, FileOutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long totalBytes = 0;
        
        while ((bytesRead = input.read(buffer)) != -1) {
            totalBytes += bytesRead;
            
            // Enforce file size limit
            if (totalBytes > MAX_FILE_SIZE) {
                throw new IOException("File size exceeds " + (MAX_FILE_SIZE / 1024 / 1024) + "MB limit");
            }
            
            output.write(buffer, 0, bytesRead);
        }
        output.flush();
    }

    private static void deleteOldUDFPSIcons(File directory) {
        File[] files = directory.listFiles((dir, name) -> 
                name.startsWith("UDFPS_CUSTOM_ICON_") && 
                (name.endsWith(".png") || name.endsWith(".gif") || 
                 name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                 name.endsWith(".webp")));
        
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    Log.w(TAG, "Failed to delete old UDFPS icon: " + file.getName());
                }
            }
        }
    }

    private static String getFileExtension(Context context, Uri uri) {
        String extension = ".png";

        if ("content".equals(uri.getScheme())) {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.contains("gif")) {
                    return ".gif";
                } else if (mimeType.contains("webp")) {
                    return ".webp";
                } else if (mimeType.contains("jpeg")) {
                    return ".jpg";
                } else if (mimeType.contains("png")) {
                    return ".png";
                }
            }
        }

        return extension;
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing stream: " + e.getMessage());
            }
        }
    }
}
