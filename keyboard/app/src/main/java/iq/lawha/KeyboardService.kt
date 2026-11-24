package iq.lawha

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle

class KeyboardService : InputMethodService() {

    private val imeOwner = ImeLifecycleOwner()

    override fun onCreateInputView(): View {
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val view = ComposeView(this)
        imeOwner.attach(view)
        view.setContent {
            val skin = Prefs.getSkin(this@KeyboardService)
            KeyboardScreen(
                skin = skin,
                onChar = { ch -> currentInputConnection?.commitText(ch, 1) },
                onDelete = { currentInputConnection?.deleteSurroundingText(1, 0) },
                onSpace = { currentInputConnection?.commitText(" ", 1) },
                onEnter = { sendEnter() },
                onGlobe = { switchKeyboard() },
                onMic = { Toast.makeText(this@KeyboardService, "الإملاء الصوتي قريباً", Toast.LENGTH_SHORT).show() },
            )
        }
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun sendEnter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun switchKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
        } else {
            @Suppress("DEPRECATION")
            imm.switchToNextInputMethod(window.window?.attributes?.token, false)
        }
    }
}
