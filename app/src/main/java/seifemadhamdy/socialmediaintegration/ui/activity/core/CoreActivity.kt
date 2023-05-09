package seifemadhamdy.socialmediaintegration.ui.activity.core

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import seifemadhamdy.socialmediaintegration.R
import seifemadhamdy.socialmediaintegration.databinding.ActivityCoreBinding
import seifemadhamdy.socialmediaintegration.ui.activity.profile.ProfileActivity
import seifemadhamdy.socialmediaintegration.utils.blur.BlurUtils


class CoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCoreBinding
    private val blurUtils by lazy { BlurUtils() }
    private var areViewsUpdatedForInsets = false
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { firebaseAuthUIAuthenticationResult ->
        onSignInResult(firebaseAuthUIAuthenticationResult)
    }

    private val tag by lazy { javaClass.simpleName }
    private val callbackManager by lazy { CallbackManager.Factory.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityCoreBinding.inflate(layoutInflater)
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

        if (FirebaseAuth.getInstance().currentUser != null) {
            // Navigate authenticated user to RemindersActivity
            navigateToProfileActivity()
        } else {
            binding.googleMaterialButton.setOnClickListener {
                // Launch login UI using Firebase AuthUI
                signInLauncher.launch(
                    AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                        // List of login providers
                        arrayListOf(
                            // Google login provider
                            AuthUI.IdpConfig.GoogleBuilder().build()
                        )
                    )
                        // Set a custom layout for the AuthMethodPickerActivity screen
                        .setAuthMethodPickerLayout(
                            AuthMethodPickerLayout.Builder(R.layout.activity_core)
                                // Set the ID of the Google sign in button in the custom layout
                                .setGoogleButtonId(R.id.google_material_button).build()
                        ).build()
                )
            }

            val accessToken = AccessToken.getCurrentAccessToken()

            if (accessToken != null && !accessToken.isExpired) {
                navigateToProfileActivity()
            }

            LoginManager.getInstance()
                .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                    override fun onCancel() {
                        Log.i(tag, "onCancel: User has cancelled logging in")
                    }

                    override fun onError(error: FacebookException) {
                        Log.i(tag, "onError: ${error.message}")
                    }

                    override fun onSuccess(result: LoginResult) {
                        navigateToProfileActivity()
                    }
                })

            binding.facebookMaterialButton.setOnClickListener {
                LoginManager.getInstance().logInWithReadPermissions(
                    this, callbackManager, listOf(
                        "public_profile, email"
                    )
                )
            }
        }
    }

    private fun View.updateLayoutParamsHeight(heightSize: Int) {
        updateLayoutParams<ViewGroup.LayoutParams> {
            height = heightSize
        }
    }

    private fun navigateToProfileActivity() {
        Intent(this, ProfileActivity::class.java).apply {
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

    private fun onSignInResult(firebaseAuthUIAuthenticationResult: FirebaseAuthUIAuthenticationResult) {
        if (firebaseAuthUIAuthenticationResult.resultCode == RESULT_OK) {
            // Successfully signed in

            // Navigate the user to RemindersActivity
            navigateToProfileActivity()

            // Log that the user's authentication is successful
            Log.i(
                tag,
                "onSignInResult: User ${FirebaseAuth.getInstance().currentUser?.displayName} has successfully signed in."
            )
        } else {
            // Sign in failed.

            // Log the user's failed authentication error code
            Log.i(
                tag,
                "onSignInResult: ${firebaseAuthUIAuthenticationResult.idpResponse?.error?.errorCode}"
            )
        }
    }
}