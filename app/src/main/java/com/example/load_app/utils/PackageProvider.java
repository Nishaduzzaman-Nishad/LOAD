package com.example.load_app.utils;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PackageProvider extends ContentProvider {

    // This is the unique identifier for our provider (Must match the Manifest)
    public static final String AUTHORITY = "com.example.load_app.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/packages");

    // Column names for the external apps to query
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PACKAGE_ID = "package_id";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_PRIORITY = "priority";

    // URI Matcher to determine what the external app is asking for
    private static final int PACKAGES = 1;
    private static final int PACKAGE_ID = 2;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "packages", PACKAGES);
        uriMatcher.addURI(AUTHORITY, "packages/#", PACKAGE_ID);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        // We use a MatrixCursor to return data instantly without needing SQLite
        String[] columns = {COLUMN_ID, COLUMN_PACKAGE_ID, COLUMN_STATUS, COLUMN_PRIORITY};
        MatrixCursor cursor = new MatrixCursor(columns);

        switch (uriMatcher.match(uri)) {
            case PACKAGES:
                // For demonstration: returning some mock/active packages
                // In a real app, you would read this from your Repository/Local Cache
                cursor.addRow(new Object[]{1, "PKG-1001", "PENDING", "URGENT"});
                cursor.addRow(new Object[]{2, "PKG-1002", "ASSIGNED", "NORMAL"});
                cursor.addRow(new Object[]{3, "PKG-1003", "DELIVERED", "NORMAL"});
                break;
            case PACKAGE_ID:
                // Extracting the ID from the URI (e.g., content://.../packages/1)
                String id = uri.getLastPathSegment();
                cursor.addRow(new Object[]{id, "PKG-" + id + "00", "ASSIGNED", "URGENT"});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        return cursor;
    }

    // We will leave these unimplemented since this is a read-only provider
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}
