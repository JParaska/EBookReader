package sk.sanctuary.android.ebookreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.URIResolver;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import sk.sanctuary.android.ebookreader.provider.EBooksContentProvider;

public class PagerEBookActivity extends AppCompatActivity {

    private final String SAVED_CHAPTER = "savedChapter";

    String path;
    private ViewPager pager;

    private Book book;
    private List<Resource> contents;

    int chapter = -1;
    long id;

    List<int[]> bookmarks = new ArrayList<>();
    private final int BOOKMARK_CHAPTER = 0;
    private final int BOOKMARK_POSITION = 1;
    private final int BOOKMARK_ID = 2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebook_pager);

        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        if (savedInstanceState != null) {
            chapter = savedInstanceState.getInt(SAVED_CHAPTER);
        } else {
            chapter = intent.getIntExtra("chapter", 0);
        }
        id = intent.getLongExtra("id", 0);

        try {
            File bookTMP = new File(path);

            InputStream eBookIS = new FileInputStream(bookTMP);
            book = (new EpubReader()).readEpub(eBookIS);
            contents = book.getContents();

            setTitle(book.getTitle());

            initializePager();
            loadBookmarks();
        } catch (Exception e) {
            Toast.makeText(this, "Book not found", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadBookmarks() {
        bookmarks = new ArrayList<>();
        AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()){
                    do {
                        int[] bookmark = new int[3];
                        bookmark[BOOKMARK_CHAPTER] = cursor.getInt(cursor.getColumnIndex(EBooks.Bookmark.CHAPTER));
                        bookmark[BOOKMARK_POSITION] = cursor.getInt(cursor.getColumnIndex(EBooks.Bookmark.POSITION));
                        bookmark[BOOKMARK_ID] = (int)cursor.getLong(cursor.getColumnIndex(EBooks.Bookmark._ID));
                        bookmarks.add(bookmark);
                    } while (cursor.moveToNext());
                }
            }
        };

        String selection = EBooks.Bookmark.EBOOK_ID + "=" + id;
        handler.startQuery(0, null, EBooksContentProvider.CONTENT_URI_BOOKMARK, null, selection, null, null);
    }

    private void initializePager() {
        pager = (ViewPager) findViewById(R.id.eBookViewPager);

        pager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                try {
                    if (position == 0) {
                        return ChapterFragment.newInstance(book.getCoverImage().getData());
                    } else {
                        return ChapterFragment.newInstance(new String(contents.get(position).getData()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public int getCount() {
                return contents.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return "Cover";
                } else {
                    return position + ". part";
                }
            }
        });

        pager.setCurrentItem(chapter);

        //https://guides.codepath.com/android/ViewPager-with-FragmentPagerAdapter
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                chapter = position;
                upDateBookChapter();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * update actual chapter
     */
    private void upDateBookChapter() {
        ContentValues values = new ContentValues();
        values.put(EBooks.EBook.CHAPTER, chapter);

        AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onUpdateComplete(int token, Object cookie, int result) {
                //nothing
            }
        };

        handler.startUpdate(0, null, ContentUris.withAppendedId(EBooksContentProvider.CONTENT_URI_EBOOK, id), values, null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ebook, menu);
        return true;
    }

    public void goTo(MenuItem item) throws IOException, InterruptedException {
        getNumberFromDialog();
    }

    /**
     * opens new activiry with book details
     */
    public void showEBookDetails(MenuItem menu) {
        Intent intent = new Intent(this, EBookDetailActivity.class);
        intent.putExtra("path", path);
        intent.putExtra("id", id);

        startActivity(intent);
        finish();
    }

    /**
     * http://stackoverflow.com/questions/17805040/how-to-create-a-number-picker-dialog
     */
    public void getNumberFromDialog() {
        final Dialog d = new Dialog(this);
        d.setTitle("Set chapter/part/page");
        d.setContentView(R.layout.number_picker_dialog);

        final NumberPicker picker = (NumberPicker) d.findViewById(R.id.numberPicker);
        picker.setMinValue(0);
        picker.setMaxValue(contents.size() - 1);
        picker.setValue(chapter);

        Button okButton = (Button) d.findViewById(R.id.okButton);
        Button cancelButton = (Button) d.findViewById(R.id.cancelButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chapter = picker.getValue();
                pager.setCurrentItem(chapter);
                d.dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.show();
    }

    /**
     * adds new bookmark on current chapter and position
     * @param item in the menu
     */
    public void addBookmark(MenuItem item) {
        AsyncQueryHandler checkHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                cursor.moveToFirst();
                int count = cursor.getInt(0);

                if (count < 10){
                    ContentValues values = new ContentValues();
                    values.put(EBooks.EBook._ID, id);
                    values.put(EBooks.Bookmark.CHAPTER, chapter);

                    //http://stackoverflow.com/questions/18609261/getting-the-current-fragment-instance-in-the-viewpager
                    ChapterFragment fragment = (ChapterFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.eBookViewPager + ":" + pager.getCurrentItem());
                    values.put(EBooks.Bookmark.POSITION, fragment.getWebViewContentHeight());

                    AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
                        @Override
                        protected void onInsertComplete(int token, Object cookie, Uri uri) {
                            Toast.makeText(PagerEBookActivity.this, "Bookmark added!", Toast.LENGTH_SHORT).show();
                            loadBookmarks();
                        }
                    };
                    handler.startInsert(0, null, EBooksContentProvider.CONTENT_URI_BOOKMARK, values);
                } else {
                    Toast.makeText(PagerEBookActivity.this, "Max 5 bookmarks per book!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        String[] projection = {"COUNT(*)"};
        String selection = EBooks.Bookmark.EBOOK_ID + "=" + id;
        checkHandler.startQuery(0, null, EBooksContentProvider.CONTENT_URI_BOOKMARK, projection, selection, null, null);
    }

    /**
     * shows list of bookmarks in the book
     * @param item in the menu
     */
    public void showBookmarks(MenuItem item) {
        if (bookmarks.size() == 0){
            Toast.makeText(this, "There are no bookmarks in this book!", Toast.LENGTH_SHORT).show();
        } else {
            String[] bookmarkArray = new String[bookmarks.size()];
            for (int i = 0; i < bookmarks.size(); i++){
                bookmarkArray[i] = bookmarks.get(i)[BOOKMARK_CHAPTER] + ". part, " + bookmarks.get(i)[BOOKMARK_POSITION] + "%";
            }
            new AlertDialog.Builder(this)
                    .setTitle("Pick a bookmark")
                    .setIcon(android.R.drawable.star_big_on)
                    .setItems(bookmarkArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pager.setCurrentItem(bookmarks.get(which)[BOOKMARK_CHAPTER]);
                            ChapterFragment fragment = (ChapterFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.eBookViewPager + ":" + pager.getCurrentItem());
                            fragment.scrollWebView(bookmarks.get(which)[BOOKMARK_POSITION]);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }


    public void deleteBookmarks(MenuItem item) {
        if (bookmarks.size() == 0){
            Toast.makeText(this, "There are no bookmarks in this book!", Toast.LENGTH_SHORT).show();
        } else {
            String[] bookmarkArray = new String[bookmarks.size()];
            for (int i = 0; i < bookmarks.size(); i++){
                bookmarkArray[i] = bookmarks.get(i)[BOOKMARK_CHAPTER] + ". part, " + bookmarks.get(i)[BOOKMARK_POSITION] + "%";
            }
            new AlertDialog.Builder(this)
                    .setTitle("Pick a bookmark to remove")
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .setItems(bookmarkArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteBookmark(bookmarks.get(which));
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void deleteBookmark(int[] bookmark) {
        AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                loadBookmarks();
            }
        };
        String selection = EBooks.Bookmark._ID + "=" + bookmark[BOOKMARK_ID];
        handler.startDelete(0, null, ContentUris.withAppendedId(EBooksContentProvider.CONTENT_URI_BOOKMARK, (long)bookmark[BOOKMARK_ID]), selection, null);
    }
}
