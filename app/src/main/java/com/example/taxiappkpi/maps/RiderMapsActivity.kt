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
import androidx.core.content.ContextCompat
import com.example.taxiappkpi.Common
import com.example.taxiappkpi.Models.Filters.DriverFilter
import com.example.taxiappkpi.Models.Trip.RouteData
import com.example.taxiappkpi.Models.Trip.TripRequest
import com.example.taxiappkpi.Models.User.DriverInfo
import com.example.taxiappkpi.Models.User.toDriverInfo
import com.example.taxiappkpi.Models.User.toRiderInfo
import com.example.taxiappkpi.R
import com.example.taxiappkpi.databinding.ActivityRiderMapsBinding
import com.example.taxiappkpi.login.WelcomeActivity
import com.example.taxiappkpi.settings.FiltersForRiderActivity
import com.example.taxiappkpi.settings.SettingsUserActivity
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.EncodedPolyline
import com.google.maps.model.TravelMode
import com.squareup.picasso.Picasso
import java.util.LinkedList


class RiderMapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityRiderMapsBinding
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val SETTINGS_ACTIVITY_CODE = 2
    private val FILTERS_ACTIVITY_CODE = 3

    private var driverMarker: Marker? = null
    private lateinit var callTaxiButton: Button

    private lateinit var lastLocation: Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var polylineOptions: PolylineOptions
    private lateinit var startPoint: LatLng
    private lateinit var finishPoint: LatLng
    private var selectedPointsCount = 0
    private var distanceOfTrip = 0.02f
    private var distanceToDriver = 0.02f
    private var priceOfTrip = 0.0
    private lateinit var routeData: RouteData

    private var radius = 5.0
    private var stopTripConfirmation = 0
    private var tripStatus = -2
    private val orderQueue = LinkedList<String>()
    private var rejectedDrivers = mutableListOf<String>()
    private var startOrder = false
    private var driverFilter: DriverFilter = DriverFilter()

    private lateinit var mAuth: FirebaseAuth
    private lateinit var tripRequestRef: DatabaseReference
    private lateinit var driversAvailableRef: DatabaseReference
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var driversWorkingRef: DatabaseReference
    private lateinit var driversInfoRef: DatabaseReference
    private lateinit var ridersInfoRef: DatabaseReference
    private lateinit var driverAnswerListener: ValueEventListener

    private lateinit var tripTextLayout: LinearLayout
    private lateinit var buttonsPriceLayout: LinearLayout
    private lateinit var driverInfoLayout: LinearLayout
    private lateinit var driverImageRatingLayout: LinearLayout
    private lateinit var carInfoLayout: ConstraintLayout
    private lateinit var setRatingLayout: LinearLayoutCompat

    private lateinit var riderTripPriceTV: TextView
    private lateinit var riderTripDistanceTV: TextView

    private lateinit var stopTripButton: Button

    private var driverFound = false
    private var inSearch = false
    private var carInfoOpen = false
    private var driverID = ""
    private lateinit var riderID: String

    private lateinit var driverInfo: DriverInfo


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRiderMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        riderID = FirebaseAuth.getInstance().currentUser!!.uid

        checkLocationPermission()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationButton = (mapFragment.requireView()
            .findViewById<View>("1".toInt())
            .parent!! as View).findViewById<View>("2".toInt())

        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)

        init()
    }

    private fun init(){
        ridersInfoRef = FirebaseDatabase.getInstance().reference.child(Common.RIDERS_REFERENCE)
        tripRequestRef = FirebaseDatabase.getInstance().reference.child(Common.TRIPS_REQUEST_REFERENCE)
        driversAvailableRef = FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_AVAILABLE_REFERENCE)
        driversWorkingRef = FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_WORKING_REFERENCE)
        driversLocationRef = FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_LOCATION_REFERENCE)
        driversInfoRef = FirebaseDatabase.getInstance().reference.child(Common.DRIVERS_REFERENCE)

        callTaxiButton = findViewById(R.id.callTaxi)

        findViewById<ImageButton>(R.id.rider_settings_button).setOnClickListener {
            val intent = Intent(this@RiderMapsActivity, SettingsUserActivity::class.java)
            intent.putExtra("ROLE", "rider")
            intent.putExtra("IN_TRIP", driverFound)
            startActivityForResult(intent, SETTINGS_ACTIVITY_CODE)
        }

        findViewById<ImageButton>(R.id.rider_filter_button).setOnClickListener {
            startActivityForResult(Intent(this@RiderMapsActivity, FiltersForRiderActivity::class.java).putExtra("driverFilter", driverFilter), FILTERS_ACTIVITY_CODE)
        }

        initTripWidget()

    }

    private fun initTripWidget()
    {
        Log.d("log_check", "initTripWidget запускаем")
        tripTextLayout = findViewById(R.id.tripTextLayout)
        buttonsPriceLayout = findViewById(R.id.buttonsPriceLayout)
        driverInfoLayout = findViewById(R.id.driverTextLayout)
        carInfoLayout = findViewById(R.id.carInfoLayout)
        driverImageRatingLayout = findViewById(R.id.driverImageRating)
        setRatingLayout = findViewById(R.id.riderSetRatingLayout)

        riderTripPriceTV = findViewById(R.id.riderTripPrice)
        riderTripDistanceTV = findViewById(R.id.riderTripDistance)

        val incrPriceB = findViewById<Button>(R.id.incrPrice)
        val decrPriceB = findViewById<Button>(R.id.decrPrice)
        stopTripButton = findViewById(R.id.riderStopTripButton)

        callTaxiButton.setOnClickListener{
            when(tripStatus){
                -2 ->{ //Замовлення без виконавця
                    Log.d("log_check", "нажали вызов такси, трип статус 2")
                    if (selectedPointsCount == 2){
                        Log.d("log_check", "точки выбраны, начинаем")
                        buttonsPriceLayout.visibility = View.GONE
                        stopTripButton.visibility = View.VISIBLE
                        callTaxiButton.isEnabled = false
                        callTaxiButton.text = "Шукаємо водія..."
                        inSearch = true
                        Log.d("log_check", "inSearch, $inSearch")

                        val riderRequest = TripRequest(listOf(startPoint.latitude, startPoint.longitude),
                            listOf(finishPoint.latitude, finishPoint.longitude), priceOfTrip)

                        tripRequestRef.child(riderID).updateChildren(riderRequest.toMap())
                        getNearbyDrivers()
                    }else{
                        Toast.makeText(this@RiderMapsActivity, "Оберіть початкову та кінцеву точки", Toast.LENGTH_SHORT).show()
                    }
                }
                -1,0,1,2 ->{ //Замовлення є, відкрити інфо про авто
                    if(!carInfoOpen){
                        carInfoLayout.visibility = View.VISIBLE
                        callTaxiButton.text = getString(R.string.closeCarInfo)
                        carInfoOpen = true
                    }
                    else{
                        carInfoLayout.visibility = View.GONE
                        callTaxiButton.text = getString(R.string.openCarInfo)
                        carInfoOpen = false
                    }
                }
            }
        }

        incrPriceB.setOnClickListener {
            if(selectedPointsCount==2){
                priceOfTrip+= 5
                riderTripPriceTV.text = getString(R.string.tripPrice, priceOfTrip)
            }
            else{
                Toast.makeText(this@RiderMapsActivity, "Встановіть початкову і кінцеву, щоб змінювати ціну", Toast.LENGTH_SHORT).show()
            }
        }

        decrPriceB.setOnClickListener {
            if(selectedPointsCount==2){
                priceOfTrip-= 5
                riderTripPriceTV.text = getString(R.string.tripPrice, priceOfTrip)
            }
            else{
                Toast.makeText(this@RiderMapsActivity, "Встановіть початкову і кінцеву, щоб змінювати ціну", Toast.LENGTH_SHORT).show()
            }
        }

        stopTripButton.setOnClickListener {
            stopTripConfirmation++
            if(stopTripConfirmation < 2){
                Toast.makeText(this@RiderMapsActivity, "Натисніть ще раз щоб відмінити замовлення", Toast.LENGTH_SHORT).show()
            }else{
                Log.d("log_check", "стоп заказ, чек inSearch $inSearch")

                if(carInfoOpen){
                    carInfoLayout.visibility = View.GONE
                }
                driverInfoLayout.visibility = View.GONE
                driverImageRatingLayout.visibility = View.GONE

                if(!inSearch){
                    Log.d("log_check", "ставим 0 отзыв")
                    ridersInfoRef.child(riderID).addListenerForSingleValueEvent(object :
                        ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val riderInfoMap = snapshot.value as Map<*, *>
                                val riderInfo = riderInfoMap.toRiderInfo()

                                if (riderInfo.userInfo.numTrips != 0) {
                                    riderInfo.userInfo.rating = ((riderInfo.userInfo.rating * riderInfo.userInfo.numTrips) + 0) / (riderInfo.userInfo.numTrips + 1)
                                } else {
                                    riderInfo.userInfo.rating = -0.5
                                }
                                riderInfo.userInfo.numTrips += 1
                                Log.d("log_check", "$riderInfo")
                                ridersInfoRef.child(riderID).updateChildren(riderInfo.toMap())
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
                Log.d("log_check", "обнуляем состояния, удаляем данные, tripRequestRef, тот что driversWorking и наоборот driversAvailableRef тру")
                mutableListOf<String>()
                tripRequestRef.child(riderID).child("status").setValue(4)
                tripStatus = -2
                driverID = ""
                driverFound = false
                inSearch = false
                startOrder = false
                tripTextLayout.visibility = View.GONE
                stopTripButton.visibility = View.GONE
                buttonsPriceLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun initDriverInfoLayout() {
        Log.d("log_check", "инит инфы драйвера")
        callTaxiButton.isEnabled = true
        callTaxiButton.text = getString(R.string.openCarInfo)
        val driverImgIV = findViewById<ImageView>(R.id.driverImage)
        val driverRatingTV = findViewById<TextView>(R.id.driverRating)
        val driverNameTV = findViewById<TextView>(R.id.driverName)
        val driverNumberTV = findViewById<TextView>(R.id.driverNumber)

        driverRatingTV.text = getString(R.string.userRating, driverInfo.userInfo.rating)
        driverNameTV.text = driverInfo.userInfo.firstName
        driverNumberTV.text = driverInfo.userInfo.phoneNumber

        if(driverInfo.userInfo.avatar!=""){
            val uriStr = driverInfo.userInfo.avatar
            Picasso.get().load(uriStr).into(driverImgIV)
        }

        driverInfoLayout.visibility = View.VISIBLE
        driverImageRatingLayout.visibility = View.VISIBLE

        val carImgIV = findViewById<ImageView>(R.id.carImage)
        val carNumberTV = findViewById<TextView>(R.id.carNumber)
        val carBrandTV = findViewById<TextView>(R.id.carBrand)
        val carModelTV = findViewById<TextView>(R.id.carModel)
        val carBodyTV = findViewById<TextView>(R.id.carBody)
        val carColorTV = findViewById<TextView>(R.id.carColor)

        if(driverInfo.carInfo.photo!=""){
            val uriStr = driverInfo.carInfo.photo
            Picasso.get().load(uriStr).into(carImgIV)
        }

        carNumberTV.text = driverInfo.carInfo.number
        carBrandTV.text = getString(R.string.carBrand, driverInfo.carInfo.brand)
        carModelTV.text = getString(R.string.carModel, driverInfo.carInfo.model)
        carBodyTV.text = getString(R.string.carBody, resources.getStringArray(R.array.car_body)[driverInfo.carInfo.bodyType])
        carColorTV.text = getString(R.string.carBody, resources.getStringArray(R.array.car_color)[driverInfo.carInfo.color])

    }


    private fun handleMapClick(latLng: LatLng) {
        Log.d("log_check", "клик по карте")
        if (selectedPointsCount == 2) {
            Log.d("log_check", "кликнули третий раз, очистили карту")
            mMap.clear()
            selectedPointsCount = 0
            callTaxiButton.text= getString(R.string.setPoints)
        }

        selectedPointsCount++

        when (selectedPointsCount) {
            1 -> {
                Log.d("log_check", "первый клик")
                startPoint = latLng
                mMap.addMarker(MarkerOptions().position(startPoint).title("Початкова точка"))
                callTaxiButton.text= getString(R.string.setEndPoint)

                tripTextLayout.visibility = View.GONE
                buttonsPriceLayout.visibility = View.GONE
                riderTripPriceTV.text = getString(R.string.tripPrice, 0.02f)
                riderTripDistanceTV.text = getString(R.string.distanceOfTrip, 0.02f)
            }
            2 -> {
                Log.d("log_check", "второй клик")
                finishPoint = latLng
                mMap.addMarker(MarkerOptions().position(finishPoint).title("Кінцева точка"))
                callTaxiButton.text= getString(R.string.callTaxi)
                getDistanceOfTrip()

                buttonsPriceLayout.visibility = View.VISIBLE
                tripTextLayout.visibility = View.VISIBLE
                riderTripPriceTV.text = getString(R.string.tripPrice, priceOfTrip)
                riderTripDistanceTV.text = getString(R.string.distanceOfTrip, routeData.distance)
            }
        }
    }

    private fun getDistanceOfTrip() {
        polylineOptions = PolylineOptions()
        Log.d("log_check", "расчет дистанций")

        routeData = getRouteData(startPoint, finishPoint)
        Log.d("ответ гугла:", "${routeData}")
        priceOfTrip = (routeData.distance * 12) + 45
        polylineOptions.addAll(routeData.routeJson)

        polylineOptions.width(5f)
        polylineOptions.color(R.color.black)
        mMap.addPolyline(polylineOptions)

    }

    fun getRouteData(origin: LatLng, destination: LatLng): RouteData {
        val apiKey: String = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData.getString("com.google.android.geo.API_KEY")!!

        val context = GeoApiContext.Builder().apiKey(apiKey).build()

        val result: DirectionsResult = DirectionsApi.newRequest(context)
            .mode(TravelMode.DRIVING)
            .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
            .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
            .await()

        val distance = result.routes[0].legs[0].distance.inMeters.toDouble()/1000
        Log.d("ответ гугла:", "${result.routes[0].legs[0].distance.inMeters}")
        Log.d("ответ гугла:", "${distance}")
        val polyline: EncodedPolyline = result.routes[0].overviewPolyline
        val path: MutableList<com.google.maps.model.LatLng> = polyline.decodePath()
        val gmsLatLngList: List<LatLng> = path?.map { latLng ->
            com.google.android.gms.maps.model.LatLng(latLng.lat, latLng.lng)
        } ?: emptyList<com.google.android.gms.maps.model.LatLng>()


        return RouteData(distance, gmsLatLngList.toMutableList())
    }

    private fun getNearbyDrivers() {
        Log.d("log_check", "ищем ближайших драйверов")
        val geoFire = GeoFire(driversLocationRef)
        val geoQuery = geoFire.queryAtLocation(GeoLocation(startPoint.latitude, startPoint.longitude), radius)
        val handler = Handler()
        handler.postDelayed({
            if (!driverFound && orderQueue.isEmpty() && inSearch) {
                processNextDriver(geoQuery)
            }
        }, 30000)
        geoQuery.removeAllListeners()
        geoQuery.addGeoQueryEventListener(object: GeoQueryEventListener{
            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                Log.d("log_check", "Проверим не занят ли водитель другим запросом: $key")
                Log.d("log_check", "$key, $location")
                if (key != null && location != null) {
                    driversAvailableRef.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val isDriverAvailable = snapshot.value
                            Log.d("log_check", "${snapshot.value}")
                            if (isDriverAvailable != null) {
                                Log.d("log_check", "$key доступный, поэтому проверим фильтры")
                                driversInfoRef.child(key).addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val driverInfoMap = snapshot.value!! as Map<*, *>
                                            driverInfo = driverInfoMap.toDriverInfo()

                                            if (driverFilter.userFilter.gender != -1) {
                                                if (driverFilter.userFilter.gender != driverInfo.userInfo.gender) {
                                                    return
                                                }
                                            }
                                            if (driverFilter.userFilter.minAge != -1) {
                                                if (driverFilter.userFilter.minAge > (2024 - driverInfo.userInfo.birthdate.year)) {
                                                    return
                                                }
                                            }

                                            if (driverFilter.userFilter.maxAge != -1) {
                                                if (driverFilter.userFilter.maxAge < (2024 - driverInfo.userInfo.birthdate.year)) {
                                                    return
                                                }
                                            }

                                            if (driverFilter.carFilter.engType != -1) {
                                                if (driverFilter.carFilter.engType != driverInfo.carInfo.engType) {
                                                    return
                                                }
                                            }

                                            if (driverFilter.carFilter.bodyType != -1) {
                                                if (driverFilter.carFilter.bodyType != driverInfo.carInfo.bodyType) {
                                                    return
                                                }
                                            }
                                            Log.d("log_check", "Фильтры одобрены, мы добавим его в очередь")
                                            orderQueue.add(key)
                                            if (orderQueue.size == 1) {
                                                Log.d("log_check", "Обрабатываем очередь")
                                                processNextDriver(geoQuery)
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }}
            override fun onKeyExited(key: String?) {
            }

            override fun onKeyMoved(key: String?, location: GeoLocation?) {
            }

            override fun onGeoQueryReady() {
            }

            override fun onGeoQueryError(error: DatabaseError?) {

            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun processNextDriver(geoQuery: GeoQuery) {
        Log.d("log_check", "Проверка не пустая ли очередь")
        if (orderQueue.isNotEmpty()) {
            driverID = orderQueue.poll()!!
            Log.d("log_check", "$driverID проверим не отказывал ли он еще")
            if (!rejectedDrivers.contains(driverID)) {
                Log.d("log_check", "он не отказывал")
                driverAnswerListener = object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val isActive = snapshot.getValue(Boolean::class.java)
                            val handler = Handler()
                            Log.d("log_check", "мониторим состояние ответа и на всякий ставим хендлер на удаление")
                            handler.postDelayed({
                                if (!driverFound) {
                                    Log.d("log_check", "хендлер на удаление запроса отработал")
                                    driversWorkingRef.child(driverID).removeValue()
                                    driversAvailableRef.child(driverID).setValue(true)
                                }
                            }, 11000)
                            if (isActive != null && isActive) {
                                Log.d("log_check", "водитель согласился, поэтому смотрим его позицию")
                                driverFound = true
                                inSearch = false
                                startOrder = false
                                rejectedDrivers = mutableListOf()
                                getDriverLocation()
                                Log.d("log_check", "также удалим ивент GeoQuery")
                                geoQuery.removeAllListeners()
                                listenTripStatus()
                                initDriverInfoLayout()
                                driversWorkingRef.child(driverID).removeEventListener(driverAnswerListener)
                            }
                        } else {
                            if(tripStatus != 2){
                                Log.d("log_check", "Водитель отказался, поэтому возвращаем ему свободу")
                                driversWorkingRef.child(driverID).removeValue()
                                driversAvailableRef.child(driverID).setValue(true)
                                processNextDriver(geoQuery)
                                rejectedDrivers.add(driverID)
                                Toast.makeText(this@RiderMapsActivity, "Водій $driverID відмовився", Toast.LENGTH_SHORT).show()
                                driverID = ""
                            }

                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                    }
                }

                driversAvailableRef.child(driverID).removeValue()
                driversWorkingRef.child(driverID).child(riderID).setValue(false)
                driversWorkingRef.child(driverID).child(riderID).addValueEventListener(driverAnswerListener)
            }
        }else{
            if (!driverFound) {
                Log.d("log_check", "Водителя не нашли, отмена")
                geoQuery.removeAllListeners()
                Toast.makeText(this@RiderMapsActivity, "Водія не знайдено, спробуйте збільшити ціну", Toast.LENGTH_SHORT).show()
                callTaxiButton.text = getString(R.string.callTaxi)
                callTaxiButton.isEnabled = true
                buttonsPriceLayout.visibility = View.VISIBLE
                stopTripButton.visibility = View.GONE
                tripRequestRef.child(riderID).removeValue()
                inSearch = false
                startOrder = false
                rejectedDrivers = mutableListOf()

            }
        }
    }

    private fun listenTripStatus() {
        Log.d("log_check", "начинаем слушать статус поездки")
        val titleMessage = "Зміна статусу замовлення"
        tripRequestRef.child(riderID).child("status").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    tripStatus = (snapshot.value as? Long)?.toInt() ?: 0
                    when(tripStatus){
                        0 -> {
                            Log.d("log_check", "статус 0, отправляем уведомление")
                            val notificationMessage = "Водій очікує на вас"
                            showNotification(notificationMessage, titleMessage)
                        }
                        1 -> {
                            Log.d("log_check", "статус 1, отправляем уведомление")
                            val notificationMessage = "Водій відмітив що забрав вас"
                            showNotification(notificationMessage, titleMessage)
                            stopTripButton.visibility = View.GONE
                        }
                        2 ->{
                            Log.d("log_check", "статус 2, отправляем уведомление")
                            val notificationMessage = "Поїздка завершена"
                            showNotification(notificationMessage, titleMessage)
                            callTaxiButton.visibility = View.GONE
                            setRatingLayout.visibility = View.VISIBLE
                        }
                        3 -> {
                            Log.d("log_check", "статус 3, водитель отказался")
                            tripRequestRef.child(riderID).removeEventListener(this)
                            tripRequestRef.child(riderID).removeValue()
                            val notificationMessage = "Водій відмінив замовлення\n(Можете надіслати скаргу support@taxikpi.com)"
                            showNotification(notificationMessage, titleMessage)


                            tripRequestRef.child(riderID).removeValue()
                            tripStatus = -2
                            driverID = ""
                            driverFound = false
                            inSearch = false
                            startOrder = false

                            stopTripButton.visibility = View.GONE
                            driverInfoLayout.visibility = View.GONE
                            driverImageRatingLayout.visibility = View.GONE
                            tripTextLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun getDriverLocation() {
        Log.d("log_check", "начинаем слушать позицию водителя")
        driversLocationRef.child(driverID).child("l")
            .addValueEventListener(object: ValueEventListener{
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("log_check", "корды водителя изменились")
                    if(snapshot.exists()) {
                        val snapshotList = snapshot.value as? List<*>
                        var locationLat = 0.0
                        var locationLng = 0.0

                        if (snapshotList != null){
                            locationLat = (snapshotList[0] as? Double) ?: 0.0
                            locationLng = (snapshotList[1] as? Double) ?: 0.0
                        }

                        tripStatus = -1 //Водія знайдено
                        val driverLatLng = LatLng(locationLat, locationLng)
                        driverMarker?.remove()

                        val location1 = Location("")
                        location1.latitude = startPoint.latitude
                        location1.longitude = startPoint.longitude

                        val location2 = Location("")
                        location2.latitude = driverLatLng.latitude
                        location2.longitude = driverLatLng.longitude

                        distanceToDriver = location1.distanceTo(location2) / 1000
                        findViewById<TextView>(R.id.distanceToDriver).text = getString(R.string.distanceToDriver, distanceToDriver)
                        Log.d("log_check", "заадаптейтили это на фронте")
                        driverMarker = mMap.addMarker(
                            MarkerOptions().position(driverLatLng)
                                .title("Таксі")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                        )!!
                    }
                    }
                override fun onCancelled(error: DatabaseError) {
                }

            })
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onConnected(p0: Bundle?) {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).apply {
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
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onLocationChanged(p0: Location) {
        lastLocation = p0
        val latLng = LatLng(p0.latitude, p0.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_ACTIVITY_CODE && resultCode == RESULT_OK) {
            val exit = data!!.getBooleanExtra("EXIT", false)
            if(exit){
                mAuth.signOut()
                logoutRider()
            }
        }

        if (requestCode == FILTERS_ACTIVITY_CODE && resultCode == RESULT_OK){
            driverFilter = data!!.getParcelableExtra("FILTER")!!
        }
    }

    private fun stopTrip() {
        driversWorkingRef.child(driverID).removeValue()
        driversAvailableRef.child(driverID).setValue(true)
        tripRequestRef.child(riderID).removeValue()
        tripStatus = -2
        driverID = ""
        driverFound = false
        inSearch = false
        startOrder = false
        tripTextLayout.visibility = View.GONE
        stopTripButton.visibility = View.GONE
    }


    private fun logoutRider() {
        val welcomeIntent = Intent(this@RiderMapsActivity, WelcomeActivity::class.java)
        startActivity(welcomeIntent)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapClickListener { latLng ->
            if(!driverFound && !inSearch)
                handleMapClick(latLng)
        }
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

    private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        googleApiClient.connect()
    }

    fun onStarClickedRider(view: View) {
        val starIdStr = view.tag as String
        val starId = starIdStr.toInt()
        Log.d("log_check", "$starId")

        if (driverInfo.userInfo.rating!= 0.0){
            driverInfo.userInfo.rating = ((driverInfo.userInfo.rating * driverInfo.userInfo.numTrips) + starId) / (driverInfo.userInfo.numTrips + 1)
        }else{
            driverInfo.userInfo.rating = starId.toDouble()
        }
        driverInfo.userInfo.numTrips += 1

        tripRequestRef.child(riderID).removeValue()

        setRatingLayout.visibility = View.GONE
        tripTextLayout.visibility = View.GONE
        driverInfoLayout.visibility = View.GONE
        stopTripButton.visibility = View.GONE
        mMap.clear()
        selectedPointsCount = 0
        callTaxiButton.text= getString(R.string.setPoints)

        stopTrip()
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

}