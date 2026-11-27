package iq.quiz.data

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/** تهيئة WebRTC مرّة وحدة لكل التطبيق. */
object RtcClient {
    @Volatile private var factory: PeerConnectionFactory? = null
    lateinit var eglBase: EglBase
        private set

    fun init(ctx: Context) {
        if (factory != null) return
        eglBase = EglBase.create()
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(ctx.applicationContext).createInitializationOptions(),
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun factory(): PeerConnectionFactory = factory!!
}

/** نداء حقيقي إلى حساب حقيقي: WebRTC فعلي + تفاوض (SDP/ICE) عبر Realtime Database. */
class RealCall(
    private val ctx: Context,
    val callId: String,
    val isCaller: Boolean,
    val video: Boolean,
    private val onRemoteVideo: (VideoTrack?) -> Unit,
    private val onState: (String) -> Unit, // "ringing" | "connected" | "ended"
) {
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference.child("calls").child(callId)
    private var pc: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localAudioSourceMuted = false
    private val listeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
    private var ended = false

    fun start(localView: SurfaceViewRenderer?, remoteView: SurfaceViewRenderer) {
        RtcClient.init(ctx)
        localView?.init(RtcClient.eglBase.eglBaseContext, null)
        remoteView.init(RtcClient.eglBase.eglBaseContext, null)

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            ),
        ).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }

        val factory = RtcClient.factory()
        pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate) {
                val path = if (isCaller) "callerCandidates" else "calleeCandidates"
                db.child(path).push().setValue(mapOf("sdp" to c.sdp, "sdpMid" to c.sdpMid, "sdpMLineIndex" to c.sdpMLineIndex))
            }
            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack) { track.addSink(remoteView); onRemoteVideo(track) }
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> onState("connected")
                    PeerConnection.IceConnectionState.FAILED, PeerConnection.IceConnectionState.CLOSED, PeerConnection.IceConnectionState.DISCONNECTED -> onState("ended")
                    else -> {}
                }
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {}
            override fun onAddStream(p0: MediaStream) {}
            override fun onRemoveStream(p0: MediaStream) {}
            override fun onDataChannel(p0: DataChannel) {}
            override fun onRenegotiationNeeded() {}
        })

        val audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("audio0", audioSource)
        pc?.addTrack(localAudioTrack)

        if (video) {
            videoCapturer = createCapturer()
            val videoSource = factory.createVideoSource(false)
            surfaceTextureHelper = SurfaceTextureHelper.create("Capture_${callId}", RtcClient.eglBase.eglBaseContext)
            videoCapturer?.initialize(surfaceTextureHelper, ctx, videoSource.capturerObserver)
            videoCapturer?.startCapture(640, 480, 30)
            localVideoTrack = factory.createVideoTrack("video0", videoSource)
            localVideoTrack?.addSink(localView)
            pc?.addTrack(localVideoTrack)
        }

        if (isCaller) createOffer() else listenForOfferThenAnswer()
        listenRemoteCandidates()
    }

    private fun createCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(ctx)
        val front = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames.firstOrNull()
        return front?.let { enumerator.createCapturer(it, object : CameraVideoCapturer.CameraEventsHandler {
            override fun onCameraError(p0: String?) {}
            override fun onCameraDisconnected() {}
            override fun onCameraFreezed(p0: String?) {}
            override fun onCameraOpening(p0: String?) {}
            override fun onFirstFrameAvailable() {}
            override fun onCameraClosed() {}
        }) }
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        pc?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        db.child("offer").setValue(mapOf("sdp" to desc.description, "type" to desc.type.canonicalForm()))
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
        listenAnswer()
    }

    private fun listenAnswer() {
        val ref = db.child("answer")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val sdp = s.child("sdp").getValue(String::class.java) ?: return
                val type = s.child("type").getValue(String::class.java) ?: return
                pc?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp))
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(l); listeners += ref to l
    }

    private fun listenForOfferThenAnswer() {
        val ref = db.child("offer")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val sdp = s.child("sdp").getValue(String::class.java) ?: return
                val type = s.child("type").getValue(String::class.java) ?: return
                pc?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        pc?.createAnswer(object : SdpObserver {
                            override fun onCreateSuccess(desc: SessionDescription) {
                                pc?.setLocalDescription(object : SdpObserver {
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onSetSuccess() {
                                        db.child("answer").setValue(mapOf("sdp" to desc.description, "type" to desc.type.canonicalForm()))
                                    }
                                    override fun onCreateFailure(p0: String?) {}
                                    override fun onSetFailure(p0: String?) {}
                                }, desc)
                            }
                            override fun onSetSuccess() {}
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        }, MediaConstraints())
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp))
                ref.removeEventListener(this)
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(l); listeners += ref to l
    }

    private fun listenRemoteCandidates() {
        val path = if (isCaller) "calleeCandidates" else "callerCandidates"
        val ref = db.child(path)
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                for (c in s.children) {
                    val sdp = c.child("sdp").getValue(String::class.java) ?: continue
                    val mid = c.child("sdpMid").getValue(String::class.java)
                    val idx = c.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                    pc?.addIceCandidate(IceCandidate(mid, idx, sdp))
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(l); listeners += ref to l
    }

    fun setMuted(muted: Boolean) { localAudioTrack?.setEnabled(!muted); localAudioSourceMuted = muted }
    fun setVideoEnabled(enabled: Boolean) { localVideoTrack?.setEnabled(enabled) }
    fun switchCamera() { (videoCapturer as? CameraVideoCapturer)?.switchCamera(null) }

    fun end() {
        if (ended) return
        ended = true
        listeners.forEach { (ref, l) -> ref.removeEventListener(l) }
        try { videoCapturer?.stopCapture() } catch (_: Exception) {}
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        pc?.close()
        pc?.dispose()
    }
}

/** إنشاء/قبول/إنهاء المكالمة عبر Realtime Database (calls/{id}, userCalls/{uid}/incoming) */
object CallSignal {
    private val db get() = FirebaseDatabase.getInstance().reference

    fun startCall(peerUid: String, video: Boolean): String? {
        val me = Cloud.uid ?: return null
        val id = db.child("calls").push().key ?: return null
        db.child("calls").child(id).setValue(
            mapOf("callerId" to me, "calleeId" to peerUid, "video" to video, "status" to "ringing", "createdAt" to System.currentTimeMillis()),
        )
        db.child("userCalls").child(peerUid).child("incoming").setValue(id)
        return id
    }

    fun acceptCall(callId: String) { db.child("calls").child(callId).child("status").setValue("accepted") }

    fun endCall(callId: String, byUid: String) {
        db.child("calls").child(callId).child("status").setValue("ended")
        db.child("userCalls").child(byUid).child("incoming").removeValue()
    }

    fun clearIncoming(uid: String) = db.child("userCalls").child(uid).child("incoming").removeValue()

    data class IncomingCall(val id: String, val callerId: String, val video: Boolean)

    fun listenIncoming(onIncoming: (IncomingCall?) -> Unit): com.google.firebase.database.ValueEventListener? {
        val me = Cloud.uid ?: return null
        val ref = db.child("userCalls").child(me).child("incoming")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val id = s.getValue(String::class.java)
                if (id == null) { onIncoming(null); return }
                db.child("calls").child(id).get().addOnSuccessListener { call ->
                    val callerId = call.child("callerId").getValue(String::class.java)
                    val v = call.child("video").getValue(Boolean::class.java) ?: false
                    if (callerId != null) onIncoming(IncomingCall(id, callerId, v)) else onIncoming(null)
                }
            }
            override fun onCancelled(e: DatabaseError) { onIncoming(null) }
        }
        ref.addValueEventListener(l)
        return l
    }

    fun stopListeningIncoming(uid: String, l: com.google.firebase.database.ValueEventListener) {
        db.child("userCalls").child(uid).child("incoming").removeEventListener(l)
    }

    fun listenStatus(callId: String, onStatus: (String) -> Unit): com.google.firebase.database.ValueEventListener {
        val ref = db.child("calls").child(callId).child("status")
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { s.getValue(String::class.java)?.let(onStatus) }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(l)
        return l
    }

    fun stopListeningStatus(callId: String, l: com.google.firebase.database.ValueEventListener) {
        db.child("calls").child(callId).child("status").removeEventListener(l)
    }
}
