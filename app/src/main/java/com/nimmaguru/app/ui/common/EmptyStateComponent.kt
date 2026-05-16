package com.nimmaguru.app.ui.common

import android.view.View
import android.widget.TextView

object EmptyStateComponent {
    fun bind(view: TextView, isVisible: Boolean, message: String) {
        view.text = message
        view.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
