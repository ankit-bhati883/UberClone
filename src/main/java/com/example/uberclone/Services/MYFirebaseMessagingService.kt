package com.example.uberclone.Services

import android.util.Log
import com.example.uberclone.Utils.UserUtils
import com.example.uberclone.common
import com.example.uberclone.model.Model.EventBus.DriveRequestReceived
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

class MYFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            UserUtils.updatetoken(this, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        if (data != null) {
            if (data[common.NOTI_TITLE].equals(common.REQUEST_DRIVER_TITLE)) {
                val driveRequestReceived = DriveRequestReceived()
                driveRequestReceived.key = data[common.RIDER_KEY]
                driveRequestReceived.pickupLocation = data[common.PICKUP_LOCATION]
                driveRequestReceived.pickupLocationString = data[common.PICKUP_LOCATION_STRING]
                driveRequestReceived.destinationLocation = data[common.DESTINATION_LOCATION]
                driveRequestReceived.destinationLocationString =
                    data[common.DESTINATION_LOCATION_STRING]

                //New Info
                driveRequestReceived.distanceValue = data[common.RIDER_DISTANCE_VALUE]!!.toInt()
                driveRequestReceived.distanceText = data[common.RIDER_DISTANCE_TEXT]!!.toString()
                Log.d("MessagingService", "    " + (data[common.RIDER_DURATION_VALUE]!!.toInt()))
                driveRequestReceived.durationValue = data[common.RIDER_DURATION_VALUE]!!.toInt()
                driveRequestReceived.durationText = data[common.RIDER_DURATION_TEXT]!!.toString()
                driveRequestReceived.totalFee = data[common.RIDER_TOTAL_FEE]!!.toDouble()


                EventBus.getDefault()
                    .postSticky(driveRequestReceived)
            } else
                common.showNotification(
                    this, Random.nextInt(),
                    data[common.NOTI_TITLE],
                    data[common.NOTI_BODY], null
                )
        }
    }
}