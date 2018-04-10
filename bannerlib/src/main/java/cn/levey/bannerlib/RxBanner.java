package cn.levey.bannerlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import cn.levey.bannerlib.base.BannerUtil;
import cn.levey.bannerlib.base.MarqueeTextView;
import cn.levey.bannerlib.base.RxBannerConfig;
import cn.levey.bannerlib.base.RxBannerLogger;
import cn.levey.bannerlib.data.RxBannerAdapter;
import cn.levey.bannerlib.impl.RxBannerChangeListener;
import cn.levey.bannerlib.impl.RxBannerClickListener;
import cn.levey.bannerlib.impl.RxBannerLoaderInterface;
import cn.levey.bannerlib.impl.RxBannerTitleClickListener;
import cn.levey.bannerlib.manager.AutoPlayRecyclerView;
import cn.levey.bannerlib.manager.ScaleLayoutManager;
import cn.levey.bannerlib.manager.ViewPagerLayoutManager;

/**
 * Created by Levey on 2018/4/2 15:11.
 * e-mail: m@levey.cn
 */

public class RxBanner extends FrameLayout {

    private Context mContext;
    private int parentWidth, parentHeight;
    private RxBannerAdapter mAdapter;
    private AutoPlayRecyclerView mBannerRv;
    private MarqueeTextView mTitleTv;
    private ScaleLayoutManager mLayoutManager;
    private int timeInterval = RxBannerConfig.getInstance().getTimeInterval();
    private RxBannerConfig.OrderType orderType;
    private boolean autoPlay = true;
    private boolean viewPaperMode;
    private boolean infinite;
    private float itemScale;
    private int titleGravity,titleLayoutGravity;
    private int titleMargin,titleMarginTop,titleMarginBottom,titleMarginStart,titleMarginEnd;
    private int titlePadding,titlePaddingTop,titlePaddingBottom,titlePaddingStart,titlePaddingEnd;
    private int titleWidth,titleHeight,titleSize;
    private int titleColor, titleBackgroundColor,titleBackgroundResource;
    private boolean titleMarquee;
    private int itemPercent;
    private int itemSpace;
    private int orientation;
    private List<Object> mUrls = new ArrayList<>();
    private List<String> mTitles = new ArrayList<>();
    private RxBannerClickListener onBannerClickListener;
    private RxBannerLoaderInterface mLoader;
    private RxBannerTitleClickListener onTitleClickListener;
    private RecyclerView.ItemAnimator itemAnimator;
    private boolean needStart = false;

    public RxBanner(@NonNull Context context) {
        this(context, null);
    }

    public RxBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RxBanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView(context, attrs);
    }

    protected void initView(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RxBanner);
        orientation = typedArray.getInt(R.styleable.RxBanner_orientation, LinearLayout.HORIZONTAL);
        viewPaperMode = typedArray.getBoolean(R.styleable.RxBanner_itemPercent, true);
        infinite = typedArray.getBoolean(R.styleable.RxBanner_infinite, true);
        itemPercent = typedArray.getInt(R.styleable.RxBanner_itemPercent, 100);
        itemSpace = typedArray.getDimensionPixelSize(R.styleable.RxBanner_itemSpace, 0);
        itemScale = typedArray.getFloat(R.styleable.RxBanner_itemScale, 1.0f);
        timeInterval = typedArray.getInt(R.styleable.RxBanner_timeInterval, RxBannerConfig.getInstance().getTimeInterval());
        if (itemPercent <= 0) throw new IllegalArgumentException(RxBannerLogger.LOGGER_TAG + ": itemPercent should be greater than 0");
        if (timeInterval < 200) throw new IllegalArgumentException(RxBannerLogger.LOGGER_TAG + ": for better performance, timeInterval should be greater than 200 millisecond");
        orderType = BannerUtil.getOrder(typedArray.getInt(R.styleable.RxBanner_orderType,
                BannerUtil.getOrderType(RxBannerConfig.getInstance().getOrderType())));

        //title
        titleGravity = typedArray.getInt(R.styleable.RxBanner_titleGravity, Gravity.CENTER);
        titleLayoutGravity = typedArray.getInt(R.styleable.RxBanner_titleLayoutGravity, Gravity.CENTER_VERTICAL|Gravity.BOTTOM);
        titleMargin = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleMargin, 0);
        titleMarginTop = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleMarginTop, 0);
        titleMarginBottom = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleMarginBottom, 0);
        titleMarginStart = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleMarginStart, 0);
        titleMarginEnd = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleMarginEnd, 0);

        titlePadding = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titlePadding, BannerUtil.dp2px(mContext,3));
        titlePaddingTop = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titlePaddingTop, 0);
        titlePaddingBottom = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titlePaddingBottom, 0);
        titlePaddingStart = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titlePaddingStart,0);
        titlePaddingEnd = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titlePaddingEnd, 0);

        titleWidth = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        titleHeight = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleHeight, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleSize = typedArray.getDimensionPixelSize(R.styleable.RxBanner_titleSize, BannerUtil.sp2px(mContext,14));
        titleColor = typedArray.getColor(R.styleable.RxBanner_titleColor, Color.WHITE);
        titleBackgroundColor = typedArray.getColor(R.styleable.RxBanner_titleBackgroundColor, Color.parseColor("#0e000000"));
        titleBackgroundResource = typedArray.getResourceId(R.styleable.RxBanner_titleBackgroundResource, Integer.MAX_VALUE);
        titleMarquee = typedArray.getBoolean(R.styleable.RxBanner_titleMarquee, false);
        typedArray.recycle();
        initView();
    }

    protected void initView() {
        mBannerRv = new AutoPlayRecyclerView(mContext);
        LayoutParams layoutParams = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        addView(mBannerRv, layoutParams);
        mLayoutManager = new ScaleLayoutManager.Builder(mContext, itemSpace).build();
        mLayoutManager.setOrientation(orientation);
        mLayoutManager.setItemScale(itemScale);
        mLayoutManager.setInfinite(infinite);
        if (itemAnimator != null) mBannerRv.setItemAnimator(itemAnimator);
        mTitleTv = new MarqueeTextView(mContext);
        LayoutParams titleLayoutParams = new LayoutParams(titleWidth, titleHeight);
        if(titleMarginTop == 0) titleMarginTop = titleMargin;
        if(titleMarginBottom == 0) titleMarginBottom = titleMargin;
        if(titleMarginStart == 0) titleMarginStart = titleMargin;
        if(titleMarginEnd == 0) titleMarginEnd = titleMargin;
        titleLayoutParams.setMargins(titleMarginStart,titleMarginTop,titleMarginEnd,titleMarginBottom);

        titleLayoutParams.gravity = titleLayoutGravity;
        mTitleTv.setLayoutParams(titleLayoutParams);
        mTitleTv.setTextColor(titleColor);
        mTitleTv.setBackgroundColor(titleBackgroundColor);
        mTitleTv.setGravity(titleGravity);
        mTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_PX,titleSize);

        if(titlePadding != 0) mTitleTv.setPadding(titlePadding,titlePadding,titlePadding,titlePadding);
        if(titlePaddingTop == 0) titlePaddingTop = titlePadding;
        if(titlePaddingBottom == 0) titlePaddingBottom = titlePadding;
        if(titlePaddingStart == 0) titlePaddingStart = titlePadding;
        if(titlePaddingEnd == 0) titlePaddingEnd = titlePadding;
        mTitleTv.setPadding(titlePaddingStart,titlePaddingTop,titlePaddingEnd,titlePaddingBottom);

        RxBannerLogger.i("titleBackgroundResource = " + titleBackgroundResource);
        if(titleBackgroundResource != Integer.MAX_VALUE) mTitleTv.setBackgroundResource(titleBackgroundResource);

        if(titleMarquee) {
            mTitleTv.setFocused(true);
            mTitleTv.setSingleLine(true);
            mTitleTv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            mTitleTv.setMarqueeRepeatLimit(-1);
        }else {
            mTitleTv.setFocused(false);
        }

        addView(mTitleTv,titleLayoutParams);
        mTitleTv.setVisibility(GONE);
        mLayoutManager.setOnInnerBannerChangeListener(new ViewPagerLayoutManager.OnInnerBannerChangeListener() {
            @Override
            public void onInnerBannerSelected(int position) {
                if(mTitleTv == null || mTitles.isEmpty()){
                    if(mTitleTv.getVisibility() == VISIBLE) mTitleTv.setVisibility(GONE);
                    return;
                }
                if(mUrls.size() == mTitles.size()){
                    mTitleTv.setText(mTitles.get(position));
                    if(mTitleTv.getVisibility() == GONE) mTitleTv.setVisibility(VISIBLE);
                }else {
                    if(mTitleTv.getVisibility() == VISIBLE) mTitleTv.setVisibility(GONE);
                }
            }

            @Override
            public void onInnerBannerScrollStateChanged(int state) {

            }
        });
    }

    public RxBanner setOnBannerChangeListener(RxBannerChangeListener onBannerChangeListener){
        if(mLayoutManager != null){
            mLayoutManager.setOnBannerChangeListener(onBannerChangeListener);
        }
        return this;
    }


    protected void initRvData() {
        if (mBannerRv != null) {
            mBannerRv.setLayoutManager(mLayoutManager);
            mBannerRv.setViewPaperMode(viewPaperMode);
            mBannerRv.setDirection(orderType);
            mBannerRv.setTimeInterval(timeInterval);
            mBannerRv.setAutoPlay(autoPlay);
        }
    }

    protected void initAdapter() {
        initRvData();
        if (mAdapter == null) {
            mAdapter = new RxBannerAdapter(mContext, orientation, getPercentSize());
            mAdapter.setLoader(mLoader);
            mAdapter.setDatas(mUrls);
            mAdapter.setRxBannerClickListener(onBannerClickListener);
            mBannerRv.setAdapter(mAdapter);
        }


        if(onTitleClickListener != null){
            mTitleTv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(onTitleClickListener != null && mLayoutManager != null){
                        onTitleClickListener.onTitleClick(mLayoutManager.getCurrentPosition());
                    }
                }
            });
        }
    }

    public RxBanner setLoader(RxBannerLoaderInterface mLoader) {
        if (mAdapter != null) {
            mAdapter.setLoader(mLoader);
        } else {
            this.mLoader = mLoader;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public RxBanner setDatas(List<?> urls) {
        if (urls != null && !urls.isEmpty()) {
            this.mUrls.clear();
            this.mUrls.addAll(urls);
            if (mBannerRv != null && mAdapter != null && mAdapter.getDatas() != null && !mAdapter.getDatas().isEmpty()) {
                restart();
            }
        }
        return this;
    }

    public RxBanner setDatas(List<?> urls,List<String> titles) {
        if(urls.size() != titles.size()){
            throw new IllegalStateException("urls size not equal to titles size");
        }
        setDatas(urls);
        setTitles(titles);
        return this;
    }

    public void addDatas(List<?> urls,List<String> titles) {
        if(urls.size() != titles.size()){
            throw new IllegalStateException("urls size not equal to titles size");
        }
        addDatas(urls);
        addTitles(titles);
    }

    public void addDatas(List<?> urls) {
        if (urls != null && !urls.isEmpty()) {
            this.mUrls.addAll(urls);
        }
    }


    protected void setTitles(List<String> titles) {
        if(titles != null && !titles.isEmpty()){
            this.mTitles.clear();
            this.mTitles.addAll(titles);
        }
    }

    public RxBanner setOnBannerTitleClickListener(final RxBannerTitleClickListener onTitleClickListener){
        this.onTitleClickListener = onTitleClickListener;
        return this;
    }

    protected void addTitles(List<String> titles) {
        if (titles != null && !titles.isEmpty()) {
            this.mTitles.addAll(titles);
        }
    }

    public RxBanner setOrderType(RxBannerConfig.OrderType orderType) {
        if (mBannerRv != null) {
            mBannerRv.setDirection(orderType);
        }
        this.orderType = orderType;
        RxBannerLogger.i("setOrderType = " + timeInterval);
        return this;
    }

    public RxBanner setTimeInterval(int timeInterval) {
        if (mBannerRv != null) {
            mBannerRv.setTimeInterval(timeInterval);
        }
        this.timeInterval = timeInterval;
        RxBannerLogger.i("setTimeInterval = " + timeInterval);
        return this;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    public RxBannerConfig.OrderType getOrderType() {
        return orderType;
    }


    protected void initStart() {
        initAdapter();
        if (mBannerRv != null && mAdapter != null && !mAdapter.getDatas().isEmpty() && isAutoPlay() && needStart) {
            mBannerRv.start();
            RxBannerLogger.i("start success");
        } else if (!isAutoPlay()) {
            RxBannerLogger.i("can not start, auto play = false");
        } else {
            RxBannerLogger.i("please call start() to start banner");
        }
    }

    public void start() {
        needStart = true;
        if (isParentCreate() && mBannerRv != null && mAdapter != null && autoPlay) {
            mBannerRv.start();
        }
    }


    public void onStop(){
        pause();
    }

    public void onDestroy(){
        pause();
        if(mAdapter != null) mAdapter = null;
        if(mBannerRv != null) mBannerRv.destroyDrawingCache();
    }

    public void onResume(){
        start();
    }

    public void pause() {
        if (mBannerRv != null && autoPlay) {
            mBannerRv.pause();
        }
    }

    protected void restart() {
        if(autoPlay) {
            pause();
            start();
        }
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public RxBanner setAutoPlay(boolean autoPlay) {
        if (mBannerRv != null && mAdapter != null) {
            mBannerRv.setAutoPlay(autoPlay);
        }
        this.autoPlay = autoPlay;
        RxBannerLogger.i("setAutoPlay = " + autoPlay);
        return this;
    }

    public void setCurrentPosition(int position) {
        if (mBannerRv != null && mAdapter != null && !mAdapter.getDatas().isEmpty() && position >= 0) {
            mBannerRv.smoothScrollToPosition(position);
            if(autoPlay && mBannerRv.isRunning()){
                mBannerRv.pause();
                mBannerRv.start();
            }
        }
    }

    public int getCurrentPosition() {
        if (mBannerRv != null && mAdapter != null && !mAdapter.getDatas().isEmpty()) {
            return mLayoutManager.getCurrentPosition();
        }
        return -1;
    }

    public RxBanner setOnBannerClickListener(RxBannerClickListener onClickListener) {
        if (mAdapter != null) {
            mAdapter.setRxBannerClickListener(onClickListener);
        }
        this.onBannerClickListener = onClickListener;
        return this;
    }

    private int getPercentSize() {
        int percentSize = -1;
        if (orientation == LinearLayout.VERTICAL) {
            if (parentHeight == -1) return -1;
            percentSize = BannerUtil.getPercentSize(parentHeight, itemPercent);
        } else if (orientation == LinearLayout.HORIZONTAL) {
            if (parentWidth == -1) return -1;
            percentSize = BannerUtil.getPercentSize(parentWidth, itemPercent);
        }
        return percentSize;
    }

    public RxBanner setItemAnimator(RecyclerView.ItemAnimator itemAnimator) {
        if (mBannerRv != null) {
            mBannerRv.setItemAnimator(itemAnimator);
        }
        this.itemAnimator = itemAnimator;
        return this;
    }

    protected boolean isParentCreate() {
        return parentWidth != 0 && parentWidth != -1 && parentHeight != 0 && parentHeight != -1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final View v;
        try {
            v = getChildAt(0);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (v.getWidth() != 0 && v.getHeight() != 0) {
                    if (parentWidth == 0 || parentWidth == -1) parentWidth = v.getWidth();
                    if (parentHeight == 0 || parentHeight == -1) parentHeight = v.getHeight();
                    if (parentWidth != 0 && parentWidth != -1 && parentHeight != 0 && parentHeight != -1 && needStart)
                        initStart();
                }
            }
        });
    }
}
