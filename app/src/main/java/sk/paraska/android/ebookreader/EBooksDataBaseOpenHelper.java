package sk.paraska.android.ebookreader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Ján on 15.5.2016.
 */
public class EBooksDataBaseOpenHelper extends SQLiteOpenHelper {
    public EBooksDataBaseOpenHelper(Context context) {
        super(context, "ebooks", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql_books = "CREATE TABLE %s (" +
                "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "%s TEXT NOT NULL, " +
                "%s TEXT NOT NULL, " +
                "%s TEXT NOT NULL, " +
                "%s INTEGER DEFAULT 0" +
                ")";

        String sql_bookmarks = "CREATE TABLE %s (" +
                "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "%s INTEGER NOT NULL, " +
                "%s INTEGER NOT NULL, " +
                "%s INTEGER NOT NULL" +
                ")";

        db.execSQL(String.format(sql_books,
                EBooks.EBook.TABLE_NAME,
                EBooks.EBook._ID,
                EBooks.EBook.PATH,
                EBooks.EBook.TITLE,
                EBooks.EBook.AUTHOR,
                EBooks.EBook.CHAPTER));

        db.execSQL(String.format(sql_bookmarks,
                EBooks.Bookmark.TABLE_NAME,
                EBooks.Bookmark._ID,
                EBooks.Bookmark.EBOOK_ID,
                EBooks.Bookmark.CHAPTER,
                EBooks.Bookmark.POSITION));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
