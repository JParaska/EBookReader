package sk.sanctuary.android.ebookreader;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import sk.sanctuary.android.ebookreader.provider.EBooksContentProvider;

public class EBookDetailActivity extends AppCompatActivity {

    private String path;
    private long id;
    private Book book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ebook_detail);

        path = getIntent().getStringExtra("path");
        id = getIntent().getLongExtra("id", 0);

        if (id == 0){
            Toast.makeText(this, "Book not found :(", Toast.LENGTH_LONG).show();
            finish();
        } else {
            try {
                loadInfo();
            } catch (IOException e) {
                Toast.makeText(this, "Book not found :(", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void loadInfo() throws IOException {
        book = (new EpubReader()).readEpub(new FileInputStream(new File(path)));
        Metadata metadata = book.getMetadata();

        //cover image as background
        ImageView coverDetailImageView = (ImageView) findViewById(R.id.coverDetailImageView);
        coverDetailImageView.setImageBitmap(BitmapFactory.decodeStream(book.getCoverImage().getInputStream()));

        //main title and subtitle
        List<String> titles = metadata.getTitles(); //0: first title
        ((TextView) findViewById(R.id.mainTitleDetailTextView)).setText(titles.get(0));
        TextView titelsTextView = (TextView) findViewById(R.id.subtitelDetailTextView);
        if (titles.size() > 1) {
            makeString("", titles, titelsTextView);
        } else {
            titelsTextView.setVisibility(View.GONE);
        }

        //authors
        List<Author> authors = metadata.getAuthors();
        ((TextView) findViewById(R.id.authorDetailTextView)).setText(authors.get(0).getFirstname() + " " + authors.get(0).getLastname());
        TextView authorsTextView = (TextView) findViewById(R.id.otherAuthorsDetailTextView);
        makeAuthorsDesc(authors, authorsTextView);

        //subjects
        List<String> subjects = metadata.getSubjects();
        TextView subjectsTextView = (TextView) findViewById(R.id.subjectsDetailTextView);
        makeString("Subjects: ", subjects, subjectsTextView);

        //publishers
        List<String> publishers = metadata.getPublishers();
        TextView publishersTextView = (TextView) findViewById(R.id.publishersDetailTetView);
        makeString("Publishers: ", publishers, publishersTextView);
    }

    private void makeString(String prefix, List<String> items, TextView textView) {
        StringBuilder sbPublishers = new StringBuilder();
        sbPublishers.append(prefix);
        if (items.size()>0){
            for (int i = 1; i < items.size()-1; i++){
                sbPublishers.append(items.get(i)).append(", ");
            }
            sbPublishers.append(items.get(items.size() - 1));
        } else {
            sbPublishers.append("N/A");
        }
        textView.setText(sbPublishers.toString());
    }

    private void makeAuthorsDesc(List<Author> authors, TextView authorsTextView) {
        StringBuilder sbAuthors = new StringBuilder();
        if (authors.size()>1){
            sbAuthors.append("and ");
            for (int i = 1; i < authors.size()-1; i++){
                sbAuthors.append(authors.get(i).getFirstname()).append(" ").append(authors.get(i).getLastname()).append(", ");
            }
            sbAuthors.append(authors.get(authors.size()-1));
            authorsTextView.setText(sbAuthors.toString());
        } else {
            authorsTextView.setVisibility(View.GONE);
        }
    }

    public void deleteBook(View view) {
        final AsyncQueryHandler handler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                finish();
            }
        };

        DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.startDelete(0, null, ContentUris.withAppendedId(EBooksContentProvider.CONTENT_URI_EBOOK, id), null, null);
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("Remove book")
                .setMessage("This WILL NOT remove book from device")
                .setPositiveButton("Delete", deleteListener)
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }
}
