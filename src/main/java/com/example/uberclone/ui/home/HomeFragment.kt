package com.example.uberclone.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.uberclone.DrivierHomeActivity
import com.example.uberclone.R
import com.example.uberclone.Remote.IGoogleAPI
import com.example.uberclone.Remote.RetrofitClient
import com.example.uberclone.Utils.LocationUtils
import com.example.uberclone.Utils.UserUtils
import com.example.uberclone.common
import com.example.uberclone.databinding.FragmentHomeBinding
import com.example.uberclone.model.Model.EventBus.DriveRequestReceived
import com.example.uberclone.model.Model.EventBus.NotifyRiderEvent
import com.example.uberclone.model.RiderInfoData
import com.example.uberclone.model.TripPlanModel
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import com.kusu.library.LoadingButton
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class HomeFragment : Fragment(), OnMapReadyCallback {


    private var cityName: String = ""
    private lateinit var txt_rating: TextView
    private lateinit var txt_type_uber: TextView
    private lateinit var img_round: ImageView
    private lateinit var layout_start_uber: CardView
    private lateinit var txt_rider_name: TextView
    private lateinit var txt_start_uber_estimate_time: TextView
    private lateinit var txt_start_uber_estimate_distance: TextView
    private lateinit var img_phone_call: ImageView
    private lateinit var btn_start_uber: LoadingButton
    private lateinit var btn_complete_trip: LoadingButton

    private var isTripStart = false
    private var onlineSystemAlreadyRegister = false

    private var tripNumberId: String = ""

    //Views
    private lateinit var chip_decline: Chip
    private lateinit var layout_accept: CardView
    private lateinit var circularProgressBar: CircularProgressBar
    private lateinit var txt_estimate_time: TextView
    private lateinit var txt_estimate_distance: TextView
    private lateinit var root_layout: FrameLayout


    private lateinit var layout_notify_rider: LinearLayout
    private lateinit var txt_notify_rider: TextView
    private lateinit var progress_notify: ProgressBar

    private var pickupGeoFire: GeoFire? = null
    private var pickUpGeoQuery: GeoQuery? = null

    private var destinationGeoFire: GeoFire? = null
    private var destinationGeoQuery: GeoQuery? = null

    private val pickupGeoQueryListener = object : GeoQueryEventListener {
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            btn_start_uber.isEnabled = true
            UserUtils.sendNotifyToRider(requireContext(), root_layout, key)
            if (pickUpGeoQuery != null) {
                //Remove
                pickupGeoFire!!.removeLocation(key)
                pickupGeoFire = null
                pickUpGeoQuery!!.removeAllListeners()
            }
        }

        override fun onKeyExited(key: String?) {
            btn_start_uber.isEnabled = false
        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {

        }

        override fun onGeoQueryReady() {

        }

        override fun onGeoQueryError(error: DatabaseError?) {

        }

    }
    private val destinationGeoQueryListener = object : GeoQueryEventListener {
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            Toast.makeText(requireContext(), "Destination Entered", Toast.LENGTH_LONG).show()
            btn_complete_trip.isEnabled = true
            if (destinationGeoQuery != null) {
                destinationGeoFire!!.removeLocation(key)
                destinationGeoFire = null
                destinationGeoQuery!!.removeAllListeners()
            }
        }

        override fun onKeyExited(key: String?) {

        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {

        }

        override fun onGeoQueryReady() {

        }

        override fun onGeoQueryError(error: DatabaseError?) {

        }

    }
    private var waiting_timer: CountDownTimer? = null

    //Decline
    private var driveRequestReceived: DriveRequestReceived? = null
    private var countDownEvent: Disposable? = null

    //Routes
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineList: ArrayList<LatLng?>? = null

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    private var locationRequest: com.google.android.gms.location.LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private val REQUEST_LOCATION_PERMISSION = 1

    private lateinit var mapFragment: SupportMapFragment

    private val binding get() = _binding!!

    //Online System
    private lateinit var onlineRef: DatabaseReference
    private var currentUserRef: DatabaseReference? = null
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentUserRef != null) {
                currentUserRef!!.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("HomeFragment", "onCreateView called")
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        initViews(root)
        init()
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Log.d("HomeFragment", "onCreateView finished")
        return root
    }

    private fun initViews(root: View) {
        chip_decline = root!!.findViewById(R.id.chip_decline) as Chip
        layout_accept = root!!.findViewById(R.id.layout_accept) as CardView
        circularProgressBar = root!!.findViewById(R.id.circularProgressBar) as CircularProgressBar
        txt_estimate_distance = root!!.findViewById(R.id.txt_estimate_distance) as TextView
        txt_estimate_time = root!!.findViewById(R.id.txt_estimate_time) as TextView
        root_layout = root!!.findViewById(R.id.root_layout) as FrameLayout


        txt_rating = root!!.findViewById(R.id.txt_rating) as TextView
        txt_type_uber = root!!.findViewById(R.id.txt_type_uber) as TextView
        img_round = root!!.findViewById(R.id.img_round) as ImageView
        layout_start_uber = root!!.findViewById(R.id.layout_start_uber) as CardView
        txt_rider_name = root!!.findViewById(R.id.txt_rider_name) as TextView
        txt_start_uber_estimate_time =
            root!!.findViewById(R.id.txt_start_ube_estimate_time) as TextView
        txt_start_uber_estimate_distance =
            root!!.findViewById(R.id.txt_start_uber_estimate_distance) as TextView
        img_phone_call = root!!.findViewById(R.id.img_phone_call) as ImageView
        btn_start_uber = root!!.findViewById(R.id.btn_start_uber) as LoadingButton
        btn_complete_trip = root!!.findViewById(R.id.btn_complete_trip) as LoadingButton


        layout_notify_rider = root.findViewById(R.id.layout_notify_rider) as LinearLayout
        txt_notify_rider = root.findViewById(R.id.txt_notify_rider) as TextView
        progress_notify = root.findViewById(R.id.progress_notify) as ProgressBar

        //Event
        chip_decline.setOnClickListener {
            if (driveRequestReceived != null) {
                if (TextUtils.isEmpty(tripNumberId)) {
                    if (countDownEvent != null) {
                        countDownEvent!!.dispose()
                    }
                    chip_decline.visibility = View.GONE
                    layout_accept.visibility = View.GONE
                    mMap.clear()
                    circularProgressBar.progress = 0f
                    UserUtils.sendDeclineRequest(
                        root_layout,
                        requireActivity(),
                        driveRequestReceived!!.key!!
                    )
                    driveRequestReceived = null
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Snackbar.make(
                            mapFragment.requireView(),
                            "Permission Required",
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener { e ->
                            Snackbar.make(
                                mapFragment.requireView(),
                                e.message!!,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        .addOnSuccessListener { location ->
                            chip_decline.visibility = View.GONE
                            layout_start_uber.visibility = View.GONE
                            mMap.clear()
                            UserUtils.sendDeclineAndRemoveTripRequest(
                                root_layout,
                                requireActivity(),
                                driveRequestReceived!!.key!!,
                                tripNumberId
                            )
                            tripNumberId = ""//set it to empty after remove
                            driveRequestReceived = null
                            makeDriverOnline(location)

                        }
                }

            }
        }

        btn_start_uber.setOnClickListener {
            if (blackPolyline != null) blackPolyline!!.remove()
            if (greyPolyline != null) greyPolyline!!.remove()
            //cancel waiting time
            if (waiting_timer != null) waiting_timer!!.cancel()
            layout_notify_rider.visibility = View.GONE
            if (driveRequestReceived != null) {
                val destinationLatLng = LatLng(
                    driveRequestReceived!!.destinationLocation!!.split(",")[0].toDouble(),
                    driveRequestReceived!!.destinationLocation!!.split(",")[1].toDouble()
                )
                mMap.addMarker(
                    MarkerOptions().position(destinationLatLng)
                        .title(driveRequestReceived!!.destinationLocationString)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                )
                //Draw path
                drawPathFromCurrentLocation(driveRequestReceived!!.destinationLocation)

            }
            btn_start_uber.visibility = View.GONE
            chip_decline.visibility = View.GONE
            btn_complete_trip.visibility = View.VISIBLE
        }
        btn_complete_trip.setOnClickListener {
//            Toast.makeText(requireContext(),"Complete trip fake action",Toast.LENGTH_LONG).show()
            //update trip
            val update_trip = HashMap<String, Any>()
            update_trip.put("done", true)
            FirebaseDatabase.getInstance().getReference(common.TRIP)
                .child(tripNumberId!!)
                .updateChildren(update_trip)
                .addOnFailureListener { e ->
                    Snackbar.make(
                        requireView(),
                        e.message!!,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                .addOnSuccessListener {
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener { e ->
                            Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                        }
                        .addOnSuccessListener { location ->
                            UserUtils.sendCompleteTripToRider(
                                mapFragment.requireView(),
                                requireContext(),
                                driveRequestReceived!!.key,
                                tripNumberId
                            )

                            //reset view
                            mMap.clear()
                            tripNumberId = ""
                            isTripStart = false
                            chip_decline.visibility = View.GONE
                            layout_accept.visibility = View.GONE
                            circularProgressBar.progress = 0f

                            layout_start_uber.visibility = View.GONE
                            layout_notify_rider.visibility = View.GONE

                            progress_notify.progress = 0

                            btn_complete_trip.isEnabled = false
                            btn_complete_trip.visibility = View.GONE

                            btn_start_uber.isEnabled = false
                            btn_start_uber.visibility = View.VISIBLE

                            destinationGeoFire = null
                            pickupGeoFire = null

                            driveRequestReceived = null
                            makeDriverOnline(location)

                        }
                }
        }
    }

    private fun drawPathFromCurrentLocation(destinationLocation: String?) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(), "Permission Require", Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location ->
                compositeDisposable.add(iGoogleAPI.getDirections(
                    "driving",
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),

                    destinationLocation,

                    getString(R.string.google_api_key)

                )
                !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { returnResult ->
                        Log.d("API_RETURN", returnResult)

                        try {

                            val jsonObject = JSONObject(returnResult)
                            val jsonArray = jsonObject.getJSONArray("routes")
                            for (i in 0 until jsonArray.length()) {
                                val route = jsonArray.getJSONObject(i)
                                val poly = route.getJSONObject("overview_polyline")
                                val polyline = poly.getString("points")
                                polylineList = common.decodePoly(polyline)
                            }

                            polylineOptions = PolylineOptions()
                            polylineOptions!!.color(Color.GRAY)
                            polylineOptions!!.width(12f)
                            polylineOptions!!.startCap(SquareCap())
                            polylineOptions!!.jointType(JointType.ROUND)
                            polylineOptions!!.addAll(polylineList!!)
                            greyPolyline = mMap.addPolyline(polylineOptions!!)

                            blackPolylineOptions = PolylineOptions()
                            blackPolylineOptions!!.color(Color.BLACK)
                            blackPolylineOptions!!.width(5f)
                            blackPolylineOptions!!.startCap(SquareCap())
                            blackPolylineOptions!!.jointType(JointType.ROUND)
                            blackPolylineOptions!!.addAll(polylineList!!)
                            blackPolyline = mMap.addPolyline(blackPolylineOptions!!)


                            val origin = LatLng(location.latitude, location.longitude)
                            val destination = LatLng(
                                destinationLocation!!.split(",")[0].toDouble(),
                                destinationLocation!!.split(",")[1].toDouble()
                            )
                            val latLngBound = LatLngBounds.Builder().include(origin)
                                .include(destination)
                                .build()




                            createGeoFireDestinationLocation(
                                driveRequestReceived!!.key,
                                destination
                            )

                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    latLngBound,
                                    160
                                )
                            )
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))


                        } catch (e: java.lang.Exception) {

                            Log.d("HomeFragment", " " + e.message)
                        }
                    })
            }
    }

    private fun createGeoFireDestinationLocation(key: String?, destination: LatLng) {
        val ref = FirebaseDatabase.getInstance().getReference(common.TRIP_DESTINATION_LOCTION_REF)
        destinationGeoFire = GeoFire(ref)
        destinationGeoFire!!.setLocation(key!!,
            GeoLocation(destination.latitude, destination.longitude), { key, error ->

            })
    }

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun init() {
        Log.d("HomeFragment", "init called")
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")


        iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)

        Log.d("HomeFragment", "init in b/w")
        if (ActivityCompat.checkSelfPermission(

                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Snackbar.make(root_layout, "Permission Required", Snackbar.LENGTH_LONG).show()

            return
        }
        Log.d("HomeFragment", "init inside")
        buildLocationRequest()

        buildLocationCallBack()
        updateLocation()


        Log.d("HomeFragment", "init finished")
    }

    private fun updateLocation() {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(requireContext())


            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Snackbar.make(root_layout, "Permission Require", Snackbar.LENGTH_LONG).show()

                return
            }

            fusedLocationProviderClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )


        }
    }

    private fun buildLocationCallBack() {
        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.d("HomeFragment", "onLocationResult called")
                    super.onLocationResult(locationResult)
                    val newPos = LatLng(
                        locationResult.lastLocation.latitude,
                        locationResult.lastLocation.longitude
                    )

                    if (pickupGeoFire != null) {
                        pickUpGeoQuery =
                            pickupGeoFire!!.queryAtLocation(
                                GeoLocation(
                                    locationResult.lastLocation.latitude,
                                    locationResult.lastLocation.longitude
                                ), common.MIN_RANGE_PICKUP_IN_KM
                            )
                        Log.d("HomeFragment", "$pickUpGeoQuery  driver arived")
                        pickUpGeoQuery!!.addGeoQueryEventListener(pickupGeoQueryListener)
                    }

                    if (destinationGeoFire != null) {
                        destinationGeoQuery =
                            destinationGeoFire!!.queryAtLocation(
                                GeoLocation(
                                    locationResult.lastLocation.latitude,
                                    locationResult.lastLocation.longitude
                                ), common.MIN_RANGE_PICKUP_IN_KM
                            )
                        destinationGeoQuery!!.addGeoQueryEventListener(destinationGeoQueryListener)
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 10f))

                    if (!isTripStart) {

                        makeDriverOnline(locationResult.lastLocation!!)
                    } else {
                        if (!TextUtils.isEmpty(tripNumberId)) {
                            //update location
                            val update_data = HashMap<String, Any>()
                            update_data["currentLat"] = locationResult.lastLocation.latitude
                            update_data["currentLng"] = locationResult.lastLocation.longitude

                            FirebaseDatabase.getInstance().getReference(common.TRIP)
                                .child(tripNumberId!!)
                                .updateChildren(update_data)
                                .addOnFailureListener { e ->
                                    Snackbar.make(
                                        mapFragment.requireView(),
                                        e.message!!,
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                                .addOnSuccessListener { }
                        }
                    }

                }
            }
        }
    }

    private fun makeDriverOnline(location: Location) {
        val saveCityName = cityName// first save old city name to variable
        Log.d("HomeFragment", "$cityName  in makeDriverOnline")
        cityName = LocationUtils.getAddressFromLocation(
            requireContext(),
            location
        )// get new city name of location
        Log.d("HomeFragment", "$cityName  in makeDriverOnline")
        if (cityName == "New DelhiIN") cityName = "DelhiIN"
        if (!cityName.equals(saveCityName))//If old cityname and new city aren't equal
        {
            Log.d("HomeFragment", "new and old city name is not equal")
            if (currentUserRef != null) {
                Log.d("HomeFragment", "currentUserRef!=null")
                currentUserRef!!.removeValue()//Remove - to prevent 1 driver have 2 location
                    .addOnFailureListener { e ->
                        Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG)
                            .show()
                        Log.d("HomeFragment", "${e.message!!} int makeDriverOnline")
                    }
                    .addOnSuccessListener {
                        Log.d("HomeFragment", "success in removing")

                    }
            }

        }
//        else
        Log.d("HomeFragment", "updateDriverLocation to be called")
        updateDriverLocation(location)//Update without delete old position


    }


    private fun updateDriverLocation(location: Location) {
        Log.d("HomeFragment", "updateDriver Location called")
        if (!TextUtils.isEmpty(cityName)) {
            Log.d("HomeFragment", "updateDriverLocation  cityname-$cityName is not empty")
            driversLocationRef = FirebaseDatabase.getInstance()
                .getReference(common.DRIVERS_LOCATION_REFERENCE)
                .child(cityName)

            currentUserRef = driversLocationRef.child(
                FirebaseAuth.getInstance().currentUser!!.uid
            )
            geoFire = GeoFire(driversLocationRef)

            geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid,
                GeoLocation(
                    location.latitude,
                    location.longitude
                )
            ) { key: String?, error: DatabaseError? ->
                if (error != null) {
                    Snackbar.make(
                        mapFragment.requireView(),
                        error.message,
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                registerOnlineSystem()
            }
        } else
            Snackbar.make(
                mapFragment.requireView(),
                "Service Unavailable",
                Snackbar.LENGTH_LONG
            ).show()
    }

    private fun buildLocationRequest() {
        if (locationRequest == null) {
            locationRequest = com.google.android.gms.location.LocationRequest()
            locationRequest!!.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
            locationRequest!!.setFastestInterval(15000)//15sec
            locationRequest!!.interval = 10000//10sec
            locationRequest!!.setSmallestDisplacement(50f)//5m
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        Log.d("HomeFragment", "ondestroy called")
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)

        compositeDisposable.clear()

        onlineSystemAlreadyRegister = false
        if (EventBus.getDefault().hasSubscriberForEvent(DrivierHomeActivity::class.java))
            EventBus.getDefault().removeStickyEvent(DrivierHomeActivity::class.java)
//        if(EventBus.getDefault().hasSubscriberForEvent(DriveRequestReceived::class.java))
//            EventBus.getDefault().removeStickyEvent(DriveRequestReceived::class.java)
        if (EventBus.getDefault().hasSubscriberForEvent(NotifyRiderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(NotifyRiderEvent::class.java)


        EventBus.getDefault().unregister(this)

        super.onDestroy()
        _binding = null
    }

    override fun onResume() {
        Log.d("HomeFragment", "onResume called")
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        if (!onlineSystemAlreadyRegister) {
            onlineRef.addValueEventListener(onlineValueEventListener)
            onlineSystemAlreadyRegister = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            Log.d("HomeFragment", "my location enabled")
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            Log.d("HomeFragment", "button enable")
            mMap.setOnMyLocationClickListener {
                fusedLocationProviderClient!!.lastLocation
                    .addOnFailureListener { e ->
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                    .addOnSuccessListener { location ->
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                userLatLng,
                                10f
                            )
                        )
                    }
                true
            }
            //Layout
            val view = mapFragment.requireView()
                .findViewById<View>("1".toInt())!!
                .parent as View
            val locationButtion = view.findViewById<View>("2".toInt())
            val params = locationButtion.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            params.bottomMargin = 250

            //Location
            buildLocationRequest()

            buildLocationCallBack()
            updateLocation()
        } else {
            Log.d("HomeFragment", "permission asked in enabledMyLocation()")
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("HomeFragment", "onMapReady called")
        mMap = googleMap
        Log.d("HomeFragment", "mMap taken")
        mMap.uiSettings.isZoomControlsEnabled = true
        enableMyLocation()
        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        return
                    }
                    Log.d("HomeFragment", "to enable")
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    Log.d("HomeFragment", "button enable")
                    mMap.setOnMyLocationClickListener {
                        fusedLocationProviderClient!!.lastLocation
                            .addOnFailureListener { e ->
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                            .addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLatLng,
                                        10f
                                    )
                                )
                            }
                        true
                    }
                    //Layout
                    val view = mapFragment.requireView()
                        .findViewById<View>("1".toInt())!!
                        .parent as View
                    val locationButtion = view.findViewById<View>("2".toInt())
                    val params = locationButtion.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 250

                    //Location
                    buildLocationRequest()

                    buildLocationCallBack()
                    updateLocation()


                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Log.d("HomeFragment", "Pemission Denied ")
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: com.karumi.dexter.listener.PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    Toast.makeText(context, "Permission $p0 denied", Toast.LENGTH_LONG).show()
                }


            })
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.uber_maps_style
                )
            )
            if (!success) {
                Log.e("EDMT error", "style parsing error")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("EDMT error", e.message.toString())
        }
        Log.d("HomeFragment", "onMapReady finished")
        Snackbar.make(mapFragment.requireView(), "You'r online", Snackbar.LENGTH_LONG).show()

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onDriveRequestReceived(event: DriveRequestReceived) {
        driveRequestReceived = event
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(), "Permission Require", Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location ->
                compositeDisposable.add(iGoogleAPI.getDirections(
                    "driving",
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),

                    event.pickupLocation,

                    getString(R.string.google_api_key)

                )
                !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { returnResult ->
                        Log.d("API_RETURN", returnResult)

                        try {

                            val jsonObject = JSONObject(returnResult)
                            val jsonArray = jsonObject.getJSONArray("routes")
                            for (i in 0 until jsonArray.length()) {
                                val route = jsonArray.getJSONObject(i)
                                val poly = route.getJSONObject("overview_polyline")
                                val polyline = poly.getString("points")
                                polylineList = common.decodePoly(polyline)
                            }

                            polylineOptions = PolylineOptions()
                            polylineOptions!!.color(Color.GRAY)
                            polylineOptions!!.width(12f)
                            polylineOptions!!.startCap(SquareCap())
                            polylineOptions!!.jointType(JointType.ROUND)
                            polylineOptions!!.addAll(polylineList!!)
                            greyPolyline = mMap.addPolyline(polylineOptions!!)

                            blackPolylineOptions = PolylineOptions()
                            blackPolylineOptions!!.color(Color.BLACK)
                            blackPolylineOptions!!.width(5f)
                            blackPolylineOptions!!.startCap(SquareCap())
                            blackPolylineOptions!!.jointType(JointType.ROUND)
                            blackPolylineOptions!!.addAll(polylineList!!)
                            blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

                            //Animator
                            val valueAnimator = ValueAnimator.ofInt(0, 100)
                            valueAnimator.duration = 1100
                            valueAnimator.repeatCount = ValueAnimator.INFINITE
                            valueAnimator.interpolator = LinearInterpolator()
                            valueAnimator.addUpdateListener { value ->
                                val points = greyPolyline!!.points
                                val percentValue = value.animatedValue.toString().toInt()
                                val size = points.size
                                val newPoints = (size * (percentValue / 100.0f)).toInt()
                                val p = points.subList(0, newPoints)
                                blackPolyline!!.points = (p)

                            }
                            valueAnimator.start()
                            val origin = LatLng(location.latitude, location.longitude)
                            val destination = LatLng(
                                event.pickupLocation!!.split(",")[0].toDouble(),
                                event.pickupLocation!!.split(",")[1].toDouble()
                            )
                            val latLngBound = LatLngBounds.Builder().include(origin)
                                .include(destination)
                                .build()
                            //Add car icon for origin
                            val objects = jsonArray.getJSONObject(0)
                            val legs = objects.getJSONArray("legs")
                            val legsObject = legs.getJSONObject(0)

                            val time = legsObject.getJSONObject("duration")
                            val duration = time.getString("text")

                            val distanceEstimate = legsObject.getJSONObject("distance")
                            val distance = time.getString("text")

                            mMap.addMarker(
                                MarkerOptions().position(destination)
                                    .icon(BitmapDescriptorFactory.defaultMarker())
                                    .title("pickup Location")
                            )

                            createGeoFirePickupLocation(event.key, destination)

                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    latLngBound,
                                    160
                                )
                            )
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))


                            //Display Layout
                            chip_decline.visibility = View.VISIBLE
                            layout_accept.visibility = View.VISIBLE

                            //CountDown
                            countDownEvent = Observable.interval(100, TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext { x ->

                                    circularProgressBar.progress += 1f
                                }
                                .takeUntil { aLong -> aLong == "100".toLong() }//10sec
                                .doOnComplete {

                                    createTripPlan(event, duration, distance)
                                }.subscribe()


                        } catch (e: java.lang.Exception) {

                            Log.d("HomeFragment", " " + e.message)
                        }
                    })
            }
    }

    private fun createGeoFirePickupLocation(key: String?, destination: LatLng) {
        val ref = FirebaseDatabase.getInstance()
            .getReference(common.TRIP_PICKUP_KEY)
        pickupGeoFire = GeoFire(ref)
        pickupGeoFire!!.setLocation(key, GeoLocation(destination.latitude, destination.longitude),
            { key1, error ->
                if (error != null) {
                    Log.d("HomeFragment", "pickupGeoFire cannot created")
                    Snackbar.make(root_layout, error.message, Snackbar.LENGTH_LONG).show()
                } else
                    Log.d("HomeFragment", key1 + "was create success")
            }
        )
    }

    private fun createTripPlan(event: DriveRequestReceived, duration: String, distance: String) {
        setLayoutProcess(true)
        //sync server time with device
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val timeOffset = snapshot.getValue(Long::class.java)

                    val estimateTimeInMs = System.currentTimeMillis() + timeOffset!!
                    var timeText = SimpleDateFormat("dd/MM/yyyy HH:mm aa")
                        .format(estimateTimeInMs)

                    //Load rider information
                    FirebaseDatabase.getInstance()
                        .getReference(common.RIDER_INFO)
                        .child(event!!.key!!)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val ridermodel = snapshot.getValue(RiderInfoData::class.java)

                                    //get location
                                    if (ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Snackbar.make(
                                            mapFragment.requireView()!!,
                                            "Permission Require",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                        return
                                    }
                                    fusedLocationProviderClient!!.lastLocation
                                        .addOnFailureListener { e ->
                                            Snackbar.make(
                                                mapFragment.requireView()!!,
                                                e.message!!,
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                        }
                                        .addOnSuccessListener { location ->
                                            //create Trip planner
                                            val tripPlanModel = TripPlanModel()
                                            tripPlanModel.driver =
                                                FirebaseAuth.getInstance().currentUser!!.uid
                                            tripPlanModel.rider = event!!.key
                                            tripPlanModel.driverInfoData = common.currentUser
                                            tripPlanModel.riderInfoData = ridermodel
                                            tripPlanModel.origin = event.pickupLocation
                                            tripPlanModel.originString = event.pickupLocationString
                                            tripPlanModel.destination = event.destinationLocation
                                            tripPlanModel.destinationString =
                                                event.destinationLocationString
                                            tripPlanModel.distancePickup = distance
                                            tripPlanModel.durationPickup = duration
                                            tripPlanModel.currentLat = location.latitude
                                            tripPlanModel.currentLng = location.longitude


                                            //New info
                                            tripPlanModel.timeText = timeText
                                            tripPlanModel.distanceText = event.distanceText!!
                                            tripPlanModel.durationText = event.durationText!!
                                            tripPlanModel.distanceValue = event.distanceValue
                                            tripPlanModel.durationValue = event.durationValue
                                            tripPlanModel.totalFee = event.totalFee

                                            tripNumberId =
                                                common.createUniueTripNumber(timeOffset)!!
                                            //submit
                                            FirebaseDatabase.getInstance().getReference(common.TRIP)
                                                .child(tripNumberId)
                                                .setValue(tripPlanModel)
                                                .addOnFailureListener { e ->
                                                    Snackbar.make(
                                                        mapFragment.requireView()!!,
                                                        e!!.message!!,
                                                        Snackbar.LENGTH_LONG
                                                    ).show()
                                                }
                                                .addOnSuccessListener { aVoid ->
                                                    txt_rider_name.setText(ridermodel!!.first_name)
                                                    txt_start_uber_estimate_distance.setText(
                                                        distance
                                                    )
                                                    txt_start_uber_estimate_time.setText(duration)
                                                    setoffineModelForDriver(
                                                        event,
                                                        duration,
                                                        distance
                                                    )
                                                }

                                        }
                                } else
                                    Snackbar.make(
                                        mapFragment.requireView()!!,
                                        "rider not found with key ${event.key}",
                                        Snackbar.LENGTH_LONG
                                    ).show()

                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    mapFragment.requireView()!!,
                                    error.message,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }

                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(mapFragment.requireView()!!, error.message, Snackbar.LENGTH_LONG)
                        .show()
                }

            })
    }

    private fun setoffineModelForDriver(
        event: DriveRequestReceived,
        duration: String,
        distance: String
    ) {

        UserUtils.sendAcceptRequestToRider(
            mapFragment.view,
            requireContext(),
            event.key!!,
            tripNumberId
        )
        //goto offline
        if (currentUserRef != null) currentUserRef!!.removeValue()

        setLayoutProcess(false)
        layout_accept.visibility = View.GONE
        layout_start_uber.visibility = View.VISIBLE
        isTripStart = true
    }

    private fun setLayoutProcess(process: Boolean) {
        var color = -1
        if (process) {
            color = ContextCompat.getColor(requireContext(), R.color.dark_grey)
            circularProgressBar.indeterminateMode = true
            txt_rating.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_star_24_dark_grey,
                0
            )
        } else {
            color = ContextCompat.getColor(requireContext(), android.R.color.white)
            circularProgressBar.indeterminateMode = true
            circularProgressBar.progress = 0f
            txt_rating.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_star_24,
                0
            )

        }

        txt_estimate_time.setTextColor(color)
        txt_estimate_distance.setTextColor(color)
        txt_rating.setTextColor(color)
        txt_type_uber.setTextColor(color)
        ImageViewCompat.setImageTintList(img_round, ColorStateList.valueOf(color))
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onNotifyRider(event: NotifyRiderEvent) {
        layout_notify_rider!!.visibility = View.VISIBLE
        progress_notify.max = common.WAIT_TIME_IN_MIN * 60
        val countDownTimer = object : CountDownTimer((progress_notify.max * 1000).toLong(), 1000) {
            override fun onTick(p0: Long) {
                progress_notify.progress += 1

                txt_notify_rider.text = String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(p0) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(
                            p0
                        )
                    ),
                    TimeUnit.MILLISECONDS.toSeconds(p0) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(
                            p0
                        )
                    )
                )
            }

            override fun onFinish() {
                Snackbar.make(root_layout, "Time Over", Snackbar.LENGTH_LONG).show()
            }

        }
            .start()
    }
}