package com.example.uberclone.Utils

import android.content.Context
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.example.uberclone.Remote.IFCMService
import com.example.uberclone.Remote.RetroFitFCMClient
import com.example.uberclone.common
import com.example.uberclone.model.FCMSendData
import com.example.uberclone.model.Model.EventBus.NotifyRiderEvent
import com.example.uberclone.model.Tokenmodel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

object UserUtils {

    fun updateuser(
        view: View?,
        updateData: Map<String, Any>
    ) {
        Log.d("UserUtils", updateData.toString())
        FirebaseDatabase.getInstance().getReference(common.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Snackbar.make(view!!, e.message!!, Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {
                Snackbar.make(view!!, "update info successfully", Snackbar.LENGTH_LONG).show()
            }
    }

    fun updatetoken(
        context: Context,
        token: String
    ) {
        val tokenmodel = Tokenmodel()
        tokenmodel.token = token

        FirebaseDatabase.getInstance()
            .getReference(common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenmodel)
            .addOnFailureListener { e -> Log.e("UserUtils", "" + e.message) }
            .addOnSuccessListener { }
    }

    fun sendDeclineRequest(view: View, activity: FragmentActivity, key: String) {

        val compositeDisposable = CompositeDisposable()
        val ifcmService = RetroFitFCMClient.instance!!.create(IFCMService::class.java)

        FirebaseDatabase.getInstance()
            .getReference(common.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val tokenmodel = snapshot.getValue(Tokenmodel::class.java)
                        Log.d("UserUtils", "${tokenmodel!!.token}")
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[common.NOTI_TITLE] = common.REQUEST_DRIVER_DECLINE
                        notificationData[common.NOTI_BODY] =
                            "This message repersent for decline action from Driver"
                        notificationData[common.DRIVER_KEY] =
                            FirebaseAuth.getInstance().currentUser!!.uid

                        val fcmSendData = FCMSendData(tokenmodel!!.token, notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse!!.success == 0) {
                                    compositeDisposable.clear()

                                    Snackbar.make(view, "Decline Failed", Snackbar.LENGTH_LONG)
                                        .show()
                                } else {
                                    Snackbar.make(
                                        view,
                                        "You have decline the request",
                                        Snackbar.LENGTH_LONG
                                    ).show()

                                }
                            }, { t: Throwable? ->
                                compositeDisposable.clear()

                                Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                Log.d("UserUtils", "  ${t.message}")
                            })
                        )
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(view, "Token Not Found", Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }

            })


    }

    fun sendAcceptRequestToRider(
        view: View?,
        requireContext: Context,
        key: String,
        tripNumberId: String
    ) {

        val compositeDisposable = CompositeDisposable()
        val ifcmService = RetroFitFCMClient.instance!!.create(IFCMService::class.java)


        FirebaseDatabase.getInstance()
            .getReference(common.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val tokenmodel = snapshot.getValue(Tokenmodel::class.java)
                        Log.d("UserUtils", " ${tokenmodel!!.token}")
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(common.NOTI_TITLE, common.REQUEST_DRIVER_ACCEPT)
                        notificationData.put(
                            common.NOTI_BODY,
                            "This message repersent for accept action from Driver"
                        )
                        notificationData.put(
                            common.DRIVER_KEY,
                            FirebaseAuth.getInstance().currentUser!!.uid
                        )

                        notificationData.put(common.TRIP_KEY, tripNumberId)


                        val fcmSendData = FCMSendData(tokenmodel!!.token, notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse!!.success == 0) {
                                    compositeDisposable.clear()

                                    Snackbar.make(view!!, "Accept Failed", Snackbar.LENGTH_LONG)
                                        .show()
                                }

                            }, { t: Throwable? ->
                                compositeDisposable.clear()

                                Snackbar.make(view!!, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                Log.d("UserUtils", "  ${t.message}")
                            })
                        )
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(view!!, "Token Not Found", Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Snackbar.make(view!!, error.message, Snackbar.LENGTH_LONG).show()
                }

            })


    }

    fun sendNotifyToRider(context: Context, view: View, key: String?) {

        val compositeDisposable = CompositeDisposable()
        val ifcmService = RetroFitFCMClient.instance!!.create(IFCMService::class.java)


        FirebaseDatabase.getInstance()
            .getReference(common.TOKEN_REFERENCE)
            .child(key!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val tokenmodel = snapshot.getValue(Tokenmodel::class.java)
                        Log.d("UserUtils", " ${tokenmodel!!.token}")
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(common.NOTI_TITLE, "Driver Arrived")
                        notificationData.put(common.NOTI_BODY, "Your Driver Arrived")
                        notificationData.put(
                            common.DRIVER_KEY,
                            FirebaseAuth.getInstance().currentUser!!.uid
                        )

                        notificationData.put(common.RIDER_KEY, key!!)


                        val fcmSendData = FCMSendData(tokenmodel!!.token, notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse!!.success == 0) {
                                    compositeDisposable.clear()

                                    Snackbar.make(view!!, "Accept Failed", Snackbar.LENGTH_LONG)
                                        .show()
                                } else
                                    EventBus.getDefault().postSticky(NotifyRiderEvent())

                            }, { t: Throwable? ->
                                compositeDisposable.clear()

                                Snackbar.make(view!!, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                Log.d("UserUtils", "  ${t.message}")
                            })
                        )
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(view!!, "Token Not Found", Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Snackbar.make(view!!, error.message, Snackbar.LENGTH_LONG).show()
                }

            })

    }

    fun sendDeclineAndRemoveTripRequest(
        view: View,
        activity: FragmentActivity,
        key: String,
        tripNumberId: String?
    ) {
        val compositeDisposable = CompositeDisposable()
        val ifcmService = RetroFitFCMClient.instance!!.create(IFCMService::class.java)

        FirebaseDatabase.getInstance().getReference(common.TRIP)
            .child(tripNumberId!!)
            .removeValue()
            .addOnFailureListener { e ->
                Snackbar.make(view, e.message!!, Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {


                FirebaseDatabase.getInstance()
                    .getReference(common.TOKEN_REFERENCE)
                    .child(key)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {

                            if (snapshot.exists()) {

                                val tokenmodel = snapshot.getValue(Tokenmodel::class.java)
                                Log.d("UserUtils", " ${tokenmodel!!.token}")
                                val notificationData: MutableMap<String, String> = HashMap()
                                notificationData.put(
                                    common.NOTI_TITLE,
                                    common.REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP
                                )
                                notificationData.put(
                                    common.NOTI_BODY,
                                    "This message repersent for decline action from Driver"
                                )
                                notificationData.put(
                                    common.DRIVER_KEY,
                                    FirebaseAuth.getInstance().currentUser!!.uid
                                )

                                val fcmSendData = FCMSendData(tokenmodel!!.token, notificationData)

                                compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ fcmResponse ->
                                        if (fcmResponse!!.success == 0) {
                                            compositeDisposable.clear()

                                            Snackbar.make(
                                                view,
                                                "Decline Failed",
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Snackbar.make(
                                                view,
                                                "You have decline the request",
                                                Snackbar.LENGTH_LONG
                                            ).show()

                                        }
                                    }, { t: Throwable? ->
                                        compositeDisposable.clear()

                                        Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG)
                                            .show()
                                        Log.d("UserUtils", "  ${t.message}")
                                    })
                                )
                            } else {
                                compositeDisposable.clear()
                                Snackbar.make(view, "Token Not Found", Snackbar.LENGTH_LONG).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                            Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                        }

                    })


            }


    }

    fun sendCompleteTripToRider(view: View, context: Context, key: String?, tripNumberId: String?) {
        val compositeDisposable = CompositeDisposable()
        val ifcmService = RetroFitFCMClient.instance!!.create(IFCMService::class.java)




        FirebaseDatabase.getInstance()
            .getReference(common.TOKEN_REFERENCE)
            .child(key!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val tokenmodel = snapshot.getValue(Tokenmodel::class.java)
                        Log.d("UserUtils", " ${tokenmodel!!.token}")
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData.put(common.NOTI_TITLE, common.RIDER_REQUEST_COMPLETE_TRIP)
                        notificationData.put(
                            common.NOTI_BODY,
                            "This message repersent for request Complete Trip to Rider"
                        )
                        notificationData.put(common.TRIP_KEY, tripNumberId!!)

                        val fcmSendData = FCMSendData(tokenmodel!!.token, notificationData)

                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse!!.success == 0) {
                                    compositeDisposable.clear()

                                    Snackbar.make(
                                        view,
                                        "Complete Trip Failed",
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                } else {
                                    Snackbar.make(
                                        view,
                                        "Thank you! You have complete the trip",
                                        Snackbar.LENGTH_LONG
                                    ).show()

                                }
                            }, { t: Throwable? ->
                                compositeDisposable.clear()

                                Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                Log.d("UserUtils", "  ${t.message}")
                            })
                        )
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(view, "Token Not Found", Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }

            })


    }
}