package sk.sanctuary.android.ebookreader;

import android.provider.BaseColumns;

/**
 * Created by Ján on 15.5.2016.
 */
public interface EBooks {

    public interface EBook extends BaseColumns{

        public static final String TABLE_NAME = "ebooks";

        public static final String PATH = "path";

        public static final String TITLE = "title";

        public static final String AUTHOR = "author";

        public static final String CHAPTER = "chapter";

        public static final String POSITION = "position";
    }

    public interface Bookmark extends BaseColumns {

        public static final String TABLE_NAME = "bookmarks";

        public static final String EBOOK_ID = "ebook_id";

        public static final String CHAPTER = "chapter";

        public static final String POSITION = "position";
    }
}
