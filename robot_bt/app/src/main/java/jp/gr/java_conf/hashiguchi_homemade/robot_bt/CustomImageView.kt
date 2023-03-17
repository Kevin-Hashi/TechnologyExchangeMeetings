package jp.gr.java_conf.hashiguchi_homemade.robot_bt

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class CustomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}