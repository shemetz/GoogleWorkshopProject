package org.team2.ridetogather

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import java.util.*

class FacebookLoginActivity : AppCompatActivity() {
    private val tag = FacebookLoginActivity::class.java.simpleName
    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_login)


        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_events", "user_friends"))

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.i(tag, "Facebook login successful!")

                    val request = GraphRequest.newMeRequest(loginResult.accessToken) { `object`, response ->
                        try {
                            Log.i(tag, "Facebook API is working!")
                        } catch (e: Exception) {
                            e.printStackTrace()

                        }
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "name,email,id,picture.type(large)")
                    request.parameters = parameters
                    request.executeAsync()


                    goToMainActivity()
                }

                override fun onCancel() {
                    finish()
                }

                override fun onError(exception: FacebookException) {

                    finish()
                }
            })
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        Log.i(tag, "is logged in: $isLoggedIn")
        if (isLoggedIn) {
            goToMainActivity()
        }
    }

    fun goToMainActivity() {
        // Go to MainActivity and start it
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}
