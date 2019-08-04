package com.alvinhkh.buseta.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.View


class FloatingActionButtonScrollBehavior(context: Context, attrs: AttributeSet) : FloatingActionButton.Behavior() {

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout,
                                     child: FloatingActionButton, directTargetChild: View, target: View,
                                     @ViewCompat.ScrollAxis axes: Int, @ViewCompat.NestedScrollType type: Int): Boolean {
        return type == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: FloatingActionButton,
                                target: View, dxConsumed: Int, dyConsumed: Int,
                                dxUnconsumed: Int, dyUnconsumed: Int, @ViewCompat.NestedScrollType type: Int) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (dyConsumed > 0 && child.visibility == View.VISIBLE) {
            child.hide(
                    // https://code.google.com/p/android/issues/detail?id=230298#c4
                    object : FloatingActionButton.OnVisibilityChangedListener() {
                        @SuppressLint("RestrictedApi")
                        override fun onHidden(fab: FloatingActionButton?) {
                            super.onHidden(fab)
                            fab?.visibility = View.INVISIBLE
                        }
                    }
            )
        } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
            child.show()
        }
    }

}