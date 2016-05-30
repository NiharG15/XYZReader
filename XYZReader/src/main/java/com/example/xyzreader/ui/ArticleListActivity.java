package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utils.PaletteTransformation;
import com.example.xyzreader.utils.Utils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_OLD_ITEM_POSITION = "old_item";
    public static final String EXTRA_NEW_ITEM_POSITION = "new_item";

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private boolean mIsRefreshing = false;
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };


    private AppBarLayout mAppBarLayout;

    //Code based on https://github.com/alexjlockwood/activity-transitions
    private int mOriginalPosition;
    private int mNewPosition;
    private boolean mReentering;
    private SharedElementCallback callback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mReentering) {
                if (mOriginalPosition != mNewPosition) {
                    ViewHolder vh = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(mNewPosition);
                    names.clear();
                    sharedElements.clear();
                    names.add("image_" + mNewPosition);
                    sharedElements.put("image_" + mNewPosition, vh.thumbnailView);
                    names.add("toolbar");
                    sharedElements.put("toolbar", mToolbar);
                }
                mReentering = false;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.isLollipopOrUp()) {
            setExitSharedElementCallback(callback);
        }
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.toolbar_container);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        mReentering = true;
        if (data != null && data.getExtras() != null) {
            mOriginalPosition = data.getExtras().getInt(EXTRA_OLD_ITEM_POSITION);
            mNewPosition = data.getExtras().getInt(EXTRA_NEW_ITEM_POSITION);
            Log.d(this.toString(), "OGPosition: " + mOriginalPosition);
            Log.d(this.toString(), "NewPosition: " + mNewPosition);
            if (mOriginalPosition != mNewPosition) {
                mRecyclerView.scrollToPosition(mNewPosition);
            }

            ActivityCompat.postponeEnterTransition(this);
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mRecyclerView.requestLayout();
                    ActivityCompat.startPostponedEnterTransition(ArticleListActivity.this);
                    return true;
                }
            });
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public LinearLayout container;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            container = (LinearLayout) view.findViewById(R.id.listItemContainer);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Pair p1 = Pair.create(mToolbar, "toolbar");
                    Pair p2 = Pair.create(vh.thumbnailView, "image_" + vh.getAdapterPosition());
                    ActivityOptionsCompat aco = ActivityOptionsCompat.makeSceneTransitionAnimation(ArticleListActivity.this,
                            p1, p2);
                    Intent i = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                    i.putExtra(ArticleDetailActivity.ARG_POSITION, vh.getAdapterPosition());
                    ActivityCompat.startActivity(ArticleListActivity.this, i, aco.toBundle());
//                    startActivity(new Intent(Intent.ACTION_VIEW,
//                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            Picasso.with(ArticleListActivity.this).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL)).transform(PaletteTransformation.instance()).into(holder.thumbnailView,
                    new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((BitmapDrawable) holder.thumbnailView.getDrawable()).getBitmap();
                            Palette palette = PaletteTransformation.getPalette(bitmap);
                            holder.container.setBackgroundColor(palette.getLightMutedColor(0xFFFFFF));
                        }

                        @Override
                        public void onError() {

                        }
                    });
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            if (Utils.isLollipopOrUp()) {
                holder.thumbnailView.setTransitionName("image_" + position);
            }
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }
}
