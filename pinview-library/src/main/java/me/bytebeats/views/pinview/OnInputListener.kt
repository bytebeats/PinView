package me.bytebeats.views.pinview

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created on 2021/9/3 10:40
 * @Version 1.0
 * @Description TO-DO
 */

interface OnInputListener {
    fun onInput(view: PinView, char: Char)
    fun onFinish(view: PinView, fromUser: Boolean)
}