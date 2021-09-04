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
import android.view.MotionEvent
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

    var editorCount: Int = DEFAULT_EDITOR_COUNT
        private set
    var editorHeight: Int = DEFAULT_EDITOR_SIZE
        private set
    var editorWidth: Int = DEFAULT_EDITOR_SIZE
        private set
    var editorDividerWidth: Int = DEFAULT_EDITOR_DIVIDER_SIZE
        private set
    var editorCursorVisible: Boolean = false
        private set
    var editorInputMasked: Boolean = false
        private set
    var editorForceKeyboard: Boolean = true
    var onlyNotifyUserInput: Boolean = true

    @DrawableRes
    var editorBackground: Int = 0
        private set
    var editorTextSize: Float = DEFAULT_EDITOR_TEXT_SIZE
        private set

    @ColorInt
    var editorTextColor: Int = DEFAULT_EDITOR_TEXT_COLOR
        private set
    var hint: String? = null
        private set
    var inputType: InputType = InputType.TEXT
        private set

    private var isFinalNumberPin = false
    private var isFromSetValue = false

    private val editors = mutableListOf<EditText>()

    private var onClickLister: OnClickListener? = null
    var onInputListener: OnInputListener? = null
    var currentFocusView: View? = null
    private val lengthFilters = Array<InputFilter?>(1) { null }
    private var layoutParams: LinearLayout.LayoutParams? = null
    private var deletePressed = false

    init {
        setWillNotDraw(false)
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
        editorInputMasked = a.getBoolean(R.styleable.PinView_editorInputMasked, false)
        editorForceKeyboard = a.getBoolean(R.styleable.PinView_editorForceKeyboard, true)
        onlyNotifyUserInput = a.getBoolean(R.styleable.PinView_onlyNotifyUserInput, true)
        hint = a.getString(R.styleable.PinView_hint)
        inputType = InputType.values()[a.getInt(R.styleable.PinView_inputType, 0)]
        layoutParams = LinearLayout.LayoutParams(editorWidth, editorHeight)
        a.recycle()
        createEditors()
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
        editors.forEach { it.keyListener = if (enabled) DefaultNumberKeyListener() else null }
    }

    private fun createEditors() {
        removeAllViews()
        editors.clear()
        var editor: EditText?
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
        lengthFilters[0] = InputFilter.LengthFilter(1)
        editor.filters = lengthFilters
        editor.layoutParams = layoutParams
        editor.gravity = Gravity.CENTER
        editor.isCursorVisible = editorCursorVisible
        editor.setTextColor(editorTextColor)
        editor.setTextSize(TypedValue.COMPLEX_UNIT_PX, editorTextSize)
        if (!editorCursorVisible) {
            editor.isClickable = false
            editor.hint = hint
            editor.setOnTouchListener { _, _ ->
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
        InputType.NUMBER -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> android.text.InputType.TYPE_CLASS_TEXT
    }

    private fun setTransformation() {
        editors.forEach { editor ->
            editor.removeTextChangedListener(this)
            editor.transformationMethod = if (!editorInputMasked) null else PinTransformationMethod()
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
                if (!onlyNotifyUserInput) {
                    onInputListener?.onInput(this, value[i])
                }
            } else {
                editors[i].setText("")
            }
        }
        if (editorCount > 0) {
            if (lastOfHavingValue < editorCount - 1) {
                currentFocusView = editors[lastOfHavingValue + 1]
            } else {
                currentFocusView = editors.last()
                if (inputType == InputType.NUMBER || editorInputMasked) {
                    isFinalNumberPin = true
                }
                if (value.length == editorCount) {
                    onInputListener?.onFinish(this, false)
                }
            }
            currentFocusView?.requestFocus()
        }
        isFromSetValue = false
        updateEnabledState()
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
            val index = indexOfFocusEditor()
            if ((inputType == InputType.NUMBER || editorInputMasked) && index == editorCount - 1 && isFinalNumberPin) {
                if (editors[index].length() > 0) {
                    editors[index].setText("")
                }
                isFinalNumberPin = false
            } else if (index > 0) {
                deletePressed = true
                if (editors[index].length() == 0) {
                    editors[index - 1].requestFocus()
                }
                editors[index].setText("")
            } else {
                if (editors[index].length() > 0) {
                    editors[index].setText("")
                }
            }
            return true
        }
        return false
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus && !editorCursorVisible) {
            if (deletePressed) {
                currentFocusView = v
                deletePressed = false
                return
            }
            for (editor in editors) {
                if (editor.length() == 0) {
                    if (editor == v) {
                        currentFocusView = v
                    } else {
                        editor.requestFocus()
                    }
                    return
                }
            }
            if (editors.last() != v) {
                editors.last().requestFocus()
            } else {
                currentFocusView = v
            }
        } else if (hasFocus && editorCursorVisible) {
            currentFocusView = v
        } else {
            v?.clearFocus()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        onClickLister = l
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s?.length == 1 && currentFocusView != null) {
            val index = indexOfFocusEditor()
            if (index < editorCount - 1) {
                val delay = if (editorInputMasked) 25L else 1L
                this.postDelayed({
                    val nextEditor = editors[index + 1]
                    nextEditor.isEnabled = true
                    nextEditor.requestFocus()
                }, delay)
            }
            if (index == editorCount - 1 && (inputType == InputType.NUMBER || editorInputMasked)) {
                isFinalNumberPin = true
            }
        } else if (s?.length == 0) {
            val index = indexOfFocusEditor()
            deletePressed = true
            if (editors[index].length() > 0) {
                editors[index].setText("")
            }
        }
        for (i in 0 until editorCount) {
            if (editors[i].length() < 1) {
                break
            }
            if (!isFromSetValue && i + 1 == editorCount) {
                onInputListener?.onFinish(this, true)
            }
        }
        updateEnabledState()
    }

    override fun afterTextChanged(s: Editable?) {}

    fun setEditorDividerWidth(width: Int) {
        editorDividerWidth = width
        layoutParams?.setMargins(editorDividerWidth / 2)
        editors.forEach { it.layoutParams = layoutParams }
    }

    fun setEditorHeight(height: Int) {
        editorHeight = height
        layoutParams?.height = editorHeight
        editors.forEach { it.layoutParams = layoutParams }
    }

    fun setEditorWidth(width: Int) {
        editorWidth = width
        layoutParams?.width = editorWidth
        editors.forEach { it.layoutParams = layoutParams }
    }

    fun setEditorCount(count: Int) {
        editorCount = count
        createEditors()
    }

    fun setEditorInputMasked(masked: Boolean) {
        editorInputMasked = masked
        setTransformation()
    }

    fun setHint(hint: String?) {
        this.hint = hint
        editors.forEach { it.hint = this.hint }
    }

    fun setEditorBackground(@DrawableRes backgroundRes: Int) {
        editorBackground = backgroundRes
        editors.forEach { it.setBackgroundResource(editorBackground) }
    }

    fun setInputType(inputType: InputType) {
        this.inputType = inputType
        val type = editorInputType()
        editors.forEach { it.inputType = type }
    }

    private inner class PinTransformationMethod : TransformationMethod {
        override fun getTransformation(source: CharSequence?, view: View?): CharSequence =
            PasswordCharSequence(source)

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