/*
 Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team and does not follow any standards at all ;). Server side code used by this application is in another repository. You can however reverse engineer this app or simply contact me if you feel like you have to use this App. (C) 2015 Jason Rogena <j.rogena@cgiar.org>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cgiar.ilri.odk.pull.backend.storage;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.cgiar.ilri.odk.pull.backend.carriers.Form;

import java.io.File;

/**
 * This Class implements an database context allowing for databases to be created in custom directories
 *
 * Created by Jason Rogena <j.rogena@cgiar.org> on 7th May 2015.
 */
class DatabaseContext extends ContextWrapper {

    private static final String TAG = "ODKPuller.DatabaseContext";
    private final String directory;

    /**
     * Default constructor.
     *
     * @param context   The context (e.g activity or service) creating the database
     * @param directory The directory to store the database file
     */
    public DatabaseContext(Context context, String directory) {
        super(context);
        this.directory = directory;
    }

    /**
     * Returns a {@link java.io.File} containing the specified database
     *
     * @param name  The name of the database. The .db extension in the name is not mandatory
     *
     * @return  {@link java.io.File} corresponding to the specified database
     */
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

    /**
     * Opens or creates the specified database. This method is called internally by {@link android.database.sqlite.SQLiteOpenHelper}
     * objects and should not be called directly in your code.
     *
     * This version is called for android devices < api-11
     *
     * @param name      The name of the database. The .db extension in the name is not mandatory
     * @param mode      The mode to open the database in
     * @param factory   Used to allow returning sub-classes of {@link android.database.Cursor} when calling query.
     *
     * @return The opened {@link android.database.sqlite.SQLiteDatabase}
     */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        return result;
    }

    /**
     * Opens or creates the specified database. This method is called internally by {@link android.database.sqlite.SQLiteOpenHelper}
     * objects and should not be called directly in your code.
     *
     * This version is called for android devices < api-11
     *
     * @param name          The name of the database. The .db extension in the name is not mandatory
     * @param mode          The mode to open the database in
     * @param factory       Used to allow returning sub-classes of {@link android.database.Cursor} when calling query.
     * @param errorHandler  Interface to let you define the actions to take when the following errors are detected database corruption
     *
     */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name,mode, factory);
    }
}
