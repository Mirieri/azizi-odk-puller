package org.cgiar.ilri.odk.pull.backend.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.cgiar.ilri.odk.pull.backend.carriers.Form;

import java.io.File;

/**
 * Created by jrogena on 07/05/15.
 */
class DatabaseContext extends ContextWrapper {

    private static final String TAG = "DatabaseContext";
    private final String directory;
    public DatabaseContext(Context context, String directory) {
        super(context);
        this.directory = directory;
    }

    @Override
    public File getDatabasePath(String name) {
        String dbfile = directory + File.separator + name;
        if (!dbfile.endsWith(Form.SUFFIX_DB)) {
            dbfile += Form.SUFFIX_DB;
        }
        File result = new File(dbfile);
        if (!result.getParentFile().exists()) {
            result.getParentFile().mkdirs();
        }
        Log.w(TAG, "Creating " + name + " in " + result.getAbsolutePath());
        return result;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        return result;
    }
}
