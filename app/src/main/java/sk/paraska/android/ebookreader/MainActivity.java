package sk.paraska.android.ebookreader;

import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import sk.paraska.android.ebookreader.FileChooser.FileChooser;
import sk.paraska.android.ebookreader.provider.EBooksContentProvider;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;

    private final String[] extensions = {".epub"};

    private final int FIRST_CHAPTER = 0;

    private ListView eBookListView;

    public static final boolean DO_NOT_READ_AGAIN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.settings, DO_NOT_READ_AGAIN);

        setTitle("EBookReader");

        eBookListView = (ListView) findViewById(R.id.eBookListView);

        eBookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
                    @Override
                    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                        if (cursor != null) {
                            cursor.moveToFirst();
                            String path = cursor.getString(cursor.getColumnIndex(EBooks.EBook.PATH));
                            int chapter = cursor.getInt(cursor.getColumnIndex(EBooks.EBook.CHAPTER));

                            showEBook(path, chapter, cursor.getLong(cursor.getColumnIndex(EBooks.EBook._ID)));
                        } else {
                            Toast.makeText(MainActivity.this, "EBook not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                handler.startQuery(0, null, ContentUris.withAppendedId(EBooksContentProvider.CONTENT_URI_EBOOK, id), null, null, null, null);
            }
        });

        eBookListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
                    @Override
                    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                        if (cursor != null) {
                            cursor.moveToFirst();
                            String path = cursor.getString(cursor.getColumnIndex(EBooks.EBook.PATH));
                            long id = cursor.getLong(cursor.getColumnIndex(EBooks.EBook._ID));

                            showEBookDetails(path, id);
                        } else {
                            Toast.makeText(MainActivity.this, "EBook not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                handler.startQuery(0, null, ContentUris.withAppendedId(EBooksContentProvider.CONTENT_URI_EBOOK, id), null, null, null, null);
                return true;
            }
        });

        String[] from = {EBooks.EBook.TITLE, EBooks.EBook.AUTHOR, EBooks.EBook.CHAPTER};
        int[] to = {R.id.eBookTitleTextView, R.id.eBookAuthorTextView, R.id.eBookChapterTextView};

        adapter = new SimpleCursorAdapter(this, R.layout.ebook, null, from, to, 0);
        eBookListView.setAdapter(adapter);
        getLoaderManager().initLoader(0, Bundle.EMPTY, this);
        eBookListView.setEmptyView(findViewById(R.id.eBookImageView));
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * pick a book from file system and save info into database
     * @param item file from filesystem
     */
    public void addNewEBook(MenuItem item) {
        FileChooser fc = new FileChooser(MainActivity.this).setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                getMetadataAndSave(file);
            }
        });

        fc.setExtensions(extensions);
        fc.showDialog();
    }

    /**
     * gets basic metadata from book and save them into database
     * @param file file from filesystem
     */
    private void getMetadataAndSave(File file) {
        String path = file.getAbsolutePath();
        String title;
        String author = "";
        try {
            Book book = (new EpubReader()).readEpub(new FileInputStream(file));
            title = book.getTitle();

            List<Author> authors = book.getMetadata().getAuthors();
            if (authors.size() > 0) {
                author = authors.get(0).getFirstname() + " " + authors.get(0).getLastname();
            }
        } catch (IOException e) {
            title = path.substring(path.lastIndexOf('/') + 1, path.indexOf('.'));
            author = "N/A";
        }

        saveEBook(path, title, author);
    }

    /**
     * opens new activity with book to read
     *
     * @param path    of the book
     * @param chapter actually read by user
     */
    private void showEBook(String path, int chapter, long id) {
        Intent intent = new Intent(MainActivity.this, PagerEBookActivity.class);
        intent.putExtra("path", path);
        intent.putExtra("chapter", chapter);
        intent.putExtra("id", id);

        startActivity(intent);
    }

    /**
     * opens new activity with book details
     *
     * @param path of the book
     * @param id   of the book
     */
    private void showEBookDetails(String path, long id) {
        Intent intent = new Intent(MainActivity.this, EBookDetailActivity.class);
        intent.putExtra("path", path);
        intent.putExtra("id", id);

        startActivity(intent);
    }

    private void saveEBook(final String path, String title, String author) {
        ContentValues values = new ContentValues();
        values.put(EBooks.EBook.PATH, path);
        values.put(EBooks.EBook.TITLE, title);
        values.put(EBooks.EBook.AUTHOR, author);

        AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onInsertComplete(int token, Object cookie, Uri uri) {
                Toast.makeText(MainActivity.this, "New EBook saved", Toast.LENGTH_SHORT).show();
                showEBook(path, FIRST_CHAPTER, ContentUris.parseId(uri));
            }
        };

        handler.startInsert(0, null, EBooksContentProvider.CONTENT_URI_EBOOK, values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ebooks_list, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(this);
        loader.setUri(EBooksContentProvider.CONTENT_URI_EBOOK);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        this.adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        this.adapter.swapCursor(null);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (eBookListView != null) {
            if (eBookListView.getAdapter().isEmpty()) {
                eBookListView.setVisibility(View.GONE);
            } else {
                eBookListView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void settings(MenuItem item) {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}