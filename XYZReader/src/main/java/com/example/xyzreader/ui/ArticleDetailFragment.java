package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.utils.PaletteTransformation;
import com.example.xyzreader.utils.Utils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mColor = 0;

    private ImageView mPhotoView;

    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private boolean collapsed = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail_new, container, false);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        bindViews();

        mToolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mAppBarLayout = (AppBarLayout) mRootView.findViewById(R.id.appbar_detail);
            getActivityCast().setSupportActionBar(mToolbar);
            getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getActivityCast().getSupportActionBar().setHomeButtonEnabled(true);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivityCast().onBackPressed();
                }
            });
            mToolbar.setTitle("");

            if (Utils.isLollipopOrUp())
                if (getActivityCast().getSelectedItemId() == mItemId) {
                    Intent i = getActivityCast().getIntent();
                    String transitionName = "image_" + i.getIntExtra(ArticleDetailActivity.ARG_POSITION, 0);
                    mPhotoView.setTransitionName(transitionName);
                    mToolbar.setTransitionName("toolbar");
                }

            final int offset = getResources().getDimensionPixelSize(R.dimen.collapsed_offset);
            mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    collapsed = verticalOffset <= -offset;
                }
            });
        }
        return mRootView;
    }

    public ImageView getPhotoView() {
        return collapsed ? null : mPhotoView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        //bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setVisibility(View.VISIBLE);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)).toString())
                            .getIntent(), getString(R.string.action_share)));
                }
            });
            Picasso.with(getActivity()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL)).transform(PaletteTransformation.instance()).into(mPhotoView, new Callback() {
                @Override
                public void onSuccess() {
                    // http://jakewharton.com/coercing-picasso-to-play-with-palette/
                    Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                    Palette palette = PaletteTransformation.getPalette(bitmap);
                    if (palette != null) {
                        applyColor(palette.getMutedColor(0xFF333333));
                        mColor = palette.getMutedColor(0);
                    }
                }

                @Override
                public void onError() {

                }
            });
            startPostponedTransition();
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    private void applyColor(@ColorInt final int color) {
        final FrameLayout colorBar = (FrameLayout) mRootView.findViewById(R.id.color_bar);
        if (Utils.isLollipopOrUp()) {
            //If there is shared element transition, delay the color reveal by 500 ms
            final boolean delayed = getActivity() instanceof ArticleDetailActivity;
            //To make sure that view is laid out before we request width and height
            colorBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onGlobalLayout() {
                    colorBar.setBackgroundColor(color);
                    float halfWidth = colorBar.getMeasuredWidth() / 2;
                    float height = colorBar.getMeasuredHeight();
                    float endRadius = (float) Math.sqrt(halfWidth * halfWidth + height * height);
                    Log.d(TAG, String.valueOf(endRadius));
                    if (endRadius > 0) {
                        colorBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        Animator reveal = ViewAnimationUtils.createCircularReveal(colorBar, colorBar.getRight() / 2, colorBar.getTop(), 0, endRadius);
                        reveal.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                colorBar.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {

                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });
                        if (delayed) {
                            reveal.setStartDelay(500);
                        }
                        reveal.start();
                    }
                }
            });
        } else {
            //Fall back to fade animation on 19 and below
            colorBar.setBackgroundColor(0xFF333333);
            ObjectAnimator animator = ObjectAnimator.ofObject(colorBar, "backgroundColor", new ArgbEvaluator(), 0xFF333333, color);
            animator.setDuration(500);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    colorBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animator) {

                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            animator.start();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private void startPostponedTransition() {
        mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                ActivityCompat.startPostponedEnterTransition(getActivity());
                return true;
            }
        });
    }

    public int getmColor() {
        return mColor;
    }
}
