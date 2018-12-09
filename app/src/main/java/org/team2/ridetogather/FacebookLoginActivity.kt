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

    var callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_login)


        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_events"))

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {

                    val request = GraphRequest.newMeRequest(loginResult.accessToken) { `object`, response ->
                        try {
                            //here is the data that you want
                            Log.d("FBLOGIN", `object`.toString())

                            if (`object`.has("id")) {
                                var email = response.getJSONObject().get("email").toString()
                                Log.d("FBLOGIN mail", email)
                            } else {
                                Log.e("FBLOGIN_FAILD", `object`.toString())
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()

                        }
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "name,email,id,picture.type(large)")
                    request.parameters = parameters
                    request.executeAsync()


                    Log.i("FBLOGIN", "hi there")
                    startActivity(Intent(applicationContext, MainActivity::class.java))
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
        Log.i("FBLOGIN", "is logged in: $isLoggedIn")

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}
