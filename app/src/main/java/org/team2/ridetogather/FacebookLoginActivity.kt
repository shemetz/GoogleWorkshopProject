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

        val loginButton = findViewById<LoginButton>(R.id.login_button)
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_events", "user_friends"))

        loginButton.registerCallback(callbackManager,
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
            goToMainActivity()
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    /*fun buildDialog(){
        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        builder.setTitle("Sorry,")
        builder.setMessage("This app currently requires a Facebook account.")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(R.string.exit_login) { dialog, which ->
            finish()
        }
        builder.setNegativeButton(R.string.login_again) { dialog, which ->
            openLoginScreen()
        }
        builder.setOnCancelListener {
            finish()}
        builder.show()

    }*/

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
