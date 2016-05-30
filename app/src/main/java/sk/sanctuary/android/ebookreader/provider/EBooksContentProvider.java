package sk.sanctuary.android.ebookreader.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import sk.sanctuary.android.ebookreader.EBooks;
import sk.sanctuary.android.ebookreader.EBooksDataBaseOpenHelper;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static sk.sanctuary.android.ebookreader.EBooks.EBook;
import static sk.sanctuary.android.ebookreader.EBooks.Bookmark;

public class EBooksContentProvider extends ContentProvider {

    private EBooksDataBaseOpenHelper helper;

    public static final String AUTHORITY = "sk.sanctuary.android.ebookreader.provider.EBooksContentProvider";

    public static final Uri CONTENT_URI_EBOOK = new Uri.Builder().scheme(SCHEME_CONTENT).authority(AUTHORITY).appendPath(EBook.TABLE_NAME).build();
    public static final Uri CONTENT_URI_BOOKMARK = new Uri.Builder().scheme(SCHEME_CONTENT).authority(AUTHORITY).appendPath(Bookmark.TABLE_NAME).build();

    private UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int URI_MATCH_EBOOKS = 0;
    private static final int URI_MATCH_EBOOK_BY_ID = 1;
    private static final int URI_MATCH_BOOKMARKS = 2;
    private static final int URI_MATCH_BOOKMARK_BY_ID = 3;

    public EBooksContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db;
        long id;
        switch (uriMatcher.match(uri)){
            case URI_MATCH_EBOOK_BY_ID:
                int affectedBooks = deleteEBook(uri);
                getContext().getContentResolver().notifyChange(CONTENT_URI_EBOOK, null);
                return affectedBooks;
            case URI_MATCH_BOOKMARK_BY_ID:
                int affectedBookmarks = deleteBookmark(uri);
                getContext().getContentResolver().notifyChange(CONTENT_URI_BOOKMARK, null);
                return affectedBookmarks;
            default:
                return 0;
        }
    }

    private int deleteBookmark(Uri uri) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = ContentUris.parseId(uri);
        String selection = Bookmark._ID + "=" + id;
        return db.delete(Bookmark.TABLE_NAME, selection, null);
    }

    private int deleteAllBookmarks(Uri uri) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = ContentUris.parseId(uri);
        String selection = Bookmark.EBOOK_ID + "=" + id;
        return db.delete(Bookmark.TABLE_NAME, selection, null);
    }

    private int deleteEBook(Uri uri) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = ContentUris.parseId(uri);
        String selection = EBook._ID + "=" + id;
        deleteAllBookmarks(uri);
        return db.delete(EBook.TABLE_NAME, selection, null);
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri newUri;
        switch (uriMatcher.match(uri)){
            case URI_MATCH_EBOOKS:
                newUri = saveEBook(values);
                getContext().getContentResolver().notifyChange(CONTENT_URI_EBOOK, null);
                return newUri;
            case URI_MATCH_BOOKMARKS:
                newUri = saveBookmark(values);
                getContext().getContentResolver().notifyChange(CONTENT_URI_BOOKMARK, null);
                return newUri;
            default:
                return null; //NO_URI
        }
    }

    private Uri saveEBook(ContentValues values) {
        ContentValues eBook = new ContentValues();
        eBook.put(EBook._ID, (byte[]) null); //AUTOGENERATED_ID
        eBook.put(EBook.PATH, values.getAsString(EBook.PATH));
        eBook.put(EBook.TITLE, values.getAsString(EBook.TITLE));
        eBook.put(EBook.AUTHOR, values.getAsString(EBook.AUTHOR));

        return getUri(eBook, EBook.TABLE_NAME, CONTENT_URI_EBOOK);
    }

    private int updateEBook(Uri uri, ContentValues values) {
        String selection;SQLiteDatabase db = helper.getWritableDatabase();
        long id = ContentUris.parseId(uri);
        selection = EBook._ID + "=" + id;

        ContentValues eBook = new ContentValues();
        if (values.containsKey(EBook.CHAPTER)){
            eBook.put(EBook.CHAPTER, values.getAsInteger(EBook.CHAPTER));
        }

        return db.update(EBook.TABLE_NAME, eBook, selection, null);
    }

    private Uri saveBookmark(ContentValues values) {
        ContentValues bookmark = new ContentValues();
        bookmark.put(Bookmark._ID, (byte[]) null);
        bookmark.put(Bookmark.EBOOK_ID, values.getAsLong(EBook._ID));
        bookmark.put(Bookmark.CHAPTER, values.getAsInteger(Bookmark.CHAPTER));
        bookmark.put(Bookmark.POSITION, values.getAsInteger(Bookmark.POSITION));

        return getUri(bookmark, Bookmark.TABLE_NAME, CONTENT_URI_BOOKMARK);
    }

    private Uri getUri(ContentValues values, String tableName, Uri contentUri) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long newID = db.insert(tableName, null, values);
        return ContentUris.withAppendedId(contentUri, newID);
    }


    @Override
    public boolean onCreate() {
        helper = new EBooksDataBaseOpenHelper(getContext());

        uriMatcher.addURI(AUTHORITY, EBook.TABLE_NAME, URI_MATCH_EBOOKS);
        uriMatcher.addURI(AUTHORITY, EBook.TABLE_NAME + "/#", URI_MATCH_EBOOK_BY_ID);
        uriMatcher.addURI(AUTHORITY, Bookmark.TABLE_NAME, URI_MATCH_BOOKMARKS);
        uriMatcher.addURI(AUTHORITY, Bookmark.TABLE_NAME + "/#", URI_MATCH_BOOKMARK_BY_ID);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        switch (uriMatcher.match(uri)){
            case URI_MATCH_EBOOKS:
                cursor = listItems(EBook.TABLE_NAME, CONTENT_URI_EBOOK, null, null);
                return cursor;
            case URI_MATCH_EBOOK_BY_ID:
                long idBook = ContentUris.parseId(uri);
                cursor = findEBookById(idBook);
                return cursor;
            case URI_MATCH_BOOKMARKS:
                cursor = listItems(Bookmark.TABLE_NAME, CONTENT_URI_BOOKMARK, projection, selection);
                return cursor;
            case URI_MATCH_BOOKMARK_BY_ID:
                long idBookmark = ContentUris.parseId(uri);
                cursor = findBookmarkById(idBookmark);
                return cursor;
            default:
                return null;
        }
    }

    private Cursor findBookmarkById(long idBookmark) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String selection = Bookmark._ID + "=" + idBookmark;
        return db.query(Bookmark.TABLE_NAME, null, selection, null, null, null, null);
    }

    private Cursor findEBookById(long idBook) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String selection = EBook._ID + "=" + idBook;
        return db.query(EBook.TABLE_NAME, null, selection, null, null, null, null);
    }

    private Cursor listItems(String tableName, Uri contentUri, String[] projection, String selection) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(tableName, projection, selection, null, null, null, null);
        cursor.setNotificationUri(getContext().getContentResolver(), contentUri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        switch (uriMatcher.match(uri)){
            case URI_MATCH_EBOOK_BY_ID:
                int affectedRows = updateEBook(uri, values);
                getContext().getContentResolver().notifyChange(CONTENT_URI_EBOOK, null);
                return affectedRows;
            default:
                return 0;
        }
    }
}
