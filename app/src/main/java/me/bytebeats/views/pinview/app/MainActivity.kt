package me.bytebeats.views.pinview.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import me.bytebeats.views.pinview.OnInputListener
import me.bytebeats.views.pinview.PinView

class MainActivity : AppCompatActivity() {
    private val editor by lazy { findViewById<EditText>(R.id.editor) }
    private val pinView by lazy { findViewById<PinView>(R.id.pin_view) }
    private val prompt by lazy { findViewById<TextView>(R.id.prompt) }
    private var timer: CountDownTimer? = null
    private var tryTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pinView.onInputListener = object : OnInputListener {
            override fun onFinish(view: PinView, fromUser: Boolean) {
                if (pinView.value() == editor.text.toString()) {
                    Toast.makeText(this@MainActivity, "Login", Toast.LENGTH_SHORT).show()
                } else {
                    tryTimes++
                    if (tryTimes > 5) {
                        pinView.clear()
                        pinView.enableInput(false)
                        timer?.cancel()
                        timer = PinCountDownTimer(10000, 1000)
                        timer?.start()
                        return
                    }
                    prompt.text = "Error: ${5 - tryTimes} times"
                }
            }

            override fun onInput(view: PinView, char: Char) {

            }
        }
    }

    private inner class PinCountDownTimer(millisInFuture: Long, countDownInterval: Long) :
            CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {
            prompt.text = "Try in ${millisUntilFinished / 1000}s"
        }

        override fun onFinish() {
            prompt.text = ""
            tryTimes = 0
            pinView.enableInput(true)
        }
    }
}