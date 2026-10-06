package com.beouttv.app.core.tv

import android.view.KeyEvent

/**
 * Standard TV Remote Key Event Dispatcher and Action Classifier.
 * Translates low-level D-Pad, Media, Color, and Numpad key codes into high-level BeoutTV actions.
 */
sealed class TvRemoteAction {
    object TogglePlayPause : TvRemoteAction()
    object ShowHud : TvRemoteAction()
    object ToggleQualitySheet : TvRemoteAction()
    object ToggleAudioTrack : TvRemoteAction()
    object AddMultiViewChannel : TvRemoteAction()
    object CloseSlotOrPip : TvRemoteAction()
    object NextChannel : TvRemoteAction()
    object PreviousChannel : TvRemoteAction()
    object StepLeft : TvRemoteAction()
    object StepRight : TvRemoteAction()
    object StepUp : TvRemoteAction()
    object StepDown : TvRemoteAction()
    data class SelectSlotNumber(val slotNumber: Int) : TvRemoteAction()
    object Unhandled : TvRemoteAction()
}

object TvRemoteController {

    fun parseKeyEvent(event: KeyEvent): TvRemoteAction {
        if (event.action != KeyEvent.ACTION_DOWN) return TvRemoteAction.Unhandled

        return when (event.keyCode) {
            // Center OK / Media Play/Pause
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> TvRemoteAction.TogglePlayPause

            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> TvRemoteAction.TogglePlayPause

            // D-Pad Arrows
            KeyEvent.KEYCODE_DPAD_UP -> TvRemoteAction.StepUp
            KeyEvent.KEYCODE_DPAD_DOWN -> TvRemoteAction.StepDown
            KeyEvent.KEYCODE_DPAD_LEFT -> TvRemoteAction.StepLeft
            KeyEvent.KEYCODE_DPAD_RIGHT -> TvRemoteAction.StepRight

            // Remote Color Keys (Standards on Samsung, LG, Sony, Android TV, Fire TV remotes)
            KeyEvent.KEYCODE_PROG_YELLOW,
            KeyEvent.KEYCODE_F3 -> TvRemoteAction.ToggleQualitySheet

            KeyEvent.KEYCODE_PROG_BLUE,
            KeyEvent.KEYCODE_F4 -> TvRemoteAction.ToggleAudioTrack

            KeyEvent.KEYCODE_PROG_GREEN,
            KeyEvent.KEYCODE_F2 -> TvRemoteAction.AddMultiViewChannel

            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_F1 -> TvRemoteAction.CloseSlotOrPip

            // Channel Up / Down
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_PAGE_UP -> TvRemoteAction.NextChannel

            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> TvRemoteAction.PreviousChannel

            // Number Keys 1..4 (Direct Multi-View slot selection)
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> TvRemoteAction.SelectSlotNumber(0)
            KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> TvRemoteAction.SelectSlotNumber(1)
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> TvRemoteAction.SelectSlotNumber(2)
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> TvRemoteAction.SelectSlotNumber(3)

            // Menu button
            KeyEvent.KEYCODE_MENU -> TvRemoteAction.ShowHud

            else -> TvRemoteAction.Unhandled
        }
    }
}
