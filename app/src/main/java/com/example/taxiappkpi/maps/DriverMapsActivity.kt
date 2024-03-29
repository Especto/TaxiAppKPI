package com.example.taxiappkpi.maps

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taxiappkpi.References
import com.example.taxiappkpi.Models.Filters.RiderFilter
import com.example.taxiappkpi.Models.Trip.TripRequest
import com.example.taxiappkpi.Models.User.RiderInfo
import com.example.taxiappkpi.Models.User.toDriverInfo
import com.example.taxiappkpi.Models.User.toRiderInfo
import com.example.taxiappkpi.Models.Trip.toTripRequest
import com.example.taxiappkpi.Models.User.toUserInfo
import com.example.taxiappkpi.R
import com.example.taxiappkpi.databinding.ActivityDriverMapsBinding
import com.example.taxiappkpi.login.WelcomeActivity
import com.example.taxiappkpi.settings.FiltersForDriverActivity
import com.example.taxiappkpi.settings.SettingsUserActivity
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class DriverMapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityDriverMapsBinding
    private lateinit var googleApiClient: GoogleApiClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val SETTINGS_ACTIVITY_CODE = 2
    private val FILTERS_ACTIVITY_CODE = 3

    private var lastLocation = Location("Start")

    private lateinit var manageTripLayout: ConstraintLayout
    private lateinit var setRatingLayout: LinearLayoutCompat
    private lateinit var buttonsConfirmationLayout: LinearLayout
    private lateinit var statusButton: Button
    private lateinit var distanceToRiderTV: TextView
    private lateinit var distanceToFinishTV: TextView

    private lateinit var driversWorkingRef: DatabaseReference
    private lateinit var ridersInfoRef: DatabaseReference
    private lateinit var tripRequestRef: DatabaseReference
    private lateinit var driversAvailableRef: DatabaseReference
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var driversInfoRef: DatabaseReference

    private lateinit var assignedListener: ValueEventListener
    private lateinit var tripStatusListener: ValueEventListener

    private lateinit var driverID: String

    private var riderID: String = ""
    private var riderFound = false
    private var riderFilter = RiderFilter()
    private lateinit var riderInfo: RiderInfo
    private lateinit var tripRequest: TripRequest
    private lateinit var runnable: Runnable

    private var stopTripConfirmation = 0
    private var tripStatus = -1

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        driverID = mAuth.currentUser!!.uid

        driversWorkingRef = FirebaseDatabase.getInstance().reference.child(References.DRIVERS_WORKING_REFERENCE)
        ridersInfoRef = FirebaseDatabase.getInstance().reference.child(References.RIDERS_REFERENCE)
        tripRequestRef = FirebaseDatabase.getInstance().reference.child(References.TRIPS_REQUEST_REFERENCE)
        driversAvailableRef = FirebaseDatabase.getInstance().reference.child(References.DRIVERS_AVAILABLE_REFERENCE)
        driversLocationRef = FirebaseDatabase.getInstance().reference.child(References.DRIVERS_LOCATION_REFERENCE)
        driversInfoRef = FirebaseDatabase.getInstance().reference.child(References.DRIVERS_REFERENCE)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationButton = (mapFragment.requireView()
            .findViewById<View>("1".toInt())
            .parent!! as View).findViewById<View>("2".toInt())

        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)

        findViewById<ImageButton>(R.id.driver_settings_button).setOnClickListener {
            val intent = Intent(this@DriverMapsActivity, SettingsUserActivity::class.java)
            intent.putExtra("ROLE", "driver")
            intent.putExtra("IN_TRIP", riderFound)
            startActivityForResult(intent, SETTINGS_ACTIVITY_CODE)
        }

        findViewById<ImageButton>(R.id.driver_filter_button).setOnClickListener {
            startActivityForResult(Intent(this@DriverMapsActivity, FiltersForDriverActivity::class.java).putExtra("riderFilter", riderFilter), FILTERS_ACTIVITY_CODE)
        }

        getAssignedRider()
    }

    private fun getAssignedRider() {
        assignedListener = object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("log_check", "чек предложения $snapshot")
                if (snapshot.exists() && !riderFound) {
                    for (childSnapshot in snapshot.children) {
                        val key = childSnapshot.key
                        val value = childSnapshot.value
                        if (value == false) {
                            ridersInfoRef.child(key!!).addListenerForSingleValueEvent(object :
                                ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val riderInfoMap = snapshot.value!! as Map<*, *>
                                        riderInfo = riderInfoMap.toRiderInfo()
                                        Log.d("log_check", "Rating = $riderInfo")

                                        if(riderFilter.userFilter.gender == 0 || riderFilter.userFilter.gender == 1){
                                            if(riderFilter.userFilter.gender != riderInfo.userInfo.gender){
                                                return
                                            }
                                        }
                                        if(riderFilter.userFilter.minAge != -1){
                                            if(riderFilter.userFilter.minAge > (2024-riderInfo.userInfo.birthdate.year)){
                                                return
                                            }
                                        }

                                        if(riderFilter.userFilter.maxAge != -1){
                                            if(riderFilter.userFilter.maxAge < (2024-riderInfo.userInfo.birthdate.year)){
                                                return
                                            }
                                        }
                                        Log.d("log_check", "получили предложение заказа")
                                        riderID = key
                                        Log.d("log_check", "вызываем вызов чека позиций")
                                        getTripData()

                                        runnable = Runnable{
                                            Log.d("log_check", "отработал хендлер - $riderFound")
                                            stopTrip()
                                        }
                                        handler.postDelayed(runnable, 10000)
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                }
                            })
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        driversWorkingRef.child(driverID).addValueEventListener(assignedListener)
    }

    private fun showRoute() {
        mMap.clear()
        Log.d("log_check", "отображаем маркеры")
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tripRequest.start, 14f))
        mMap.addMarker(MarkerOptions().position(tripRequest.start).title("Початкова точка")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))

        mMap.addMarker(MarkerOptions().position(tripRequest.finish).title("Кінцева точка")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))

        val polylineOptions = PolylineOptions()
        polylineOptions.addAll(tripRequest.route)
        Log.d("log_check", "polyline нарисован, но что за маршрут $tripRequest.route")
        polylineOptions.width(10f)
        polylineOptions.color(R.color.orange)
        mMap.addPolyline(polylineOptions)
    }

    @SuppressLint("CutPasteId")
    private fun initTripWidget() {
        Log.d("log_check", "запуск виджета")
        Log.d("log_check", "${riderInfo.userInfo}")
        val avatarIV: CircleImageView = findViewById(R.id.riderImage)
        if(riderInfo.userInfo.avatar != ""){
            Picasso.get().load(riderInfo.userInfo.avatar).into(avatarIV)
        }
        buttonsConfirmationLayout = findViewById(R.id.buttonsConfirmationLayout)
        manageTripLayout = findViewById(R.id.tripManageLayout)
        statusButton = findViewById(R.id.statusButton)
        setRatingLayout = findViewById(R.id.driverSetRatingLayout)

        val riderImgIV: ImageView = findViewById(R.id.riderImage)

        val riderRatingTV: TextView = findViewById(R.id.riderRating)
        val riderNameTV: TextView = findViewById(R.id.riderName)
        val riderNumberTV: TextView = findViewById(R.id.riderNumber)
        val tripCostTV: TextView = findViewById(R.id.riderTripPrice)
        distanceToRiderTV = findViewById(R.id.distanceToRider)
        distanceToFinishTV = findViewById(R.id.driverDistanceToFinish)
        val tripDistanceTV: TextView = findViewById(R.id.riderTripDistance)

        val acceptButton: Button = findViewById(R.id.acceptButton)
        val cancelButton: Button = findViewById(R.id.cancelButton)
        val stopTripButton: Button = findViewById(R.id.stopButton)

        manageTripLayout.visibility = View.VISIBLE
        buttonsConfirmationLayout.visibility = View.VISIBLE

        if(riderInfo.userInfo.avatar!=""){
            val uriStr = riderInfo.userInfo.avatar
            Picasso.get().load(uriStr).into(riderImgIV)
        }
        distanceToFinishTV.visibility = View.GONE
        distanceToRiderTV.visibility = View.VISIBLE
        riderRatingTV.text = getString(R.string.userRating, riderInfo.userInfo.rating)
        riderNameTV.text = riderInfo.userInfo.firstName
        distanceToFinishTV.text = getString(R.string.distanceToFinish, tripRequest.distance)
        riderNumberTV.text = riderInfo.userInfo.phoneNumber
        tripCostTV.text = getString(R.string.tripPrice, tripRequest.price)
        tripDistanceTV.text = getString(R.string.distanceOfTrip, tripRequest.distance)

        manageTripLayout.visibility = View.VISIBLE

        stopTripButton.setOnClickListener {
            Log.d("log_check", "нажата кнопка отмены")
            stopTripConfirmation++
            if(stopTripConfirmation < 2){
                Toast.makeText(this@DriverMapsActivity, "Натисніть ще раз щоб відмінити замовлення", Toast.LENGTH_SHORT).show()
            }else{

                Log.d("log_check", "Отменяем заказ")
                tripRequestRef.child(riderID).child("status").setValue(3)
                driversInfoRef.child(driverID).addListenerForSingleValueEvent(object :
                    ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val driverInfoMap = snapshot.value as Map<*, *>
                            val driverInfo = driverInfoMap.toDriverInfo()

                            if (driverInfo.userInfo.rating!= 0.0){
                                driverInfo.userInfo.rating = ((driverInfo.userInfo.rating * driverInfo.userInfo.numTrips) + 0) / (driverInfo.userInfo.numTrips + 1)
                            }else{
                                driverInfo.userInfo.rating = -0.5
                            }
                            driverInfo.userInfo.numTrips += 1
                            Log.d("log_check", "ставим плохой отзыв $driverInfo")
                            driversInfoRef.child(driverID).updateChildren(driverInfo.toMap())
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
                tripRequestRef.child(riderID).child("status").removeEventListener(tripStatusListener)

                stopTripButton.visibility = View.GONE
                statusButton.visibility = View.GONE
                buttonsConfirmationLayout.visibility = View.VISIBLE
                stopTrip()

            }
        }

        cancelButton.setOnClickListener {
            Log.d("log_check", "нажали отмену заказа")
            handler.removeCallbacks(runnable)
            Toast.makeText(this@DriverMapsActivity, "Замовлення відхилено", Toast.LENGTH_SHORT).show()
            stopTrip()
        }
        acceptButton.setOnClickListener {
            Log.d("log_check", "нажали принятие заказа")
            handler.removeCallbacks(runnable)
            driversWorkingRef.child(driverID).child(riderID).setValue(true)

            Toast.makeText(this@DriverMapsActivity, "Замовлення прийнято", Toast.LENGTH_SHORT).show()

            buttonsConfirmationLayout.visibility = View.GONE
            statusButton.visibility = View.VISIBLE
            stopTripButton.visibility = View.VISIBLE
            riderFound = true
            statusButton.isEnabled = false
            Log.d("log_check", "слушатель новых заказов отключается")
            driversWorkingRef.child(driverID).removeEventListener(assignedListener)
            listenTripStatus()
        }

        statusButton.setOnClickListener {
            Log.d("log_check", "нажали смену заказа")
            when(tripStatus){
                0->{ //водій в 100м від старту
                    Log.d("log_check", "смена заказа $tripStatus")
                    tripRequestRef.child(riderID).child("status").setValue(tripStatus)
                    statusButton.text = getString(R.string.pickedUpClient)
                    tripStatus = 1
                }
                1 ->{ //підібрав пасажира
                    Log.d("log_check", "смена заказа $tripStatus")
                    tripRequestRef.child(riderID).child("status").setValue(tripStatus)
                    statusButton.text = getString(R.string.successedTrip)
                    distanceToFinishTV.visibility = View.VISIBLE
                    distanceToRiderTV.visibility = View.GONE
                    statusButton.isEnabled = false
                    tripStatus = 2
                }
                2 ->{ //закінчив замовлення
                    Log.d("log_check", "смена заказа $tripStatus")
                    tripRequestRef.child(riderID).child("status").setValue(tripStatus)
                    statusButton.visibility = View.GONE
                    stopTripButton.visibility = View.GONE
                    setRatingLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun stopTrip() {
        mMap.clear()

        driversWorkingRef.child(driverID).removeValue()
        driversAvailableRef.child(driverID).setValue(true)
        tripStatus = -1
        riderID = ""
        riderFound = false
        manageTripLayout.visibility = View.GONE
    }

    private fun getTripData() {
        Log.d("log_check", "это функция позиции, следующий должен быть snapshot")
        tripRequestRef.child(riderID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("log_check", "$snapshot")
                if (snapshot.exists()) {
                    val tripRequestMap = snapshot.value as Map<*, *>
                    tripRequest = tripRequestMap.toTripRequest()

                    val distanceToRider = Location("").apply {
                        latitude = tripRequest.start.latitude
                        longitude = tripRequest.start.longitude
                    }.distanceTo(Location("").apply {
                        latitude = lastLocation.latitude
                        longitude = lastLocation.longitude
                    })/1000

                    Log.d("log_check", "получили корды, отображаем корды")
                    showRoute()
                    Log.d("log_check", "открываем виджет")
                    initTripWidget()
                    distanceToRiderTV.text = getString(R.string.distanceToRider, distanceToRider)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun listenTripStatus() {
        val titleMessage = "Зміна статусу замовлення"
        Log.d("log_check", "начинаем слушать статус поездки")
        tripStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val tripStatus = (snapshot.value as? Long)?.toInt() ?: 0
                    when (tripStatus) {
                        4 -> {
                            Log.d("log_check", "статус 4, клиент отказался")
                            tripRequestRef.child(riderID).removeEventListener(this)
                            tripRequestRef.child(riderID).removeValue()
                            val notificationMessage = "Клієнт відмінив замовлення"
                            showNotification(notificationMessage, titleMessage)
                            getAssignedRider()
                            stopTrip()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        }
        tripRequestRef.child(riderID).child("status").addValueEventListener(tripStatusListener)
    }

    private fun showNotification(message: String, title: String) {
        Log.d("log_check", "$message, отправляем уведомление")

        val channelId = "my_channel_id"
        val notificationId = 1

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(this)

        createNotificationChannel()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "main",
                "TaxiAppKpi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {}

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.isMyLocationEnabled = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }


        buildGoogleApiClient()
    }

    override fun onConnected(p0: Bundle?) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).apply {
            setMinUpdateDistanceMeters(50f)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
        driversAvailableRef.child(driverID).setValue(true)
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onLocationChanged(p0: Location) {
        lastLocation = p0
        val latLng = LatLng(p0.latitude, p0.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))

        GeoFire(driversLocationRef).setLocation(driverID, GeoLocation(p0.latitude, p0.longitude))

        if(riderFound){
            Log.d("log_check", "Смена позиции, чекаем дистанцию до точки")

            if(tripStatus==-1){
                val distanceToStartPoint = lastLocation.distanceTo(Location("").apply {
                    latitude = tripRequest.start.latitude
                    longitude = tripRequest.start.longitude
                })

                distanceToRiderTV.text = getString(R.string.distanceToRider, distanceToStartPoint/1000)
                if (distanceToStartPoint < 100) {
                    Log.d("log_check", "Мы рядом с точкой старта, можно изменить статус")
                    statusButton.isEnabled = true
                    tripStatus = 0
                }
            }

            if(tripStatus==2){

                val distanceToFinishPoint = lastLocation.distanceTo(Location("").apply {
                    latitude = tripRequest.finish.latitude
                    longitude = tripRequest.finish.longitude
                })
                distanceToFinishTV.text = getString(R.string.distanceToFinish, distanceToFinishPoint/1000)

                if (distanceToFinishPoint < 50) {
                    Log.d("log_check", "Мы рядом с точкой финиша, можно изменить статус")
                    statusButton.isEnabled = true
                }
            }
        }
    }

     private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        googleApiClient.connect()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_ACTIVITY_CODE && resultCode == RESULT_OK) {
            val exit = data!!.getBooleanExtra("EXIT", false)
            if(exit){
                mAuth.signOut()
                logoutDriver()
            }
        }

        if (requestCode == FILTERS_ACTIVITY_CODE && resultCode == RESULT_OK){
            riderFilter = data!!.getParcelableExtra("FILTER")!!
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!riderFound) {
            disconnectDriver()
        }
    }

    private fun disconnectDriver() {
        driversAvailableRef.child(driverID).removeValue()
        GeoFire(driversLocationRef).removeLocation(driverID)
    }

    private fun logoutDriver() {
        disconnectDriver()
        val welcomeIntent = Intent(this@DriverMapsActivity, WelcomeActivity::class.java)
        startActivity(welcomeIntent)
        finish()
    }

    fun onStarClickedDriver(view: View) {
        val starIdStr = view.tag as String
        val starId = starIdStr.toInt()

        if (riderInfo.userInfo.numTrips != 0) {
            riderInfo.userInfo.rating = (((riderInfo.userInfo.rating * riderInfo.userInfo.numTrips) + starId) / (riderInfo.userInfo.numTrips + 1))
        } else {
            riderInfo.userInfo.rating = starId.toDouble()
        }
        riderInfo.userInfo.numTrips += 1


        ridersInfoRef.child(riderID).child("userInfo").updateChildren(riderInfo.userInfo.toMap())

        tripRequestRef.child(riderID).child("status").removeEventListener(tripStatusListener)

        getAssignedRider()
        setRatingLayout.visibility = View.GONE

        stopTrip()
    }
}