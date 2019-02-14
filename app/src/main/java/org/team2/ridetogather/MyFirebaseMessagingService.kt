package org.team2.ridetogather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject


class MyFirebaseMessagingService : FirebaseMessagingService() {
    val TAG = "Firebase Messaging"
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        Database.getUser(Database.idOfCurrentUser){user: User ->
            user.firebaseId = token!!
            Database.updateUser(user)
        }
    }
    fun createChannel(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val CHANNEL_ID = "ridetogather"
            val name = "ridetogather_channel"
            val Description = "RideToGather Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = Description
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage?.from}")

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            createChannel()
            val intent : Intent
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val click_action = remoteMessage.data["click_action"]
            val picUrl = remoteMessage.data["pic_url"]
            if(click_action != null){
                intent = Intent(click_action)
                val keysMap = JSONObject(remoteMessage.data["keys"])
                for(key in keysMap.keys()){
                    if(keysMap.optInt(key,-1) != -1){
                        intent.putExtra(key, keysMap.optInt(key))
                    }
                    else{
                        intent.putExtra(key, keysMap.optBoolean(key))
                    }
                    Log.d("FirebaseCheck", "key: " + key + "val: " + keysMap.get(key))
                }
            }
            else{
                intent = Intent(this, MainActivity::class.java)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val mainIntent = Intent(applicationContext, MainActivity::class.java)
            val eventIntent = Intent(applicationContext, EventRidesActivity::class.java)

            val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
                addNextIntent(mainIntent)
                if(click_action.equals("com.google.firebase.RIDE_PAGE")){
                    eventIntent.putExtra(Keys.EVENT_ID.name,intent.getIntExtra(Keys.EVENT_ID.name,-1))
                    addNextIntent(eventIntent)
                }
                addNextIntent(intent)
                // Get the PendingIntent containing the entire back stack
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }



            //val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            //    PendingIntent.FLAG_ONE_SHOT)

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this,"ridetogather")
                .setSmallIcon(R.drawable.ic_notification_car)
                .setContentTitle(remoteMessage.data["title"])
                .setContentText(remoteMessage.data["body"])
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(resultPendingIntent)
                .setStyle(NotificationCompat.BigTextStyle()
                    .setBigContentTitle(remoteMessage.data["title"])
                    .bigText(remoteMessage.data["body"]))
            if(picUrl != null){
                notificationBuilder.setLargeIcon(getBitmapFromURL(picUrl))
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, notificationBuilder.build())
        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
}