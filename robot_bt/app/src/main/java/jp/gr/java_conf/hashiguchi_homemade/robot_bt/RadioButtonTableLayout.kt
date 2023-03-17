package jp.gr.java_conf.hashiguchi_homemade.robot_bt

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TableLayout
import android.widget.TableRow

private const val TAG = "RadioButtonTableLayout"

class RadioButtonTableLayout : TableLayout, View.OnClickListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private lateinit var activeRadioButton: RadioButton
    override fun onClick(v: View?) {
        check(v)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        if (child is TableRow) {
            setChildrenOnClickListener(child)
        }
    }

    private fun setChildrenOnClickListener(tr: TableRow) {
        val count = tr.childCount
        for (i in 0 until count) {
            val v: View = tr.getChildAt(i)
            if (v is RadioButton) {
                v.setOnClickListener(this)
            }
        }
    }

    fun getCheckedRadioButtonId(): Int {
        if (::activeRadioButton.isInitialized) {
            return activeRadioButton.id
        }
        return -1
    }

    fun check(id: Int) {
        val v = findViewById<RadioButton>(id)
        check(v)
    }

    private fun check(v: View?) {
        if (v is RadioButton) {
            if (::activeRadioButton.isInitialized) {
                activeRadioButton.isChecked = false
            }
            v.isChecked = true
            activeRadioButton = v
        }
    }
}