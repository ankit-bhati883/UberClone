package com.example.uberclone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.uberclone.model.DriverInfoData
import com.google.android.gms.maps.model.LatLng

object common {
    fun buildWelcomeMessange(): String {
        if (currentUser != null) {
            return StringBuilder("Welcome,")
                .append(" ")
                .append(currentUser!!.first_name)
                .append(" ")
                .append(currentUser!!.last_name)
                .toString()
        } else return StringBuilder("Welcome,").toString()
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        body: String?,
        intent: Intent?
    ) {
        var pendingIntent: PendingIntent? = null

        if (intent != null)
            pendingIntent = PendingIntent.getActivities(
                context,
                id,
                arrayOf(intent),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        val NOTIFICATION_CHANNEL_ID = "Ankit_uber_clone"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "Uber Clone",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Uber Clone"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_baseline_directions_car_24
                )
            )
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent!!)
        val notification = builder.build()
        notificationManager.notify(id, notification)
    }


    const val RIDER_TOTAL_FEE: String = "TotalFeeRider"
    const val RIDER_DURATION_VALUE: String = "DurationRiderValue"
    const val RIDER_DURATION_TEXT: String = "DurationRider"
    const val RIDER_DISTANCE_VALUE: String = "DistanceRiderValue"
    const val RIDER_DISTANCE_TEXT: String = "RiderDistance"

    const val RIDER_REQUEST_COMPLETE_TRIP: String = "RequestCompleteTripToRider"
    const val REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP: String = "DeclineAndRemoveTrip"
    const val TRIP_DESTINATION_LOCTION_REF: String = "TripDestinationLocation"
    const val WAIT_TIME_IN_MIN: Int = 1
    const val MIN_RANGE_PICKUP_IN_KM: Double = 0.05
    const val TRIP_PICKUP_KEY: String = "TripPickupLocation"
    const val TRIP_KEY: String = "TripKey"
    const val REQUEST_DRIVER_ACCEPT: String = "Accept"
    const val TRIP: String = "Trips"
    const val RIDER_INFO: String = "Riders"
    const val DESTINATION_LOCATION: String = "DestinationLocation"
    const val DESTINATION_LOCATION_STRING: String = "DestinationLocationString"
    const val PICKUP_LOCATION_STRING: String = "PickupLocationString"
    const val DRIVER_KEY: String = "DriverKey"
    const val REQUEST_DRIVER_DECLINE: String = "Decline"
    const val TOKEN_REFERENCE: String = "Token"
    const val NOTI_BODY = "body"
    const val NOTI_TITLE = "title"
    const val DRIVERS_LOCATION_REFERENCE: String = "DriversLocation"
    var currentUser: DriverInfoData? = null
    const val DRIVER_INFO_REFERENCE: String = "DriverInfo"


    const val RIDER_KEY: String = "RiderKey"
    const val PICKUP_LOCATION: String = "PickupLocation"
    const val REQUEST_DRIVER_TITLE: String = "RequestDriver"


    //DECODE POLY
    fun decodePoly(encoded: String): ArrayList<LatLng?>? {
        val poly = ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    fun createUniueTripNumber(timeOffset: Long?): String? {
        val rd = java.util.Random()
        var current = System.currentTimeMillis() + timeOffset!!
        var unique = current + rd.nextLong()
        if (unique < 0) unique *= -1
        return unique.toString()

    }
}