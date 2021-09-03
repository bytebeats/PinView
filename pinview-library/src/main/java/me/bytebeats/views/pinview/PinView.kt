package me.bytebeats.views.pinview

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/9/3 10:37
 * @Version 1.0
 * @Description TO-DO
 */

class PinView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), TextWatcher, View.OnFocusChangeListener, View.OnKeyListener {
    private val DENSITY = context.resources.displayMetrics.density

    var editorCount: Int = 0
    var editorHeight: Int = DEFAULT_EDITOR_SIZE
    var editorWidth: Int = DEFAULT_EDITOR_SIZE
    var editorDividerWidth: Int = DEFAULT_EDITOR_DIVIDER_SIZE
    var editorCursorVisible: Boolean = false
    var editorInputVisible: Boolean = false
    var editorForceKeyboard: Boolean = true

    @DrawableRes
    var editorBackground: Int = 0
    var editorTextSize: Float = DEFAULT_EDITOR_TEXT_SIZE

    @ColorInt
    var editorTextColor: Int = DEFAULT_EDITOR_TEXT_COLOR
    var hint: String? = null
    var inputType: InputType = InputType.TEXT

    private var isFinalNumberPin = false
    private var isFromSetValue = false

    private val editors = mutableListOf<EditText>()

    private var onClickLister: OnClickListener? = null
    var currentFocusView: View? = null
    private val filters = Array<InputFilter>(1) { InputFilter.LengthFilter(1) }
    private lateinit var layoutParams: LinearLayout.LayoutParams

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, 0)
        a.recycle()
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        TODO("Not yet implemented")
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        TODO("Not yet implemented")
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        TODO("Not yet implemented")
    }

    override fun afterTextChanged(s: Editable?) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val DEFAULT_EDITOR_SIZE = 50
        private const val DEFAULT_EDITOR_DIVIDER_SIZE = 20
        private const val DEFAULT_EDITOR_TEXT_SIZE = 30F
        private const val DEFAULT_EDITOR_TEXT_COLOR = 0XFFFFFF
    }
}