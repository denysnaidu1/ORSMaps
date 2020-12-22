package com.example.mapsactivity

import android.app.Activity
import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.location.LocationDataSource
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.symbology.TextSymbol
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters
import com.example.mapsactivity.adapter.MapsSVAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_content.*
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var mLocationDisplay:LocationDisplay
    private var mSearchView:SearchView?=null
    private lateinit var mGraphicsOverlay:GraphicsOverlay
    private var mLocatorTask: LocatorTask?=null
    private var mGeocodeParameters: GeocodeParameters?=null
    private var graphicsOverlay = GraphicsOverlay()
    private var markerCoor : Point?=null
    private var myLocation:Point?=null
    private lateinit var botSheetBehavior:BottomSheetBehavior<ConstraintLayout>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Codes to hide status bar (to make fullscreen)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        actionBar?.hide()
        setContentView(R.layout.activity_main)
        progressDialog.setMessage("Preparing maps..\n This process needs internet connection.");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        setupMap()
        setupLocator()
        setupLocationDisplay()
        mapView.graphicsOverlays.add(graphicsOverlay)
        mapView.onTouchListener=object: DefaultMapViewOnTouchListener(this, mapView) {
            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                val mapPoint=android.graphics.Point(e!!.x.roundToInt(), e.y.roundToInt())
                val mapPoint2=mapView.screenToLocation(mapPoint)
                //Turn screen point into maps point
                //Coord x as Longitude & Coord y as Latitude
                markerCoor=GeometryEngine.project(mapPoint2, SpatialReferences.getWgs84()) as Point
                Log.d("MapsCoor", markerCoor!!.x.toString() + ',' + markerCoor!!.y.toString())
                val picSymbol=ContextCompat.getDrawable(this@MainActivity, R.drawable.placeholder) as BitmapDrawable
                //val pinUri:Uri = Uri.parse(R.drawable.ic_maps_pin.toString())
                //Log.d("UriTest",pinUri.toString())
                val mapsPin=PictureMarkerSymbol(picSymbol)
                mapsPin.height=40F
                mapsPin.width=40F
                mapsPin.offsetY=11F
                mapsPin.loadAsync()
                val graphic=Graphic(markerCoor, mapsPin)
                graphicsOverlay.graphics.clear()
                graphicsOverlay.graphics.add(graphic)
                reGeocode(markerCoor!!)

            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                if (markerCoor!=null){
                    val mapPoint=android.graphics.Point(e!!.x.roundToInt(), e.y.roundToInt())
                    val mapPoint2=mapView.screenToLocation(mapPoint)
                    val tempPoint=GeometryEngine.project(mapPoint2, SpatialReferences.getWgs84()) as Point
                    val diffX=abs(tempPoint.x - markerCoor!!.x)
                    val diffY= abs(tempPoint.y - markerCoor!!.y)
                    Log.d("PositionAwal", "${markerCoor!!.x}&${markerCoor!!.y}")
                    Log.d("PositionBary", "${tempPoint.x}&${tempPoint.y}")
                    Log.d("Diff", "$diffX&$diffY")
                    Log.d(
                        "ZoomLevel",
                        mapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).targetScale.toString()
                    )
                    Log.d(
                        "ZoomLevel",
                        mapView.getCurrentViewpoint(
                            Viewpoint.Type.values().get(0)
                        ).targetScale.toString()
                    )
                    val zoomLevel=mapView.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE).targetScale.toInt()
                    if(zoomLevel<=2000){
                        if(diffX<0.00008 && diffY<0.00008){
                            reGeocode(markerCoor!!)
                        }

                    }
                    else if(zoomLevel<=3000){
                        if(diffX<0.0004 && diffY<0.0001){
                            reGeocode(markerCoor!!)
                        }
                    }
                    else if(zoomLevel<=5000){
                        if(diffX<0.0003 && diffY<0.0002){
                            reGeocode(markerCoor!!)
                        }
                    }
                    else if(zoomLevel<=10000){
                        if(diffX<0.0003 && diffY<0.0005){
                            reGeocode(markerCoor!!)
                        }
                    }
                    else if(zoomLevel<=15000){
                        if(diffX<0.0008 && diffY<0.0005){
                            reGeocode(markerCoor!!)
                        }
                    }
                    else if(zoomLevel<=20000){
                        if(diffX<0.0008 && diffY<0.0008){
                            reGeocode(markerCoor!!)
                        }
                    }

                }
                return super.onSingleTapUp(e)
            }
        }
        botSheetBehavior=BottomSheetBehavior.from(sheetContent)
        botSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

        })
        containerData.setOnClickListener {
            if(botSheetBehavior.state==BottomSheetBehavior.STATE_EXPANDED){
                botSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
            }
            else{
                botSheetBehavior.state=BottomSheetBehavior.STATE_EXPANDED
            }
        }

        svLocation.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                queryLocator(query!!,"submit")
                adapter?.destroy()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText!!.isNotBlank() && newText.length>=3){
                    checker=true
                    queryLocator(newText,"change")
                }

                if(newText.isBlank()){
                    checker=false
                    adapter?.destroy()
                }
                return true
            }

        })


        fab2.setOnClickListener{
            mapView.setViewpointCenterAsync(myLocation)
        }
    }

    private var checker = true
    private fun reGeocode(markerPoint: Point){
        val reverseGeocodeParameters=ReverseGeocodeParameters()
        reverseGeocodeParameters.outputSpatialReference=mapView.spatialReference
        val results:ListenableFuture<List<GeocodeResult>> = mLocatorTask!!.reverseGeocodeAsync(
            markerPoint,
            reverseGeocodeParameters
        )

        results.addDoneListener{
            try{
                val geocodes : List<GeocodeResult> = results.get()
                if(geocodes.isNotEmpty()){
                    Log.d("tvSize",tv_streetName.textSize.toString())
                    tv_streetName.textSize=16F
                    tv_streetName.setTextColor(-1979711488)
                    val geocodeResult=geocodes[0]
                    val address= geocodeResult.attributes["Match_addr"].toString().split(',')
                    if(address.size==4){
                        tv_streetName.text=address[0]
                        tv_countyName.text=address[1]+", "+address[3]
                        tv_provinceName.text=address[2]

                    }
                    else if(address.size==2 || address.size==3){
                        tv_streetName.text=address[0]
                        tv_countyName.text=address[1]
                        tv_provinceName.text=""
                    }
                    else if(address.size==1){
                        tv_streetName.text=address[0]
                        tv_provinceName.text=""
                        tv_countyName.text=""
                    }else{
                        tv_streetName.text="Address not found! Yet you still able to pin this location if you want."
                        Log.d("SizeText",tv_streetName.textSize.toString())
                        tv_streetName.textSize=16F
                        tv_streetName.setTextColor(Color.RED)
                        tv_provinceName.text=""
                        tv_countyName.text=""
                    }
                    botSheetBehavior.state=BottomSheetBehavior.STATE_EXPANDED
                }
            } catch (e: Exception){
                e.printStackTrace()
                tv_streetName.text="Error getting street name. Please check your connection"
                tv_streetName.textSize=16F
                tv_streetName.setTextColor(Color.RED)
                tv_provinceName.text=""
                tv_countyName.text=""
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater=menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        val searchMenuItem=menu?.findItem(R.id.search)
        if(searchMenuItem!=null){
            mSearchView=searchMenuItem.actionView as SearchView
            if(mSearchView!=null){
                val searchManager=getSystemService(Context.SEARCH_SERVICE) as SearchManager
                mSearchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
                mSearchView!!.isIconifiedByDefault = false
            }
        }

        return true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if(Intent.ACTION_SEARCH.equals(intent!!.action)){
            /*queryLocator(intent.getStringExtra(svLocation.query.toString()))*/
        }
    }

    private var adapter:MapsSVAdapter?=null
    private fun queryLocator(query: String?,flags:String){
        if(query!=null && query.isNotEmpty()){
            adapter?.destroy()
            mLocatorTask!!.cancelLoad()
            val geocodeFuture:ListenableFuture<List<GeocodeResult>>? = mLocatorTask?.geocodeAsync(
                query,
                mGeocodeParameters
            )
            geocodeFuture?.addDoneListener(object : Runnable {
                override fun run() {
                    try {
                        val geocodeResult = geocodeFuture.get()
                        if (geocodeResult.isNotEmpty() && checker) {
                            if(flags == "change"){
                                adapter=MapsSVAdapter(this@MainActivity,geocodeResult)
                                lv_searchResult.adapter=adapter
                                Log.d("SizeList",geocodeResult.size.toString())
                            }
                            else{
                                displaySearchResult(geocodeResult[0])
                            }
                        } else {
                            if(flags=="submit"){
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.nothing_found),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {

                    }
                    geocodeFuture.removeDoneListener(this)
                }

            })
        }
    }

    fun displaySearchResult(geocodeResult: GeocodeResult){
        val displayLabel=geocodeResult.label
        val textLabel=TextSymbol(
            16f,
            displayLabel,
            Color.rgb(192, 32, 32),
            TextSymbol.HorizontalAlignment.CENTER,
            TextSymbol.VerticalAlignment.BOTTOM
        )
        val textGraphic=Graphic(geocodeResult.displayLocation, textLabel)
        val mapMarker: Graphic = Graphic(
            geocodeResult.displayLocation, geocodeResult.attributes, SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.SQUARE, Color.rgb(
                    255,
                    0,
                    0
                ), 12.0f
            )
        )
        val allGraphic=mGraphicsOverlay.graphics
        allGraphic.clear()
        allGraphic.add(mapMarker)
        allGraphic.add(textGraphic)
        mapView.setViewpointCenterAsync(geocodeResult.displayLocation)
    }

    private fun setupLocator(){
        svLocation.inputType=InputType.TYPE_NULL
        Log.d("InputType",svLocation.inputType.toString())
        svLocation.setOnSearchClickListener {
            Toast.makeText(this@MainActivity,"It's disabled",Toast.LENGTH_SHORT).show()
        }
        svLocation.setOnFocusChangeListener { v, hasFocus ->
            Toast.makeText(this@MainActivity,"It's disabled",Toast.LENGTH_SHORT).show()
        }
        svLocation.setOnClickListener {
            if(svLocation.inputType==0){

            }
        }
        val locatorService="https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer"
        mLocatorTask=LocatorTask(locatorService)
        mLocatorTask?.addDoneLoadingListener {
            if (mLocatorTask!!.loadStatus == LoadStatus.LOADED) {
                mGeocodeParameters = GeocodeParameters()
                mGeocodeParameters!!.resultAttributeNames!!.add("*")
                mGeocodeParameters!!.maxResults = 1
                mGraphicsOverlay = GraphicsOverlay()
                mapView.graphicsOverlays.add(mGraphicsOverlay)
                svLocation.inputType=InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
                Log.d("Locator","Done Loading")
                //progressDialog.dismiss()
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                //or using progress bar
                progressBarMain.visibility=View.GONE

            } else {
                Log.d("Locator", "asdasdasd")
                svLocation.setOnClickListener {
                    Toast.makeText(this@MainActivity, "It's disabled", Toast.LENGTH_SHORT).show()
                }
                mLocatorTask?.retryLoadAsync()
            }
        }
        mLocatorTask?.loadAsync()
    }

    private fun setupLocationDisplay(){
        mLocationDisplay=mapView.locationDisplay
        mLocationDisplay.locationDataSource.addLocationChangedListener { p0 ->
            myLocation = p0?.location?.position
        }
        mLocationDisplay.addDataSourceStatusChangedListener {
            if(it.isStarted || it.error==null) {
                return@addDataSourceStatusChangedListener
            }
        }
        mLocationDisplay.autoPanMode=LocationDisplay.AutoPanMode.NAVIGATION
        mLocationDisplay.startAsync()
    }
    private fun setupMap(){
        if(mapView!=null){
            ArcGISRuntimeEnvironment.setLicense(resources.getString(R.string.arcgis_license_key))
            val basemapType= Basemap.Type.OPEN_STREET_MAP
            val latitude = 3.213133
            val longitude = 98.5633413
            val levelOfDetail=13
            val map=ArcGISMap(basemapType, latitude, longitude, levelOfDetail)
            mapView.setMap(map)
        }
    }

    override fun onPause() {
        mapView?.pause()
        super.onPause()
    }

    override fun onResume() {
        mapView?.resume()
        super.onResume()
    }

    override fun onDestroy() {
        mapView?.dispose()
        super.onDestroy()
    }

}

@Parcelize
data class LocationItem(
    val address:String,
    val details:String
):Parcelable