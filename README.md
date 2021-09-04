# PinView
PIN码输入控件. 可输入密码或者字符.

Effect Graph
-------
<img src="/media/pin_view.gif" width="360" height="800"/>

How to use?
------
<br>In xml files, 
```
        <me.bytebeats.views.pinview.PinView
            app:editorCount="6"
            android:id="@+id/pin_view"
            app:editorCursorVisible="true"
            app:editorDividerWidth="15dp"
            app:editorTextColor="@color/white"
            app:editorInputMasked="false"
            app:editorForceKeyboard="false"
            app:editorTextSize="20sp"
            app:editorWidth="30dp"
            app:inputType="number"
            app:hint="#"
            app:editorHeight="30dp"
            android:orientation="horizontal"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />
```
<br>In codes, 
```
        pinView.setEditorBackground(R.drawable.default_editor_background)
        pinView.setEditorCount(6)
        pinView.setEditorWidth(30)
        pinView.setEditorHeight(30)
        pinView.setEditorDividerWidth(10)
        pinView.setEditorInputMasked(false)
        pinView.setHint("")
        pinView.setInputType(InputType.NUMBER)
        pinView.setOnClickListener { 
            
        }
        pinView.onInputListener = object : OnInputListener {
            override fun onFinish(view: PinView, fromUser: Boolean) {
                if (pinView.value() == editor.text.toString()) {
                    Toast.makeText(this@MainActivity, "Login", Toast.LENGTH_SHORT).show()
                } else {
                    tryTimes++
                    if (tryTimes >= 3) {
                        pinView.clear()
                        pinView.enableInput(false)
                        timer?.cancel()
                        timer = PinCountDownTimer(5000, 1000)
                        timer?.start()
                        return
                    }
                    prompt.text = "Error: ${3 - tryTimes} times"
                }
            }

            override fun onInput(view: PinView, char: Char) {

            }
        }
```

Features
------
- 支持更改 PinView 中每一个 EditText 的输入前、输入后、正在输入的 background
- 支持设置任意长度的 PIN 码长度
- 支持设置可输入的 PIN 码格式，目前支持文本、数字, 可以自主设置
- 支持设置输入错误次数限制时间
- 支持设置输入的 PIN 码是否以明文显示
- 支持输入默认值和清除数据
- 支持设置可输入长度和每一个 EditText 的宽高和背景
