package com.nkanaev.comics.fragment;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.*;
import android.widget.*;
import android.support.v7.widget.SearchView;

import com.nkanaev.comics.R;
import com.nkanaev.comics.activity.MainActivity;
import com.nkanaev.comics.activity.ReaderActivity;
import com.nkanaev.comics.managers.LocalCoverHandler;
import com.nkanaev.comics.managers.Utils;
import com.nkanaev.comics.model.Comic;
import com.nkanaev.comics.model.Storage;
import com.nkanaev.comics.view.CoverImageView;
import com.squareup.picasso.Picasso;

public class LibraryBrowserFragment extends Fragment
        implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener {
    public static final String PARAM_PATH = "browserCurrentPath";

    private GridView mGridView;
    private ArrayList<Comic> mComics;
    private ArrayList<Integer> mDisplayedIndexes;
    private Picasso mPicasso;
    private int mCurrentComicIndex = -1;
    private String mFilterSearch = "";
    private int mFilterRead = R.id.menu_browser_filter_all;


    public static LibraryBrowserFragment create(String path) {
        LibraryBrowserFragment fragment = new LibraryBrowserFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_PATH, path);
        fragment.setArguments(args);
        return fragment;
    }

    public LibraryBrowserFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String path = getArguments().getString(PARAM_PATH);

        mComics = Storage.getStorage(getActivity()).listComics(path);
        filterContent();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_librarybrowser, container, false);
        final BrowserAdapter adapter = new BrowserAdapter();

        mPicasso = ((MainActivity) getActivity()).getPicasso();

        int deviceWidth = Utils.getDeviceWidth(getActivity());
        int columnWidth = getActivity().getResources().getInteger(R.integer.grid_comic_column_width);
        int numColumns = Math.round((float) deviceWidth / columnWidth);

        mGridView = (GridView)view.findViewById(R.id.gridView);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setNumColumns(numColumns);

        getActivity().setTitle(new File(getArguments().getString(PARAM_PATH)).getName());

        return view;
    }

    @Override
    public void onResume() {
        if (mCurrentComicIndex != -1) {
            Comic currentComic = mComics.get(mCurrentComicIndex);
            Comic updatedComic = Storage.getStorage(getActivity()).getComic(currentComic.getId());
            mComics.set(mCurrentComicIndex, updatedComic);
            mCurrentComicIndex = -1;
            mGridView.invalidateViews();
        }
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.browser, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_browser_filter_all:
            case R.id.menu_browser_filter_read:
            case R.id.menu_browser_filter_unread:
            case R.id.menu_browser_filter_reading:
                item.setChecked(true);
                mFilterRead = item.getItemId();
                filterContent();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mFilterSearch = s;
        filterContent();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return true;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Comic comic = mComics.get(mDisplayedIndexes.get(position));
        mCurrentComicIndex = mDisplayedIndexes.get(position);

        Intent intent = new Intent(getActivity(), ReaderActivity.class);
        intent.putExtra(ReaderFragment.PARAM_HANDLER, comic.getId());
        intent.putExtra(ReaderFragment.PARAM_MODE, ReaderFragment.Mode.MODE_LIBRARY);
        startActivity(intent);
    }

    private void filterContent() {
        mDisplayedIndexes = new ArrayList<>();
        for (int i = 0; i < mComics.size(); i++) {
            Comic c = mComics.get(i);
            if (mFilterSearch.length() > 0 && !c.getFile().getName().contains(mFilterSearch))
                continue;
            if (mFilterRead != R.id.menu_browser_filter_all) {
                if (mFilterRead == R.id.menu_browser_filter_read && c.getCurrentPage() != c.getTotalPages())
                    continue;
                if (mFilterRead == R.id.menu_browser_filter_unread && c.getCurrentPage() != 0)
                    continue;
                if (mFilterRead == R.id.menu_browser_filter_reading &&
                        (c.getCurrentPage() == 0 || c.getCurrentPage() == c.getTotalPages()))
                    continue;
            }
            mDisplayedIndexes.add(i);
        }

        if (mGridView != null) mGridView.invalidateViews();
    }

    private final class BrowserAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mDisplayedIndexes.size();
        }

        @Override
        public Object getItem(int position) {
            return mDisplayedIndexes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup comicView = (ViewGroup) convertView;
            if (comicView == null) {
                comicView = (ViewGroup)getActivity()
                        .getLayoutInflater()
                        .inflate(R.layout.card_comic, parent, false);
            }

            Comic comic = mComics.get(mDisplayedIndexes.get(position));

            CoverImageView coverImageView = (CoverImageView)comicView.findViewById(R.id.comicImageView);
            TextView titleTextView = (TextView)comicView.findViewById(R.id.comicTitleTextView);
            TextView pagesTextView = (TextView)comicView.findViewById(R.id.comicPagerTextView);

            titleTextView.setText(comic.getFile().getName());
            pagesTextView.setText(Integer.toString(comic.getCurrentPage()) + '/' + Integer.toString(comic.getTotalPages()));

            mPicasso.load(LocalCoverHandler.getComicCoverUri(comic))
                    .into(coverImageView);

            return comicView;
        }
    }
}
