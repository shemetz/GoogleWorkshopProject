package org.team2.ridetogather

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_facebook_login.*
import java.util.*

class FacebookLoginActivity : AppCompatActivity() {
    private val tag = FacebookLoginActivity::class.java.simpleName
    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_login)

        val prefManager = PrefManager(this@FacebookLoginActivity)
        login_button.setReadPermissions(Arrays.asList("public_profile", "email", "user_events"))

        login_button.registerCallback(callbackManager,
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


        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        Log.i(tag, "is logged in: $isLoggedIn")
        if (isLoggedIn) {
            if (prefManager.thisUserId != -1) {
                goToMainActivity()
            } else {
                getUserIdAndGoToMainActivity(accessToken)
            }
        } else {
            // in case it wasn't deleted already
            prefManager.thisUserId = -1
            Database.idOfCurrentUser = -1
        }
    }

    private fun goToMainActivity() {
        // Go to MainActivity and start it
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun getUserIdAndGoToMainActivity(accessToken: AccessToken) {
        val prefManager = PrefManager(this)

        @Suppress("DEPRECATION")
        facebook_login_layout.setBackgroundColor(resources.getColor(android.R.color.white))
        progress_bar.visibility = View.VISIBLE
        val request = GraphRequest.newMeRequest(accessToken) { _, response ->
            Log.i(tag, "Facebook API is working: ${response.jsonObject}")
            val name = response.jsonObject.getString("name") ?: ""
            val facebookProfileId = response.jsonObject.getString("id")
            Database.getOrAddUserByFacebook(name, facebookProfileId) {user ->
                Log.i(tag, "Updating stored user ID… (${prefManager.thisUserId} → ${user.id})")
                FirebaseInstanceId.getInstance().instanceId
                    .addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("Firebase", "getInstanceId failed", task.exception)
                            return@OnCompleteListener
                        }
                        val token = task.result?.token
                        user.firebaseId = token!!
                        Database.updateUser(user)

                        // Log
                        Log.d("Firebase", token)
                    })
                prefManager.thisUserId = user.id
                Database.idOfCurrentUser = user.id
                progress_bar.visibility = View.GONE
                goToMainActivity()
            }
            val profilePicUrl = response.jsonObject.getJSONObject("picture")
                .getJSONObject("data").getString("url")

            Log.i(tag, profilePicUrl)
        }
        val parameters = Bundle()
        parameters.putString("fields", "name,email,id,picture.type(large)")
        request.parameters = parameters
        request.executeAsync()

        login_button.visibility = View.GONE
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
