package org.team2.ridetogather

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import java.util.*

class FacebookLoginActivity : AppCompatActivity() {
    private val tag = FacebookLoginActivity::class.java.simpleName
    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_login)
        Database.initialize(this)

        val prefManager = PrefManager(this@FacebookLoginActivity)
        val loginButton = findViewById<LoginButton>(R.id.login_button)
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_events", "user_friends"))

        loginButton.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.i(tag, "Facebook login successful!")

                    getUserIdAndGoToMainActivity(loginResult.accessToken)
                }

                override fun onCancel() {
                    finish()
                }

                override fun onError(exception: FacebookException) {
                    Toast.makeText(
                        applicationContext,
                        exception.toString(), Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            })


        /*openLoginScreen()
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
                    Toast.makeText(applicationContext,
                        exception.toString(), Toast.LENGTH_SHORT).show()
                    finish()
                }
            })*/
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        Log.i(tag, "is logged in: $isLoggedIn")
        if (isLoggedIn) {
            if (prefManager.thisUserId != -1) {
                goToMainActivity()
            } else {
                getUserIdAndGoToMainActivity(accessToken)
            }
        }
    }

    /*fun openLoginScreen() {
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_events", "user_friends"))
    }*/

    fun goToMainActivity() {
        // Go to MainActivity and start it
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    fun getUserIdAndGoToMainActivity(accessToken: AccessToken) {
        val prefManager = PrefManager(this)
        val request = GraphRequest.newMeRequest(accessToken) { _, response ->
            Log.i(tag, "Facebook API is working: ${response.jsonObject}")
            val name = response.jsonObject.getString("name") ?: ""
            val facebookProfileId = response.jsonObject.getString("id")
            Database.getOrAddUserByFacebook(name, facebookProfileId) {user ->
                Log.i(tag, "Updating stored user ID… (${prefManager.thisUserId} → ${user.id})")
                prefManager.thisUserId = user.id
                Database.idOfCurrentUser = user.id
                goToMainActivity()
            }
            val profilePicUrl = response.jsonObject.getJSONObject("picture")
                .getJSONObject("data").getString("url")
        }
        val parameters = Bundle()
        parameters.putString("fields", "name,email,id,picture.type(large)")
        request.parameters = parameters
        request.executeAsync()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.i(tag, "back button pressed")
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        Log.i(tag, "on back pressed 1")
        moveTaskToBack(true)
    }
}
