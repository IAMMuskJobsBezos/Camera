package org.fossify.camera.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.view.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayout
import org.fossify.camera.BuildConfig
import org.fossify.camera.R
import org.fossify.camera.databinding.ActivityMainBinding
import org.fossify.camera.extensions.config
import org.fossify.camera.extensions.fadeIn
import org.fossify.camera.extensions.fadeOut
import org.fossify.camera.extensions.setShadowIcon
import org.fossify.camera.helpers.*
import org.fossify.camera.implementations.CameraXInitializer
import org.fossify.camera.implementations.CameraXPreviewListener
import org.fossify.camera.interfaces.MyPreview
import org.fossify.camera.models.ResolutionOption
import org.fossify.camera.models.TimerMode
import org.fossify.camera.views.FocusCircleView
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import java.util.concurrent.TimeUnit

class MainActivity : SimpleActivity(), PhotoProcessor.MediaSavedListener, CameraXPreviewListener {
    private companion object {
        private const val ANIMATION_DURATION = 500L
        private const val PHOTO_MODE_INDEX = 0
        private const val VIDEO_MODE_INDEX = 1
        private const val TIMER_2_SECONDS = 2001
        private const val SWITCH_CAMERA_ROTATION_ANGLE = 180f
        private const val DISABLED_BUTTON_ALPHA = 0.4f
    }

    private val binding by viewBinding(ActivityMainBinding::inflate)

    private lateinit var mOrientationEventListener: OrientationEventListener
    private lateinit var mFocusCircleView: FocusCircleView
    private lateinit var mediaSoundHelper: MediaSoundHelper
    private var mPreview: MyPreview? = null
    private var mPreviewUri: Uri? = null
    private var mIsHardwareShutterHandled = false
    private var mLastHandledOrientation = 0
    private var countDownTimer: CountDownTimer? = null
    private var mOriginalBrightness: Float? = null

    private val tabSelectedListener = object : TabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            handlePermission(PERMISSION_RECORD_AUDIO) {
                if (it) {
                    when (tab.position) {
                        VIDEO_MODE_INDEX -> mPreview?.initVideoMode()
                        PHOTO_MODE_INDEX -> mPreview?.initPhotoMode()
                        else -> throw IllegalStateException("Unsupported tab position ${tab.position}")
                    }
                } else {
                    toast(org.fossify.commons.R.string.no_audio_permissions)
                    selectPhotoTab()
                    if (isVideoCaptureIntent()) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        appLaunched(BuildConfig.APPLICATION_ID)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        initVariables()
        tryInitCamera()
        supportActionBar?.hide()
        setupOrientationEventListener()

        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())

        if (isOreoMr1Plus()) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasStorageAndCameraPermissions()) {
            val isInPhotoMode = isInPhotoMode()
            setupPreviewImage(isInPhotoMode)
            if (::mFocusCircleView.isInitialized) {
                mFocusCircleView.setStrokeColor(getProperPrimaryColor())
            }
            toggleActionButtons(enabled = true)
            mOrientationEventListener.enable()
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mOriginalBrightness = window.updateBrightness(config.maxBrightness, mOriginalBrightness)
        ensureTransparentNavigationBar()
        if (ViewCompat.getWindowInsetsController(window.decorView) == null) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!isAskingPermissions) {
            cancelTimer()
        }

        if (!hasStorageAndCameraPermissions() || isAskingPermissions) {
            return
        }

        mOrientationEventListener.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPreview = null
        mediaSoundHelper.release()
    }

    private fun selectPhotoTab(triggerListener: Boolean = false) {
        if (!triggerListener) {
            removeTabListener()
        }

        binding.cameraModeTab.getTabAt(PHOTO_MODE_INDEX)?.select()
        setTabListener()
    }

    private fun selectVideoTab(triggerListener: Boolean = false) {
        if (!triggerListener) {
            removeTabListener()
        }
        binding.cameraModeTab.getTabAt(VIDEO_MODE_INDEX)?.select()
        setTabListener()
    }

    private fun setTabListener() {
        binding.cameraModeTab.addOnTabSelectedListener(tabSelectedListener)
    }

    private fun removeTabListener() {
        binding.cameraModeTab.removeOnTabSelectedListener(tabSelectedListener)
    }

    private fun ensureTransparentNavigationBar() {
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)
    }

    private fun initVariables() {
        mIsHardwareShutterHandled = false
        mediaSoundHelper = MediaSoundHelper(this)
        mediaSoundHelper.loadSounds()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_CAMERA && !mIsHardwareShutterHandled) {
            mIsHardwareShutterHandled = true
            shutterPressed()
            true
        } else if (!mIsHardwareShutterHandled && config.volumeButtonsAsShutter && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            mIsHardwareShutterHandled = true
            shutterPressed()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mIsHardwareShutterHandled = false
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun hideIntentButtons() = binding.apply {
        cameraModeHolder.beGone()
        lastPhotoVideoPreview.beInvisible()
    }

    private fun tryInitCamera() {
        handlePermission(PERMISSION_CAMERA) { grantedCameraPermission ->
            if (grantedCameraPermission) {
                handleStoragePermission {
                    val isInPhotoMode = isInPhotoMode()
                    if (isInPhotoMode) {
                        initializeCamera(true)
                    } else {
                        handlePermission(PERMISSION_RECORD_AUDIO) { grantedRecordAudioPermission ->
                            if (grantedRecordAudioPermission) {
                                initializeCamera(false)
                            } else {
                                toast(org.fossify.commons.R.string.no_audio_permissions)
                                if (isThirdPartyIntent()) {
                                    finish()
                                } else {
                                    // re-initialize in photo mode
                                    config.initPhotoMode = true
                                    tryInitCamera()
                                }
                            }
                        }
                    }
                }
            } else {
                toast(org.fossify.commons.R.string.no_camera_permissions)
                finish()
            }
        }
    }

    private fun isInPhotoMode(): Boolean {
        return mPreview?.isInPhotoMode() ?: if (isVideoCaptureIntent()) {
            false
        } else if (isImageCaptureIntent()) {
            true
        } else {
            config.initPhotoMode
        }
    }

    private fun handleStoragePermission(callback: (granted: Boolean) -> Unit) {
        if (isTiramisuPlus()) {
            val mediaPermissionIds =
                mutableListOf(PERMISSION_READ_MEDIA_IMAGES, PERMISSION_READ_MEDIA_VIDEO)
            if (isUpsideDownCakePlus()) {
                mediaPermissionIds.add(PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED)
            }

            handlePartialMediaPermissions(permissionIds = mediaPermissionIds, callback = callback)
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE, callback)
        }
    }

    private fun isThirdPartyIntent() = isVideoCaptureIntent() || isImageCaptureIntent()

    private fun isImageCaptureIntent(): Boolean =
        intent?.action == MediaStore.ACTION_IMAGE_CAPTURE || intent?.action == MediaStore.ACTION_IMAGE_CAPTURE_SECURE

    private fun isVideoCaptureIntent(): Boolean = intent?.action == MediaStore.ACTION_VIDEO_CAPTURE

    private fun initializeCamera(isInPhotoMode: Boolean) {
        setContentView(binding.root)
        initButtons()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.viewHolder) { _, windowInsets ->
            val safeInsetBottom = windowInsets.displayCutout?.safeInsetBottom ?: 0
            val safeInsetTop = windowInsets.displayCutout?.safeInsetTop ?: 0

            binding.topOptions.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = safeInsetTop
            }

            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // sit close to the bottom edge - just clearing the system bar - so the row has
            // room above it to use bigger touch targets without crowding the tabs above
            val marginBottom = systemBarsInsets.bottom +
                    resources.getDimensionPixelSize(org.fossify.commons.R.dimen.small_margin)

            binding.shutter.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = marginBottom
            }

            WindowInsetsCompat.CONSUMED
        }

        if (isInPhotoMode) {
            selectPhotoTab()
        } else {
            selectVideoTab()
        }

        val outputUri = intent.extras?.get(MediaStore.EXTRA_OUTPUT) as? Uri
        val isThirdPartyIntent = isThirdPartyIntent()
        mPreview = CameraXInitializer(this).createCameraXPreview(
            binding.previewView,
            listener = this,
            mediaSoundHelper = mediaSoundHelper,
            outputUri = outputUri,
            isThirdPartyIntent = isThirdPartyIntent,
            initInPhotoMode = isInPhotoMode,
        )

        mFocusCircleView = FocusCircleView(this).apply {
            id = View.generateViewId()
        }
        binding.viewHolder.addView(mFocusCircleView)

        setupPreviewImage(true)

        if (isThirdPartyIntent) {
            hideIntentButtons()
        }
    }

    private fun initButtons() = binding.apply {
        timerText.setFactory { layoutInflater.inflate(R.layout.timer_text, null) }
        flipButton.setOnClickListener {
            animateCameraToggle()
            mPreview!!.toggleFrontBackCamera()
        }

        lastPhotoVideoPreview.setOnClickListener { showLastMediaPreview() }

        layoutTop.apply {
            toggleFlash.setOnClickListener { mPreview!!.handleFlashlightClick() }
            toggleTimer.setOnClickListener {
                val nextMode = config.timerMode.next()
                config.timerMode = nextMode
                setTimerModeIcon(nextMode)
            }
        }

        shutter.setOnClickListener { shutterPressed() }

        setTimerModeIcon(config.timerMode)
    }

    private fun animateCameraToggle() {
        ObjectAnimator.ofFloat(binding.toggleCamera, "rotation", 0f, SWITCH_CAMERA_ROTATION_ANGLE)
            .apply {
                duration = ANIMATION_DURATION
                interpolator = FastOutSlowInInterpolator()
                start()
            }
    }

    private fun setTimerModeIcon(timerMode: TimerMode) = binding.layoutTop.toggleTimer.apply {
        setShadowIcon(timerMode.getTimerModeDrawableRes())
        text = getString(timerMode.getTimerModeLabelRes())
    }

    private fun showLastMediaPreview() {
        if (mPreviewUri != null) {
            val path =
                applicationContext.getRealPathFromURI(mPreviewUri!!) ?: mPreviewUri!!.toString()

            // force Fossify Gallery specifically instead of letting the OS pick/chooser a
            // different default photo viewer; fall back to the generic behavior if it's missing
            if (isPackageInstalled("org.fossify.gallery")) {
                ensureBackgroundThread {
                    val finalUri = getFinalUriFromPath(path, BuildConfig.APPLICATION_ID) ?: return@ensureBackgroundThread
                    val mimeType = getUriMimeType(path, finalUri)
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(finalUri, mimeType)
                        setPackage("org.fossify.gallery")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(IS_FROM_GALLERY, true)
                        putExtra(REAL_FILE_PATH, path)
                        launchActivityIntent(this)
                    }
                }
            } else {
                openPathIntent(path, false, BuildConfig.APPLICATION_ID)
            }
        }
    }

    private fun shutterPressed() {
        if (countDownTimer != null) {
            cancelTimer()
        } else if (isInPhotoMode()) {
            val timerMode = config.timerMode
            if (timerMode == TimerMode.OFF) {
                mPreview?.tryTakePicture()
            } else {
                scheduleTimer(timerMode)
            }
        } else {
            mPreview?.toggleRecording()
        }
    }

    private fun cancelTimer() {
        mediaSoundHelper.stopTimerCountdown2SecondsSound()
        countDownTimer?.cancel()
        countDownTimer = null
        resetViewsOnTimerFinish()
    }

    override fun onInitPhotoMode() {
        binding.apply {
            shutter.setImageResource(R.drawable.ic_shutter_animated)
            setButtonGreyedOut(layoutTop.toggleTimer, greyedOut = false)
        }
        setupPreviewImage(true)
        selectPhotoTab()
    }

    override fun onInitVideoMode() {
        binding.apply {
            shutter.setImageResource(R.drawable.ic_video_rec_animated)
            // the timer only applies to photo capture; keep it in place, just inert
            setButtonGreyedOut(layoutTop.toggleTimer, greyedOut = true)
        }
        setupPreviewImage(false)
        selectVideoTab()
    }

    private fun setButtonGreyedOut(button: View, greyedOut: Boolean) {
        button.isEnabled = !greyedOut
        button.alpha = if (greyedOut) DISABLED_BUTTON_ALPHA else 1f
    }

    private fun setupPreviewImage(isPhoto: Boolean) {
        val uri =
            if (isPhoto) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val lastMediaId = getLatestMediaId(uri)
        if (lastMediaId == 0L) {
            return
        }

        loadLastTakenMedia(Uri.withAppendedPath(uri, lastMediaId.toString()))
    }

    private fun loadLastTakenMedia(uri: Uri?) {
        mPreviewUri = uri
        runOnUiThread {
            if (!isDestroyed) {
                val options = RequestOptions()
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.ic_photos_vector)
                    .error(R.drawable.ic_photos_vector)

                Glide.with(this)
                    .load(uri)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.photosIcon)
            }
        }
    }

    private fun hasStorageAndCameraPermissions(): Boolean {
        return if (isInPhotoMode()) hasPhotoModePermissions() else hasVideoModePermissions()
    }

    private fun hasPhotoModePermissions(): Boolean {
        return if (isTiramisuPlus()) {
            var hasMediaPermission = hasPermission(PERMISSION_READ_MEDIA_IMAGES) || hasPermission(
                PERMISSION_READ_MEDIA_VIDEO
            )
            if (isUpsideDownCakePlus()) {
                hasMediaPermission =
                    hasMediaPermission || hasPermission(PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED)
            }
            hasMediaPermission && hasPermission(PERMISSION_CAMERA)
        } else {
            hasPermission(PERMISSION_WRITE_STORAGE) && hasPermission(PERMISSION_CAMERA)
        }
    }

    private fun hasVideoModePermissions(): Boolean {
        return if (isTiramisuPlus()) {
            var hasMediaPermission = hasPermission(PERMISSION_READ_MEDIA_VIDEO)
            if (isUpsideDownCakePlus()) {
                hasMediaPermission =
                    hasMediaPermission || hasPermission(PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED)
            }
            hasMediaPermission && hasPermission(PERMISSION_CAMERA) && hasPermission(
                PERMISSION_RECORD_AUDIO
            )
        } else {
            hasPermission(PERMISSION_WRITE_STORAGE) && hasPermission(PERMISSION_CAMERA) && hasPermission(
                PERMISSION_RECORD_AUDIO
            )
        }
    }

    private fun setupOrientationEventListener() {
        mOrientationEventListener = object : OrientationEventListener(
            this, SensorManager.SENSOR_DELAY_NORMAL
        ) {
            override fun onOrientationChanged(orientation: Int) {
                if (isDestroyed) {
                    mOrientationEventListener.disable()
                    return
                }

                val currOrient = when (orientation) {
                    in 75..134 -> ORIENT_LANDSCAPE_RIGHT
                    in 225..289 -> ORIENT_LANDSCAPE_LEFT
                    else -> ORIENT_PORTRAIT
                }

                if (currOrient != mLastHandledOrientation) {
                    val degrees = when (currOrient) {
                        ORIENT_LANDSCAPE_LEFT -> 90
                        ORIENT_LANDSCAPE_RIGHT -> -90
                        else -> 0
                    }

                    animateViews(degrees)
                    mLastHandledOrientation = currOrient
                }
            }
        }
    }

    private fun animateViews(degrees: Int) = binding.apply {
        val views = arrayOf(
            flipButton,
            layoutTop.toggleTimer,
            layoutTop.toggleFlash,
            shutter,
            lastPhotoVideoPreview,
        )
        for (view in views) {
            rotate(view, degrees)
        }
    }

    private fun rotate(view: View, degrees: Int) =
        view.animate().rotation(degrees.toFloat()).start()

    override fun setHasFrontAndBackCamera(hasFrontAndBack: Boolean) {
        setButtonGreyedOut(binding.flipButton, greyedOut = !hasFrontAndBack)
    }

    override fun setFlashAvailable(available: Boolean) {
        setButtonGreyedOut(binding.layoutTop.toggleFlash, greyedOut = !available)
        if (!available) {
            mPreview?.setFlashlightState(FLASH_OFF)
        }
    }

    override fun onPhotoCaptureStart() {
        toggleActionButtons(enabled = false)
    }

    override fun onPhotoCaptureEnd() {
        toggleActionButtons(enabled = true)
    }

    private fun toggleActionButtons(enabled: Boolean) = binding.apply {
        runOnUiThread {
            shutter.isClickable = enabled
            previewView.isEnabled = enabled
            flipButton.isClickable = enabled
            layoutTop.toggleFlash.isClickable = enabled
        }
    }

    override fun shutterAnimation() {
        binding.shutterAnimation.alpha = 1.0f
        binding.shutterAnimation.animate().alpha(0f).setDuration(ANIMATION_DURATION).start()
    }

    override fun onMediaSaved(uri: Uri) {
        loadLastTakenMedia(uri)
        if (isImageCaptureIntent()) {
            Intent().apply {
                data = uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                setResult(RESULT_OK, this)
            }
            finish()
        } else if (isVideoCaptureIntent()) {
            Intent().apply {
                data = uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                setResult(RESULT_OK, this)
            }
            finish()
        }
    }

    override fun onImageCaptured(bitmap: Bitmap) {
        if (isImageCaptureIntent()) {
            Intent().apply {
                putExtra("data", bitmap)
                setResult(RESULT_OK, this)
            }
            finish()
        }
    }

    override fun onChangeFlashMode(flashMode: Int) {
        binding.layoutTop.toggleFlash.apply {
            val isOn = flashMode == FLASH_ON
            setShadowIcon(if (isOn) R.drawable.ic_flash_on_vector else R.drawable.ic_flash_off_vector)
            text = getString(if (isOn) R.string.flash_on else R.string.flash_off)
        }
    }

    override fun onVideoRecordingStarted() {
        binding.apply {
            cameraModeHolder.beInvisible()
            videoRecCurrTimer.beVisible()

            flipButton.fadeOut()
            lastPhotoVideoPreview.fadeOut()

            shutter.post {
                if (!isDestroyed) {
                    shutter.isSelected = true
                }
            }
        }
    }

    override fun onVideoRecordingStopped() {
        binding.apply {
            cameraModeHolder.beVisible()

            flipButton.fadeIn()
            lastPhotoVideoPreview.fadeIn()

            videoRecCurrTimer.text = 0.getFormattedDuration()
            videoRecCurrTimer.beGone()

            shutter.isSelected = false
        }
    }

    override fun onVideoDurationChanged(durationNanos: Long) {
        val seconds = TimeUnit.NANOSECONDS.toSeconds(durationNanos).toInt()
        binding.videoRecCurrTimer.text = seconds.getFormattedDuration()
    }

    override fun onFocusCamera(xPos: Float, yPos: Float) {
        mFocusCircleView.drawFocusCircle(xPos, yPos)
    }

    override fun onTouchPreview() {
        // no-op: there is no options panel to dismiss on this screen anymore
    }

    override fun displaySelectedResolution(resolutionOption: ResolutionOption) {
        // resolution is locked to the app default; nothing on screen shows it
    }

    override fun adjustPreviewView(requiresCentering: Boolean) {
        binding.apply {
            val constraintSet = ConstraintSet()
            constraintSet.clone(viewHolder)
            if (requiresCentering) {
                constraintSet.connect(
                    previewView.id,
                    ConstraintSet.TOP,
                    topOptions.id,
                    ConstraintSet.BOTTOM
                )
                constraintSet.connect(
                    previewView.id,
                    ConstraintSet.BOTTOM,
                    cameraModeHolder.id,
                    ConstraintSet.TOP
                )
            } else {
                constraintSet.connect(
                    previewView.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                constraintSet.connect(
                    previewView.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
            }
            constraintSet.applyTo(viewHolder)
        }
    }

    override fun mediaSaved(path: String) {
        rescanPaths(arrayListOf(path)) {
            setupPreviewImage(true)
            Intent(BROADCAST_REFRESH_MEDIA).apply {
                putExtra(REFRESH_PATH, path)
                `package` = "org.fossify.gallery"
                sendBroadcast(this)
            }
        }

        if (isImageCaptureIntent()) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun scheduleTimer(timerMode: TimerMode) {
        hideViewsOnTimerStart()
        binding.shutter.setImageState(intArrayOf(R.attr.state_timer_cancel), true)
        binding.timerText.beVisible()
        var playSound = true
        countDownTimer = object : CountDownTimer(timerMode.millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1).toString()
                binding.timerText.setText(seconds)
                if (playSound && config.isSoundEnabled) {
                    if (millisUntilFinished <= TIMER_2_SECONDS) {
                        mediaSoundHelper.playTimerCountdown2SecondsSound()
                        playSound = false
                    } else {
                        mediaSoundHelper.playTimerCountdownSound()
                    }
                }
            }

            override fun onFinish() {
                cancelTimer()
                mPreview!!.tryTakePicture()
            }
        }.start()
    }

    private fun hideViewsOnTimerStart() = binding.apply {
        arrayOf(topOptions, flipButton, lastPhotoVideoPreview, cameraModeHolder).forEach {
            it.fadeOut()
            it.beInvisible()
        }
    }

    private fun resetViewsOnTimerFinish() = binding.apply {
        arrayOf(topOptions, flipButton, lastPhotoVideoPreview, cameraModeHolder).forEach {
            it.fadeIn()
            it.beVisible()
        }

        timerText.beGone()
        shutter.setImageState(intArrayOf(-R.attr.state_timer_cancel), true)
    }
}
