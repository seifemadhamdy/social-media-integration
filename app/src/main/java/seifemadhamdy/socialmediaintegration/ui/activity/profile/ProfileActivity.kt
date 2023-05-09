package seifemadhamdy.socialmediaintegration.ui.activity.profile

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import seifemadhamdy.socialmediaintegration.R
import seifemadhamdy.socialmediaintegration.databinding.ActivityProfileBinding
import seifemadhamdy.socialmediaintegration.ui.activity.core.CoreActivity
import seifemadhamdy.socialmediaintegration.utils.blur.BlurUtils


class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val blurUtils by lazy { BlurUtils() }
    private var areViewsUpdatedForInsets = false
    private var lastKnownVideoViewPosition = 0
    private val isGoogleLogin by lazy { Firebase.auth.currentUser != null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            val navigationBarHeight =
                windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            binding.statusBarBlurView.updateLayoutParamsHeight(
                statusBarHeight
            )

            binding.navigationBarBlurView.updateLayoutParamsHeight(
                navigationBarHeight
            )

            if (!areViewsUpdatedForInsets) {
                binding.nestedScrollView.updatePaddingForInsets(top = statusBarHeight)
                binding.nestedScrollView.updatePaddingForInsets(bottom = navigationBarHeight)
                areViewsUpdatedForInsets = true
            }

            binding.root.also {
                blurUtils.apply {
                    initializeBlurView(blurView = binding.statusBarBlurView, viewGroup = it)

                    initializeBlurView(
                        blurView = binding.navigationBarBlurView, viewGroup = it
                    )
                }
            }

            WindowInsetsCompat.CONSUMED
        }

        prepareVideo()

        binding.closeMaterialButton.setOnClickListener {
            finishAffinity()
        }

        if (isGoogleLogin) {
            populateViews(name = Firebase.auth.currentUser?.displayName
                ?: getString(R.string.undefined),
                email = Firebase.auth.currentUser?.email ?: getString(R.string.undefined),
                profileUrl = Firebase.auth.currentUser?.photoUrl.run {
                    "${this.toString().substringBeforeLast("=")}=s200-c".apply {
                        Log.i(
                            "TAG",
                            "onCreate: $this"
                        )
                    }
                })
        } else {
            GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken()) { `object`, _ ->
                populateViews(
                    name = `object`?.getString("name") ?: getString(R.string.undefined),
                    email = `object`?.getString("email") ?: getString(R.string.undefined),
                    profileUrl = `object`?.getJSONObject("picture")?.getJSONObject("data")
                        ?.getString("url")
                )
            }.apply {
                parameters = Bundle().run {
                    putString("fields", "id, name, link, picture.type(large), email")
                    this
                }

                executeAsync()
            }
        }

        binding.signOutMaterialButton.setOnClickListener {
            if (isGoogleLogin) {
                logoutGoogle()
            } else {
                logoutFacebook()
            }
        }
    }

    private fun logoutFacebook() {
        LoginManager.getInstance().logOut()
        navigateToCoreActivity()
    }

    override fun onPause() {
        super.onPause()
        binding.videoView.pause()
        lastKnownVideoViewPosition = binding.videoView.currentPosition
    }

    override fun onResume() {
        super.onResume()

        if (lastKnownVideoViewPosition != 0) binding.videoView.seekTo(lastKnownVideoViewPosition)

        binding.videoView.start()
    }

    private fun View.updateLayoutParamsHeight(heightSize: Int) {
        updateLayoutParams<ViewGroup.LayoutParams> {
            height = heightSize
        }
    }

    private fun navigateToCoreActivity() {
        Intent(this, CoreActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
            finish()
        }
    }

    private fun View.updatePaddingForInsets(
        left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0
    ) {
        setPadding(
            paddingLeft + left, paddingTop + top, paddingRight + right, paddingBottom + bottom
        )
    }

    private fun prepareVideo() {
        binding.videoView.also { videoView ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
            } else {
                @Suppress("DEPRECATION") (getSystemService(AUDIO_SERVICE) as AudioManager).requestAudioFocus(
                    null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
                )
            }

            videoView.setOnPreparedListener {
                it.isLooping = true
            }

            videoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.cover))

            MediaController(this).apply {
                setAnchorView(videoView)
                setMediaPlayer(videoView)
                videoView.setMediaController(null)
            }
        }
    }

    private fun logoutGoogle() {
        // Logout current user
        AuthUI.getInstance().signOut(this).addOnSuccessListener {
            // Navigate to CoreActivity
            navigateToCoreActivity()
        }
    }

    private fun populateViews(name: String, email: String, profileUrl: String?) {
        binding.nameTextView.text = name
        binding.emailTextView.text = email

        profileUrl?.let {
            Glide.with(applicationContext).load(it).into(binding.profileShapeableImageView)
        }
    }
}