package com.alvinhkh.buseta.view.common;

/**
 * Copyright 2015 Bartosz Lipinski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.IntDef;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

import java.lang.ref.WeakReference;

public class ControllableAppBarLayout extends AppBarLayout {
    private AppBarLayout.Behavior mBehavior;
    private WeakReference<CoordinatorLayout> mParent;
    private @ToolbarChange int mQueuedChange = TOOLBARCHANGE_NONE;
    private boolean mAfterFirstDraw = false;

    public ControllableAppBarLayout(Context context) {
        super(context);
    }

    public ControllableAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!(getLayoutParams() instanceof CoordinatorLayout.LayoutParams) || !(getParent() instanceof CoordinatorLayout)) {
            throw new IllegalStateException("ControllableAppBarLayout must be a direct child of CoordinatorLayout.");
        } else {
            mParent = new WeakReference<CoordinatorLayout>((CoordinatorLayout) getParent());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mBehavior == null) {
            mBehavior = (Behavior) ((CoordinatorLayout.LayoutParams) getLayoutParams()).getBehavior();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (r - l > 0 && b - t > 0 && mAfterFirstDraw && mQueuedChange != TOOLBARCHANGE_NONE) {
            analyzeQueuedChange();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mAfterFirstDraw) {
            mAfterFirstDraw = true;
            if (mQueuedChange != TOOLBARCHANGE_NONE) {
                analyzeQueuedChange();
            }
        }
    }

    private synchronized void analyzeQueuedChange() {
        switch (mQueuedChange) {
            case TOOLBARCHANGE_COLLAPSE:
                performCollapsingWithoutAnimation();
                break;
            case TOOLBARCHANGE_COLLAPSE_WITH_ANIMATION:
                performCollapsingWithAnimation();
                break;
            case TOOLBARCHANGE_EXPAND:
                performExpandingWithoutAnimation();
                break;
            case TOOLBARCHANGE_EXPAND_WITH_ANIMATION:
                performExpandingWithAnimation();
                break;
        }

        mQueuedChange = TOOLBARCHANGE_NONE;
    }

    public void collapseToolbar() {
        collapseToolbar(false);
    }

    public void collapseToolbar(boolean withAnimation) {
        mQueuedChange = withAnimation ? TOOLBARCHANGE_COLLAPSE_WITH_ANIMATION : TOOLBARCHANGE_COLLAPSE;
        requestLayout();
    }

    public void expandToolbar() {
        expandToolbar(false);
    }

    public void expandToolbar(boolean withAnimation) {
        mQueuedChange = withAnimation ? TOOLBARCHANGE_EXPAND_WITH_ANIMATION : TOOLBARCHANGE_EXPAND;
        requestLayout();
    }

    private void performCollapsingWithoutAnimation() {
        if (mParent.get() != null) {
            mBehavior.onNestedPreScroll(mParent.get(), this, null, 0, 256, new int[]{0, 0});
        }
    }

    private void performCollapsingWithAnimation() {
        if (mParent.get() != null) {
            mBehavior.onNestedFling(mParent.get(), this, null, 0, getHeight(), true);
        }
    }

    private void performExpandingWithoutAnimation() {
        if (mParent.get() != null) {
            mBehavior.setTopAndBottomOffset(0);
        }
    }

    private void performExpandingWithAnimation() {
        if (mParent.get() != null) {
            mBehavior.onNestedFling(mParent.get(), this, null, 0, -getHeight() * 5, false);
        }
    }

    @IntDef({TOOLBARCHANGE_COLLAPSE, TOOLBARCHANGE_COLLAPSE_WITH_ANIMATION, TOOLBARCHANGE_EXPAND, TOOLBARCHANGE_EXPAND_WITH_ANIMATION, TOOLBARCHANGE_NONE})
    public @interface ToolbarChange {}

    public static final int TOOLBARCHANGE_COLLAPSE = 0;
    public static final int TOOLBARCHANGE_COLLAPSE_WITH_ANIMATION = 1;
    public static final int TOOLBARCHANGE_EXPAND = 2;
    public static final int TOOLBARCHANGE_EXPAND_WITH_ANIMATION = 3;
    public static final int TOOLBARCHANGE_NONE = 4;
}