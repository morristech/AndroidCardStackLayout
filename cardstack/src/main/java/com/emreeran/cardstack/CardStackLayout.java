package com.emreeran.cardstack;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

/**
 * Created by Emre Eran on 21/05/2017.
 */

public class CardStackLayout extends FrameLayout {

    private static final int DURATION = 300;

    private int mLayoutWidth;
    private int mYMultiplier;

    private int mCurrentAdapterItem;
    private int mStackSize;
    private boolean mRepeat;

    private BaseAdapter mAdapter;
    private final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            refreshViewsFromAdapter();
        }

        @Override
        public void onInvalidated() {
            removeAllViews();
        }
    };

    private OnCardCountChangedListener mOnCardCountChangedListener;
    private OnCardMovedListener mOnCardMovedListener;
    private OnCardReleasedListener mOnCardReleasedListener;

    public CardStackLayout(Context context) {
        super(context);
        init(context, null);
    }

    public CardStackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CardStackLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public void setOnCardCountChangedListener(OnCardCountChangedListener onCardCountChangedListener) {
        mOnCardCountChangedListener = onCardCountChangedListener;
    }

    @SuppressWarnings("WeakerAccess unused") // Public API
    public void setOnCardMovedListener(OnCardMovedListener onCardMovedListener) {
        mOnCardMovedListener = onCardMovedListener;
    }

    @SuppressWarnings("WeakerAccess unused") // Public API
    public void setOnCardReleasedListener(OnCardReleasedListener onCardReleasedListener) {
        mOnCardReleasedListener = onCardReleasedListener;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (mOnCardCountChangedListener != null) {
            mOnCardCountChangedListener.onAdd(getChildCount());
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        if (mOnCardCountChangedListener != null) {
            mOnCardCountChangedListener.onRemove(getChildCount());
        }
    }

    void onCardMoved(View view, float posX) {
        int childCount = getChildCount();
        for (int i = childCount - 2; i >= 0; i--) {
            CardStackItemContainerLayout tinderCardView = (CardStackItemContainerLayout) getChildAt(i);

            if (tinderCardView != null) {
                if (Math.abs(posX) == (float) mLayoutWidth) {
                    float scaleValue = 1 - ((childCount - 2 - i) / 50.0f);

                    tinderCardView.animate()
                            .x(0)
                            .y((childCount - 2 - i) * mYMultiplier)
                            .scaleX(scaleValue)
                            .rotation(0)
                            .setInterpolator(new AnticipateOvershootInterpolator())
                            .setDuration(DURATION);
                }
            }
        }

        if (mOnCardMovedListener != null) {
            mOnCardMovedListener.onMove(view);
        }
    }

    void onCardReleased(View view) {
        if (mOnCardReleasedListener != null) {
            mOnCardReleasedListener.onRelease(view);
        }
    }

    private void init(Context context, AttributeSet attrs) {
        setClipChildren(false);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLayoutWidth = getWidth();
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CardStackLayout);
        mStackSize = typedArray.getInteger(
                R.styleable.CardStackLayout_stack_size,
                getResources().getInteger(R.integer.card_stack_layout_default_stack_size)
        );
        mRepeat = typedArray.getBoolean(R.styleable.CardStackLayout_stack_repeat, false);
        typedArray.recycle();

        mYMultiplier = getResources().getDimensionPixelSize(R.dimen.card_stack_layout_child_size_multiplier);
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public void addCard(View cardView) {
        CardStackItemContainerLayout cardStackItemContainerLayout = new CardStackItemContainerLayout(cardView.getContext());
        cardStackItemContainerLayout.addView(cardView);
        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        int childCount = getChildCount();
        addView(cardStackItemContainerLayout, 0, layoutParams);

        float scaleValue = 1 - (childCount / 50.0f);

        cardStackItemContainerLayout.animate()
                .x(0)
                .y(childCount * mYMultiplier)
                .scaleX(scaleValue)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setDuration(DURATION);
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public void setAdapter(BaseAdapter adapter) {
        // Unregister observer if there was a previous adapter
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        mAdapter = adapter;

        // Register to new adapter if one is set
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }

        initViewsFromAdapter();
    }

    private void initViewsFromAdapter() {
        removeAllViews();

        if (mAdapter != null) {
            for (mCurrentAdapterItem = 0;
                 mCurrentAdapterItem < mStackSize && mCurrentAdapterItem < mAdapter.getCount();
                 mCurrentAdapterItem++) {
                View view = mAdapter.getView(mCurrentAdapterItem, null, this);
                addCard(view);
            }

            setOnCardCountChangedListener(new OnCardCountChangedListener() {
                @Override
                public void onAdd(int cardCount) {

                }

                @Override
                public void onRemove(int cardCount) {
                    if (mCurrentAdapterItem < mAdapter.getCount()) {
                        View view = mAdapter.getView(mCurrentAdapterItem, null, CardStackLayout.this);
                        addCard(view);
                        mCurrentAdapterItem++;
                    } else if (mRepeat) {
                        mCurrentAdapterItem = 0;
                        View view = mAdapter.getView(mCurrentAdapterItem, null, CardStackLayout.this);
                        addCard(view);
                        mCurrentAdapterItem++;
                    }
                }
            });
        }
    }

    private void refreshViewsFromAdapter() {
        int childCount = getChildCount();
        int adapterSize = mAdapter.getCount();
        int reuseCount = Math.min(childCount, adapterSize);

        for (int i = 0; i < reuseCount; i++) {
            mAdapter.getView(i, getChildAt(i), this);
        }

        if (childCount < adapterSize) {
            for (int i = childCount; i < adapterSize; i++) {
                addView(mAdapter.getView(i, null, this), i);
            }
        } else if (childCount > adapterSize) {
            removeViews(adapterSize, childCount);
        }
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public interface OnCardCountChangedListener {
        void onAdd(int cardCount);

        void onRemove(int cardCount);
    }

    @SuppressWarnings("WeakerAccess unused") // Public API
    public interface OnCardMovedListener {
        void onMove(View view);
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public interface OnCardReleasedListener {
        void onRelease(View view);
    }
}
