package iq.lawha

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle

class KeyboardService : InputMethodService() {

    private val imeOwner = ImeLifecycleOwner()
    // يُبنى مرّة وحدة فقط ويُعاد استخدامه؛ Android يعيد استدعاء onCreateInputView()
    // بمناسبات كثيرة (تغيير الاتجاه، تبديل التطبيق...) وبناء/تهيئة ComposeView من جديد
    // كل مرة كان يسبب كراش حقيقي (SavedStateRegistry يُستعاد مرتين + قفزة غير صالحة بدورة الحياة).
    private var rootView: ComposeView? = null
    private val skinState = androidx.compose.runtime.mutableStateOf(Skin.IOS_DARK)

    override fun onCreate() {
        CrashLog.install(this)
        super.onCreate()
        // لا نرسل ON_CREATE هنا: لازم يصير composition (setContent) أول وهو بمرحلة
        // "التهيئة" (INITIALIZED) قبل أي حدث ON_CREATE، وإلا Compose يرمي استثناء حقيقي
        // "Restarter must be created only during owner's initialization stage" —
        // نفس آلية ComponentActivity بالضبط (performRestore + setContent قبل ON_CREATE).
    }

    override fun onCreateInputView(): View {
        rootView?.let { return it }
        val view = ComposeView(this)
        imeOwner.attach(view)
        view.setContent {
            val skin by skinState
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
        rootView = view
        imeOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        skinState.value = Prefs.getSkin(this)
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
        rootView = null
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
        } else {
            @Suppress("DEPRECATION")
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            val token = window?.window?.attributes?.token
            if (token != null) imm.switchToNextInputMethod(token, false)
        }
    }
}
