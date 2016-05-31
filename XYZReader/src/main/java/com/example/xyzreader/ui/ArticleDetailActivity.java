package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_POSITION = "position";
    public static final String STATE_CURRENT_PAGE = "state_current_page";

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;


    private boolean mReturning;
    private int mCurrentPosition;
    private int mStartingPosition;
    private ArticleDetailFragment mCurrentFragment;

    private SharedElementCallback callBack = new SharedElementCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mReturning) {
                Log.d(this.toString(), "Yo!");
                Log.d(this.toString(), "Current Item No: " + mPager.getCurrentItem());
                ImageView sharedElement = (mPagerAdapter.getRegisteredFragment(mPager.getCurrentItem())).getPhotoView();
                if (sharedElement == null) {
                    sharedElements.clear();
                    names.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    if (sharedElement.getTransitionName() == null) {
                        sharedElement.setTransitionName("image_" + mCurrentPosition);
                    }
                    names.clear();
                    sharedElements.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.isLollipopOrUp()) {
            Transition t = TransitionInflater.from(this).inflateTransition(R.transition.shared_detail_enter);
            getWindow().setSharedElementEnterTransition(t);
            Slide s = new Slide(Gravity.BOTTOM);
            s.excludeTarget(android.R.id.statusBarBackground, true);
            s.excludeTarget(android.R.id.navigationBarBackground, true);
            s.excludeTarget(R.id.photo_container, true);
            s.excludeTarget(R.id.appbar_detail, true);
            s.excludeTarget(R.id.toolbar, true);
            Fade s2 = new Fade();
            s2.addTarget(R.id.appbar_detail);
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.addTransition(s).addTransition(s2);
            getWindow().setEnterTransition(transitionSet);
        }
        ActivityCompat.postponeEnterTransition(this);
        setContentView(R.layout.activity_article_detail);
        getLoaderManager().initLoader(0, null, this);
        if (Utils.isLollipopOrUp()) {
            setEnterSharedElementCallback(callBack);
        }

        mStartingPosition = getIntent().getExtras().getInt(ARG_POSITION);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE);
        }
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                mCurrentPosition = position;
                Log.d(this.toString(), "Current Position = " + mCurrentPosition + " Starting pos = " + mStartingPosition);
                if (Utils.isLollipopOrUp()) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    int color = mPagerAdapter.getRegisteredFragment(position).getmColor();
                    if (color != 0) {
                        getWindow().setStatusBarColor(color);
                    } else {
                        getWindow().setStatusBarColor(ContextCompat.getColor(ArticleDetailActivity.this, R.color.theme_primary_dark));
                    }
                }
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mCurrentPosition);
    }

    @Override
    public void finishAfterTransition() {
        mReturning = true;
        Intent data = new Intent();
        data.putExtra(ArticleListActivity.EXTRA_OLD_ITEM_POSITION, mStartingPosition);
        data.putExtra(ArticleListActivity.EXTRA_NEW_ITEM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public long getSelectedItemId() {
        return mSelectedItemId;
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<WeakReference<Fragment>> registeredFragments = new SparseArray<>();
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        // http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            WeakReference<Fragment> f = new WeakReference<Fragment>((Fragment) super.instantiateItem(container, position));
            registeredFragments.put(position, f);
            return f.get();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public ArticleDetailFragment getRegisteredFragment(int position) {
            if (registeredFragments.get(position) != null) {
                return (ArticleDetailFragment) registeredFragments.get(position).get();
            }
            return null;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            mCurrentFragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            return mCurrentFragment;
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
