package com.velocity.launcher.data

import android.graphics.drawable.Drawable
import android.os.UserHandle

data class AppModel(
    val label: String,
    val packageName: String,
    val className: String,
    val userHandle: UserHandle,
    var iconDrawable: Drawable? = null,
    val isWorkProfile: Boolean = false,
    var assignedFocus: FocusMode = FocusMode.ALL,
    var isFavorite: Boolean = false,
    var isHidden: Boolean = false
)
