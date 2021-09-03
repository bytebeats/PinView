package me.bytebeats.views.pinview

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.NumberKeyListener
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.setMargins
import androidx.core.view.setPadding

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

    var editorCount: Int = DEFAULT_EDITOR_COUNT
    var editorHeight: Int = DEFAULT_EDITOR_SIZE
    var editorWidth: Int = DEFAULT_EDITOR_SIZE
    var editorDividerWidth: Int = DEFAULT_EDITOR_DIVIDER_SIZE
    var editorCursorVisible: Boolean = false
    var mPassword: Boolean = false
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

    var onClickLister: OnClickListener? = null
    var onInputListener: OnInputListener? = null
    var currentFocusView: EditText? = null
    private val filters = Array<InputFilter>(1) { InputFilter.LengthFilter(1) }
    private var layoutParams: LinearLayout.LayoutParams? = null
    private var deletePressed = false

    private val numberKeyListener by lazy { DefaultNumberKeyListener() }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PinView, defStyleAttr, 0)
        editorBackground = a.getResourceId(R.styleable.PinView_editorBackground, R.drawable.default_editor_background)
        editorCount = a.getInt(R.styleable.PinView_editorCount, DEFAULT_EDITOR_COUNT)
        editorWidth = a.getDimensionPixelOffset(R.styleable.PinView_editorWidth, DEFAULT_EDITOR_SIZE)
        editorHeight = a.getDimensionPixelOffset(R.styleable.PinView_editorHeight, DEFAULT_EDITOR_SIZE)
        editorDividerWidth =
            a.getDimensionPixelOffset(R.styleable.PinView_editorDividerWidth, DEFAULT_EDITOR_DIVIDER_SIZE)
        editorTextSize = a.getDimension(R.styleable.PinView_editorTextSize, DEFAULT_EDITOR_TEXT_SIZE)
        editorTextColor = a.getColor(R.styleable.PinView_editorTextColor, DEFAULT_EDITOR_TEXT_COLOR)
        editorCursorVisible = a.getBoolean(R.styleable.PinView_editorCursorVisible, false)
        mPassword = a.getBoolean(R.styleable.PinView_editorInputVisible, false)
        editorForceKeyboard = a.getBoolean(R.styleable.PinView_editorForceKeyboard, true)
        hint = a.getString(R.styleable.PinView_hint)
        inputType = InputType.values()[a.getInt(R.styleable.PinView_inputType, 0)]
        a.recycle()
        createEditors()
        setWillNotDraw(false)
        layoutParams = LinearLayout.LayoutParams(editorWidth, editorHeight)
        super.setOnClickListener {
            var focused = false
            for (editor in editors) {
                if (editor.length() == 0) {
                    editor.requestFocus()
                    popupKeyboard()
                    focused = true
                    break
                }
            }
            if (!focused && editors.isNotEmpty()) {
                editors.last().requestFocus()
            }
            onClickLister?.onClick(this@PinView)
        }
        editors.firstOrNull()?.postDelayed({ popupKeyboard() }, 200)
        updateEnabledState()
    }

    fun enableInput(enabled: Boolean) {
        this.isEnabled = enabled
        this.isFocusable = enabled
        editors.forEach { it.keyListener = if (enabled) numberKeyListener else null }
    }


    private fun createEditors() {
        removeAllViews()
        editors.clear()
        var editor: EditText? = null
        for (i in 0 until editorCount) {
            editor = EditText(context)
            editors.add(i, editor)
            this.addView(editor)
            stylizeEditor(editor, i.toString())
        }
        setTransformation()
    }

    private fun stylizeEditor(editor: EditText, tag: String) {
        layoutParams?.setMargins(editorDividerWidth / 2)
        editor.filters = filters
        editor.layoutParams = layoutParams
        editor.gravity = Gravity.CENTER
        editor.isCursorVisible = editorCursorVisible
        editor.setTextColor(editorTextColor)
        editor.setTextSize(TypedValue.COMPLEX_UNIT_PX, editorTextSize)
        if (!editorCursorVisible) {
            editor.isClickable = false
            editor.hint = hint
            editor.setOnTouchListener { v, event ->
                deletePressed = false
                false
            }
        }
        editor.setBackgroundResource(editorBackground)
        editor.setPadding(0)
        editor.tag = tag
        editor.inputType = editorInputType()
        editor.addTextChangedListener(this)
        editor.onFocusChangeListener = this
        editor.setOnKeyListener(this)
    }

    private fun editorInputType(): Int = when (inputType) {
        InputType.NUMBER -> android.text.InputType.TYPE_CLASS_NUMBER and android.text.InputType.TYPE_CLASS_PHONE
        else -> android.text.InputType.TYPE_CLASS_TEXT
    }

    private fun setTransformation() {
        editors.forEach { editor ->
            editor.removeTextChangedListener(this)
            editor.transformationMethod = if (!mPassword) null else PinTransformationMethod()
            editor.addTextChangedListener(this)
        }
    }

    private fun updateEnabledState() {
        val index = 0.coerceAtLeast(indexOfFocusEditor())
        for (i in editors.indices) {
            editors[i].isEnabled = index >= i
        }
    }

    private fun indexOfFocusEditor(): Int = editors.indexOf(currentFocusView)

    private fun popupKeyboard() {
        if (editorForceKeyboard) {
            val manager =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) context.getSystemService(InputMethodManager::class.java)
                else (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            manager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        editors.forEach { it.isEnabled = enabled }
    }

    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(focusable)
        editors.forEach { it.isEnabled = focusable }
    }

    override fun setFocusableInTouchMode(focusableInTouchMode: Boolean) {
        super.setFocusableInTouchMode(focusableInTouchMode)
        editors.forEach { it.isEnabled = focusableInTouchMode }
    }

    fun value(): String = editors.joinToString(separator = "") { it.text.toString() }

    fun requestPinEntryFocus(): EditText? {
        val index = 0.coerceAtLeast(indexOfFocusEditor())
        val focusEditor = editors.getOrNull(index)
        focusEditor?.requestFocus()
        popupKeyboard()
        return focusEditor
    }

    fun clear() {
        value("")
    }

    fun value(value: String) {
        val regex = "[0-9]*".toRegex()
        isFromSetValue = true
        if (inputType == InputType.NUMBER && !value.matches(regex)) {
            return
        }
        var lastOfHavingValue = -1
        for (i in editors.indices) {
            if (value.length > i) {
                lastOfHavingValue = i
                editors[i].setText(value[i].toString())
            } else {
                editors[i].setText("")
            }
        }
        if (editorCount > 0) {
            if (lastOfHavingValue < editorCount - 1) {
                currentFocusView = editors[lastOfHavingValue]
            } else {
                currentFocusView = editors.last()
                if (inputType == InputType.NUMBER || !mPassword) {
                    isFinalNumberPin = true
                } else {
                    onInputListener?.onEnter(this, false)
                }
            }
            currentFocusView?.requestFocus()
        }
        isFromSetValue = false
        updateEnabledState()
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

    private inner class PinTransformationMethod : TransformationMethod {
        override fun getTransformation(source: CharSequence?, view: View?): CharSequence =
            PasswordCharSequence((source))

        override fun onFocusChanged(
            view: View?,
            sourceText: CharSequence?,
            focused: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {

        }

        private inner class PasswordCharSequence(private val source: CharSequence?) : CharSequence {
            private val BULLET = '\u2022'

            override val length: Int
                get() = source?.length ?: 0

            override fun get(index: Int): Char = BULLET
            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
                PasswordCharSequence(source?.subSequence(startIndex, endIndex))
        }
    }

    private class DefaultNumberKeyListener : NumberKeyListener() {
        override fun getAcceptedChars(): CharArray = charArrayOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
        override fun getInputType(): Int = android.text.InputType.TYPE_CLASS_NUMBER
    }

    companion object {
        private const val DEFAULT_EDITOR_SIZE = 50
        private const val DEFAULT_EDITOR_DIVIDER_SIZE = 20
        private const val DEFAULT_EDITOR_TEXT_SIZE = 30F
        private const val DEFAULT_EDITOR_TEXT_COLOR = 0XFFFFFF
        private const val DEFAULT_EDITOR_COUNT = 4
    }
}