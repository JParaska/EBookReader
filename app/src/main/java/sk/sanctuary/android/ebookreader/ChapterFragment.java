package sk.sanctuary.android.ebookreader;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;

public class ChapterFragment extends Fragment {

    public static final String TYPE = "type";
    public static final int CHAPTER = 1;
    public static final int COVER = 0;
    public static final String CHAPTER_CONTENT = "chapterContent";
    private static final String CURRENT_WEBVIEW_POSITION_Y = "currentWebViewPositionY";
    private WebView webView;
    private ImageView coverImageView;

    public ChapterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            double Y = 100.0 * webView.getScrollY() / webView.getContentHeight();
            outState.putDouble(CURRENT_WEBVIEW_POSITION_Y, Y);
        }

        super.onSaveInstanceState(outState);
    }

    public static ChapterFragment newInstance(String text) {
        Bundle args = new Bundle();
        args.putInt(TYPE, CHAPTER);
        args.putString(CHAPTER_CONTENT, text);

        ChapterFragment fragment = new ChapterFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ChapterFragment newInstance(byte[] coverByteArray) {
        Bundle args = new Bundle();
        args.putInt(TYPE, COVER);
        args.putByteArray(CHAPTER_CONTENT, coverByteArray);

        ChapterFragment fragment = new ChapterFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chapter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        webView = (WebView) view.findViewById(R.id.textWebViewInFragment);
        coverImageView = (ImageView) view.findViewById(R.id.coverImageViewInFragment);

        if (args != null && args.containsKey(TYPE)) {
            showChapter(args);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_WEBVIEW_POSITION_Y)) {
            scrollWebView(savedInstanceState.getDouble(CURRENT_WEBVIEW_POSITION_Y));
        }
    }

    private void showChapter(Bundle args) {
        switch (args.getInt(TYPE)){
            case COVER:
                webView.setVisibility(View.GONE);
                coverImageView.setVisibility(View.VISIBLE);

                byte[] cover = args.getByteArray(CHAPTER_CONTENT);
                coverImageView.setImageBitmap(BitmapFactory.decodeByteArray(cover, 0, cover.length));
                break;
            case CHAPTER:
                coverImageView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);

                String text = args.getString(CHAPTER_CONTENT);
                if (text.length() > 0) {
                    webView.scrollTo(0, 0);
                    webView.clearCache(true);
                    webView.loadData(text, "text/html; charset=UTF-8", null);
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    webView.reload();
                } else {
                    webView.loadData("No data", null, null);
                }
                break;
        }
    }

    public int getWebViewContentHeight(){
        if (webView != null){
            return (int) Math.floor(100.0 * webView.getScrollY() / webView.getContentHeight());
        } else {
            return 0;
        }
    }

    public void scrollWebView(final double position){
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView.getContentHeight() > 0){
                    int Y = (int) Math.floor(position*webView.getContentHeight()/100);
                    webView.scrollTo(0, Y);
                    webView.removeCallbacks(this);
                } else {
                    webView.postDelayed(this, 10);
                }
            }
        }, 10);
    }
}