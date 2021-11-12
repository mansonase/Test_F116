package com.example.testf116

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_header.*
import kotlinx.android.synthetic.main.fragment_barcode.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),View.OnClickListener {

    private var adapter:BluetoothAdapter?=null
    private var isScanning:Boolean=false
    private var deviceArrayList:ArrayList<BluetoothDevice>?=null
    private var rssiArrayList:ArrayList<Int>?=null
    private var scanner:BluetoothLeScanner?=null
    private var toolbar:Toolbar?=null


    private var deviceName=""
    private var rssi=0
    private var mBluetoothLeService:BluetoothLeService?=null
    private var characteristic:BluetoothGattCharacteristic?=null
    private var isConnected=false

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var strOrderSerialOne:String
    private lateinit var strOrderSerialTwo:String
    private lateinit var strLotNumber:String
    private lateinit var strFirmwareVersion:String
    private lateinit var strTagNumber:String
    private var strAddress=""
    private lateinit var strRssiSmall:String
    private lateinit var strRssiLarge:String
    private lateinit var strProducingTime:String
    private var intTestDepartment=0

    private lateinit var calendar:Calendar

    private var isFirmwarePass=false
    private var isTagPass=false
    private var isRssiPass=false
    private var isCurrentPass=false
    private var isVoltagePass=false
    private var isWattPass=false
    private var isPFPass=false
    private var isLED1Pass:Boolean?=null
    private var isLED2Pass:Boolean?=null
    private var isLED3Pass:Boolean?=null
    private var isLED4Pass:Boolean?=null
    private var isResultPass=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceArrayList=ArrayList()
        rssiArrayList= ArrayList()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            finish()
        }
        val manager=getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        adapter=manager.adapter

        if (adapter==null){
            finish()
            return
        }

        val serviceIntent=Intent(this,BluetoothLeService::class.java)
        bindService(serviceIntent,mServiceConnection, BIND_AUTO_CREATE)


        calendar=Calendar.getInstance()

        setToolbar()
        initView()
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()

        registerReceiver(mReceiver, makeGattUpdateIntentFilter())

        if (adapter?.isEnabled == true){
            if (getPermissionsBLE()){
                if (scanner==null){
                    scanner=adapter?.bluetoothLeScanner
                }
                Log.d("showmac","before scanLeDevice")
                scanLeDevice(true)
            }
        }else{
            adapter?.enable()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService=null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        reHideNavigationBar(hasFocus)
    }


    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_connection->{
                if (isConnected){

                    mBluetoothLeService?.disconnect()
                }else{

                    mBluetoothLeService?.connect(strAddress)
                }
                progressbar.visibility=View.VISIBLE
            }
            R.id.btn_test->{
                doCharacteristic()
                Log.d("btnTest","test btn")
            }
            R.id.btn_save->{
                Log.d("btnTest","save btn")
                resetAfterSetup()
            }
            R.id.fail_led_1->{
                isLED1Pass=false
                show_led_1.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                checkAllPass()
            }
            R.id.fail_led_2->{
                isLED2Pass=false
                show_led_2.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                checkAllPass()
            }
            R.id.fail_led_3->{
                isLED3Pass=false
                show_led_3.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                checkAllPass()
            }
            R.id.fail_led_4->{
                isLED4Pass=false
                show_led_4.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                checkAllPass()
            }
            R.id.pass_led_1->{
                isLED1Pass=true
                show_led_1.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                checkAllPass()
            }
            R.id.pass_led_2->{
                isLED2Pass=true
                show_led_2.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                checkAllPass()
            }
            R.id.pass_led_3->{
                isLED3Pass=true
                show_led_3.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                checkAllPass()
            }
            R.id.pass_led_4->{
                isLED4Pass=true
                show_led_4.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                checkAllPass()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu==null)
            return true

        val drawAdd= getDrawable(R.drawable.ic_baseline_add_24) ?: return true
        val drawForward=getDrawable(R.drawable.ic_baseline_forward_to_inbox_24)?:return true
        val drawRenew=getDrawable(R.drawable.ic_baseline_autorenew_24)?:return true

        menu.add(0,0,0, menuIconWithText(drawAdd,getString(R.string.header)))
        menu.add(0,1,1,menuIconWithText(drawForward,getString(R.string.export)))
        menu.add(0,2,2,menuIconWithText(drawRenew,getString(R.string.renew))).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)


        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        reHideNavigationBar(true)
        val id=item.itemId
        if (id==0){
            createDialogHeader()
            return true
        }else if (id==1){
            createDialogExport()
            return true
        }else if (id==2){

            //toolbar?.setTitleTextColor(Color.WHITE)
            //supportActionBar?.setTitle(R.string.app_name)

                toolbar?.setTitle(R.string.app_name)
            scanLeDevice(true)
            Toast.makeText(this,"hihihi",Toast.LENGTH_SHORT).show()

        }
        return super.onOptionsItemSelected(item)
    }

    private fun makeGattUpdateIntentFilter():IntentFilter{
        val intentFilter=IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        return intentFilter
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode==1){
            if (grantResults.isNotEmpty()&&grantResults[0]!=PackageManager.PERMISSION_GRANTED){

                if (!ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

                    val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri:Uri= Uri.fromParts("package",packageName,null)

                    intent.data=uri
                    startActivity(intent)
                }
            }else{
                if (!adapter!!.isEnabled){
                    adapter!!.enable()
                }
            }
        }
    }

    private fun getPermissionsBLE():Boolean{
        val isPermissionGranted:Boolean

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

                AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setTitle(getString(R.string.need_fine_permission))
                        .setMessage(getString(R.string.need_fine_permission_to_use_ble))
                        .setPositiveButton(getString(R.string.ok_i_know))
                        { _, _ ->
                            ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    1
                            )
                        }.show()
            }else{
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1
                )
            }
            isPermissionGranted=false
        }else{
            isPermissionGranted=true
        }
        return isPermissionGranted
    }

    private fun setToolbar(){
        toolbar=findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)
        toolbar?.setTitleTextColor(Color.WHITE)
        toolbar?.inflateMenu(R.menu.menu_main)
        toolbar?.overflowIcon?.setTint(Color.WHITE)

        toolbar?.setOnClickListener {
            createDialogBarcode()
        }
        Log.d("showmac","settoolbar done")
    }
    private fun initView(){

        sharedPreferences=getSharedPreferences("f116_db", MODE_PRIVATE)

        btn_connection.setOnClickListener(this)
        btn_test.setOnClickListener(this)
        btn_save.setOnClickListener(this)

        fail_led_1.setOnClickListener(this)
        fail_led_2.setOnClickListener(this)
        fail_led_3.setOnClickListener(this)
        fail_led_4.setOnClickListener(this)
        pass_led_1.setOnClickListener(this)
        pass_led_2.setOnClickListener(this)
        pass_led_3.setOnClickListener(this)
        pass_led_4.setOnClickListener(this)

        if (isConnected){
            lower_cover_main.visibility=View.GONE
            setButtonClickable(true)
        }else{
            lower_cover_main.visibility=View.VISIBLE
            setButtonClickable(false)
        }

        if (strAddress.isEmpty()){
            upper_cover_main.visibility=View.VISIBLE
            btn_connection.isClickable=false
        }else{
            upper_cover_main.visibility=View.GONE
            btn_connection.isClickable=true
        }
    }

    private fun initStandard(){
        strOrderSerialOne=sharedPreferences.getString(GattAttributes.ORDER_SERIAL_1,"0000").toString()
        strOrderSerialTwo=sharedPreferences.getString(GattAttributes.ORDER_SERIAL_2,"00000").toString()
        strLotNumber=sharedPreferences.getString(GattAttributes.LOT_NUMBER,"0").toString()
        strFirmwareVersion=sharedPreferences.getString(GattAttributes.FIRMWARE,"0").toString()
        strTagNumber=sharedPreferences.getString(GattAttributes.TAG,"0000").toString()
        strRssiSmall=sharedPreferences.getString(GattAttributes.RSSI_SMALL,"0").toString()
        strRssiLarge=sharedPreferences.getString(GattAttributes.RSSI_LARGE,"0").toString()
        strProducingTime=sharedPreferences.getString(GattAttributes.PRODUCING_TIME,"0").toString()
        intTestDepartment=sharedPreferences.getInt(GattAttributes.TEST_DEP,-1)
    }

    private fun menuIconWithText(drawable: Drawable, title: String): CharSequence {
        drawable.setBounds(0,0,drawable.intrinsicWidth,drawable.intrinsicHeight)
        val sb=SpannableString("   $title")
        val imageSpan=ImageSpan(drawable,ImageSpan.ALIGN_BOTTOM)
        sb.setSpan(imageSpan,0,1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun createDialogHeader() {

        initStandard()
        val dialog=Dialog(this)
        dialog.setContentView(R.layout.dialog_header)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val lps=WindowManager.LayoutParams()
        lps.copyFrom(dialog.window?.attributes)
        lps.width=WindowManager.LayoutParams.MATCH_PARENT
        //lps.height=((resources.displayMetrics.heightPixels)*0.6).toInt()
        lps.height=WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes=lps

        dialog.order_serial_1.hint=strOrderSerialOne
        dialog.order_serial_2.hint=strOrderSerialTwo
        dialog.order_serial_lot.hint=strLotNumber
        dialog.firmware_number.hint=strFirmwareVersion
        dialog.tag_number.hint=strTagNumber
        dialog.rssi_small.hint=strRssiSmall
        dialog.rssi_large.hint=strRssiLarge
        dialog.producing_time.text=calendarToText(calendar)

        val spinnerAdapter=ArrayAdapter.createFromResource(this,R.array.test_dep,android.R.layout.simple_dropdown_item_1line)
        dialog.testing_department.adapter=spinnerAdapter
        dialog.testing_department.onItemSelectedListener= object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
               intTestDepartment=position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        dialog.testing_department.setSelection(intTestDepartment)

        dialog.cancel_button.setOnClickListener {
            dialog.dismiss()
        }

        dialog.save_button.setOnClickListener {


            strOrderSerialOne=if(dialog.order_serial_1.text.isNotEmpty()){
                dialog.order_serial_1.text.toString()
            }else{
                dialog.order_serial_1.hint.toString()
            }
            strOrderSerialTwo=if (dialog.order_serial_2.text.isNotEmpty()){
                dialog.order_serial_2.text.toString()
            }else{
                dialog.order_serial_2.hint.toString()
            }
            strLotNumber=if (dialog.order_serial_lot.text.isNotEmpty()){
                dialog.order_serial_lot.text.toString()
            }else{
                dialog.order_serial_lot.hint.toString()
            }
            strFirmwareVersion=if (dialog.firmware_number.text.isNotEmpty()){
                dialog.firmware_number.text.toString()
            }else{
                dialog.firmware_number.hint.toString()
            }
            strTagNumber=if (dialog.tag_number.text.isNotEmpty()){
                dialog.tag_number.text.toString()
            }else{
                dialog.tag_number.hint.toString()
            }
            strRssiSmall=if (dialog.rssi_small.text.isNotEmpty()){
                dialog.rssi_small.text.toString()
            }else{
                dialog.rssi_small.hint.toString()
            }
            strRssiLarge=if (dialog.rssi_large.text.isNotEmpty()){
                dialog.rssi_large.text.toString()
            }else{
                dialog.rssi_large.hint.toString()
            }

            val small=(0-strRssiSmall.toInt())
            val large=(0-strRssiLarge.toInt())
            if (small>large){

                val alertDialog=AlertDialog.Builder(this)
                        .setTitle(R.string.alert)
                        .setMessage(R.string.not_in_range)
                        .setPositiveButton(R.string.ok){dialog,which->
                            dialog.dismiss()
                        }.create()

                alertDialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                alertDialog.show()
                alertDialog.window?.decorView?.systemUiVisibility=this.window?.decorView?.systemUiVisibility!!
                alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

                return@setOnClickListener
            }


            sharedPreferences.edit()
                    .putString(GattAttributes.ORDER_SERIAL_1,strOrderSerialOne)
                    .putString(GattAttributes.ORDER_SERIAL_2,strOrderSerialTwo)
                    .putString(GattAttributes.LOT_NUMBER,strLotNumber)
                    .putString(GattAttributes.FIRMWARE,strFirmwareVersion)
                    .putString(GattAttributes.TAG,strTagNumber)
                    .putString(GattAttributes.DEVICE_ADDRESS,strAddress)
                    .putString(GattAttributes.RSSI_SMALL,strRssiSmall)
                    .putString(GattAttributes.RSSI_LARGE,strRssiLarge)
                    .putString(GattAttributes.PRODUCING_TIME,strProducingTime)
                    .putInt(GattAttributes.TEST_DEP,intTestDepartment)
                    .apply()
            dialog.dismiss()
            resetAfterSetup()
        }

        dialog.producing_time.setOnClickListener {

            val datePickerDialog=DatePickerDialog(this,
                    { view, year, month, dayOfMonth ->

                        calendar.set(year,month,dayOfMonth)
                        dialog.producing_time.text=calendarToText(calendar)
                    },
                    calendar[Calendar.YEAR],
                    calendar[Calendar.MONTH],
                    calendar[Calendar.DAY_OF_MONTH])

            datePickerDialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            datePickerDialog.show()
            datePickerDialog.window?.decorView?.systemUiVisibility=this.window?.decorView?.systemUiVisibility!!
            datePickerDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        }


        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog.show()
        dialog.window?.decorView?.systemUiVisibility=this.window?.decorView?.systemUiVisibility!!
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }
    private fun createDialogExport() {
        val dialog=Dialog(this)
        dialog.setContentView(R.layout.dialog_export)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val lps=WindowManager.LayoutParams()
        lps.copyFrom(dialog.window?.attributes)
        lps.width=WindowManager.LayoutParams.MATCH_PARENT
        lps.height=WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes=lps
        dialog.cancel_button.setOnClickListener {
            dialog.dismiss()
        }
        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog.show()
        dialog.window?.decorView?.systemUiVisibility=this.window?.decorView?.systemUiVisibility!!
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    private fun createDialogBarcode(){
        val dialog=Dialog(this)
        dialog.setContentView(R.layout.fragment_barcode)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)

        val lps=WindowManager.LayoutParams()
        lps.copyFrom(dialog.window?.attributes)
        lps.width=WindowManager.LayoutParams.MATCH_PARENT
        lps.height=WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes=lps
        dialog.cancel_show_rssi.setOnClickListener {
            dialog.dismiss()
        }

        if (strAddress.isNotEmpty()) {
            dialog.barcode_mac.setImageBitmap(
                codeAsBitmap(
                    strAddress,
                    BarcodeFormat.CODE_128,
                    900,
                    200
                )
            )
            dialog.text_mac.text=strAddress
        }

        if (deviceName.isNotEmpty()) {
            dialog.barcode_name.setImageBitmap(
                codeAsBitmap(
                    deviceName,
                    BarcodeFormat.CODE_128,
                    900,
                    200
                )
            )
            dialog.text_name.text=deviceName
        }

        if (rssi<0){
            val rssi_text="Rssi : $rssi dBM"
            dialog.content_rssi.text = rssi_text
        }

        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog.show()
        dialog.window?.decorView?.systemUiVisibility=this.window?.decorView?.systemUiVisibility!!
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

    }

    private fun hideNavigationBar(){
        window.decorView.systemUiVisibility= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private fun reHideNavigationBar(isFocus:Boolean){
        if (isFocus){
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun scanLeDevice(enable:Boolean){

        if (scanner==null){
            scanner=adapter!!.bluetoothLeScanner
        }

        if (enable){

            Handler(Looper.getMainLooper()).postDelayed({
                isScanning=false
                scanner?.stopScan(mLeScanCallback)
            },5000)

            isScanning=true
            scanner?.startScan(mLeScanCallback)
        }else{

            isScanning=false
            scanner?.stopScan(mLeScanCallback)
        }
    }


    private val mLeScanCallback:ScanCallback= object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result==null)return

            if (result.device==null)return

            if (result.device.name==null)return

            if (result.device.name.substring(0,6).trim()!="eCloud")return

            scanLeDevice(false)

            Log.d("testF116","address ${result.device.address}, name: ${result.device.name}, rssi ${result.rssi}")

            //supportActionBar?.title = address
            //toolbar?.setTitleTextColor(Color.WHITE)

            strAddress=result.device.address
            deviceName=result.device.name
            rssi=result.rssi

            toolbar?.title=strAddress


            upper_cover_main.visibility=View.GONE
            btn_connection.isClickable=true
            return
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    private val mReceiver= object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            val action:String=p1?.action?:return

            if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED==action){
                val state=p1.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1)
                when(state){
                    BluetoothAdapter.STATE_OFF->{
                        Log.d("testF116", "state off")
                    }
                    BluetoothAdapter.STATE_ON->{
                        if (scanner==null){
                            scanner=adapter?.bluetoothLeScanner
                        }
                        scanLeDevice(true)
                    }
                }
            }else if (BluetoothLeService.ACTION_GATT_CONNECTED==action){


            }else if (BluetoothLeService.ACTION_GATT_DISCONNECTED==action){
                isConnected=false
                btn_connection.setText(R.string.disconnected)
                btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                lower_cover_main.visibility=View.VISIBLE
                setButtonClickable(false)
                resetAfterSetup()
                progressbar.visibility=View.GONE
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED==action){

                isConnected=true
                btn_connection.setText(R.string.connected)
                btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                lower_cover_main.visibility=View.GONE
                setButtonClickable(true)
                progressbar.visibility=View.GONE
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE==action){
                doTest(p1)
            }
        }
    }

    private val mServiceConnection= object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mBluetoothLeService=(service as BluetoothLeService.LocalBinder).getService()
            if (mBluetoothLeService!=null){
                if (!mBluetoothLeService!!.initialize()){
                    finish()
                }
                //mBluetoothLeService!!.connect(address)
                Log.d("testf116","bingo service up")
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBluetoothLeService=null
        }
    }

    private fun codeAsBitmap(content:String, format:BarcodeFormat, desiredWidth:Int, desiredHeight:Int): Bitmap?{
        if (content.isEmpty()) return null

        val encoder=BarcodeEncoder()
        val bitmap=encoder.encodeBitmap(content,format,desiredWidth,desiredHeight)


        return bitmap
    }

    private fun calendarToText(cal:Calendar):String{

        val format=SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        return format.format(cal.time)
    }

    private fun doCharacteristic(){

        Thread{

            runOnUiThread {
                progressbar.visibility=View.VISIBLE
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_INFORMATION)as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.firmware_revision_string))
                if (characteristic!=null){
                    mBluetoothLeService?.readCharacteristic(characteristic!!)
                }
            }
            Thread.sleep(500)

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL)as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.nfc_tag_id))
                if (characteristic!=null){
                    mBluetoothLeService?.readCharacteristic(characteristic!!)
                }
            }
            Thread.sleep(500)

            runOnUiThread {
                mBluetoothLeService?.readRemoteRssii()
            }
            Thread.sleep(500)

            runOnUiThread{
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL)as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.read_recorded_data))
                if (characteristic!=null){
                    mBluetoothLeService?.readCharacteristic(characteristic!!)
                }
                progressbar.visibility=View.GONE
            }
        }.start()

    }
    private fun doTest(intent: Intent){

        initStandard()

        when(intent.getStringExtra(BluetoothLeService.CHARACTERISTIC)){

            GattAttributes.mFirmwareRevision->{
                val deviceFirmwareVersion=if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA)==null){
                    "0"
                }else{
                    intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                }

                isFirmwarePass= deviceFirmwareVersion == strFirmwareVersion

                text_firmware.text=deviceFirmwareVersion

                if (isFirmwarePass){
                    show_firmware.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_firmware.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_firmware.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_firmware.setTextColor(Color.RED)
                }
            }
            GattAttributes.mNfcTagId->{
                val deviceNfcTag=if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA)==null){
                    "0000"
                }else{
                    intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                }
                isTagPass=deviceNfcTag==strTagNumber

                text_tag.text=deviceNfcTag

                if (isTagPass){
                    show_tag.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_tag.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_tag.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_tag.setTextColor(Color.RED)
                }
            }
            GattAttributes.mRssi->{
                val deviceRssi=intent.getIntExtra(BluetoothLeService.EXTRA_DATA,0)
                val rssiSmall=(0-strRssiSmall.toInt())
                val rssiLarge=(0-strRssiLarge.toInt())

                isRssiPass= (deviceRssi in rssiSmall .. rssiLarge)

                text_rssi.text=deviceRssi.toString()

                if (isRssiPass){
                    show_rssi.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_rssi.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_rssi.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_rssi.setTextColor(Color.RED)
                }
            }
            GattAttributes.mReadRecordedData->{
                val array=intent.getStringArrayListExtra(BluetoothLeService.EXTRA_DATA)
                if (array!=null){
                    val deviceCurrent=array[2]
                    val deviceVoltage=array[3]
                    val deviceWatt=array[4]
                    val devicePowerFactor=array[5]
                    val deviceConsumption=array[6]

                    isCurrentPass=(deviceCurrent.toFloat() in GattAttributes.CURRENT_LOW .. GattAttributes.CURRENT_HIGH)

                    text_current.text=deviceCurrent

                    if (isCurrentPass){
                        show_current.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_current.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_current.setTextColor(Color.RED)
                    }

                    isVoltagePass=(deviceVoltage.toFloat() in GattAttributes.VOLTAGE_LOW .. GattAttributes.VOLTAGE_HIGH)

                    text_voltage.text=deviceVoltage

                    if (isVoltagePass){
                        show_voltage.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_voltage.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_voltage.setTextColor(Color.RED)
                    }

                    isWattPass=(deviceWatt.toFloat() in GattAttributes.WATT_LOW .. GattAttributes.WATT_HIGH)

                    text_watt.text=deviceWatt

                    if (isWattPass){
                        show_watt.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_watt.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_watt.setTextColor(Color.RED)
                    }

                    isPFPass=(devicePowerFactor.toFloat() in GattAttributes.PF_LOW .. GattAttributes.PF_HIGH)

                    text_power_factor.text=devicePowerFactor

                    if (isPFPass){
                        show_power_factor.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_power_factor.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_power_factor.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_power_factor.setTextColor(Color.RED)
                    }
                    text_wh.text=deviceConsumption
                }
            }
        }
        checkAllPass()
    }
    private fun resetAfterSetup(){

        show_firmware.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_tag.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_rssi.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_power_factor.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_1.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_2.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_3.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_4.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)

        text_firmware.setText(R.string.na)
        text_firmware.setTextColor(Color.parseColor("#050505"))
        text_tag.setText(R.string.na)
        text_tag.setTextColor(Color.parseColor("#050505"))
        text_rssi.setText(R.string.na)
        text_rssi.setTextColor(Color.parseColor("#050505"))
        text_current.setText(R.string.na)
        text_current.setTextColor(Color.parseColor("#050505"))
        text_voltage.setText(R.string.na)
        text_voltage.setTextColor(Color.parseColor("#050505"))
        text_watt.setText(R.string.na)
        text_watt.setTextColor(Color.parseColor("#050505"))
        text_power_factor.setText(R.string.na)
        text_power_factor.setTextColor(Color.parseColor("#050505"))

        text_result.setText(R.string.na)
        text_result.setTextColor(Color.parseColor("#050505"))

        isFirmwarePass=false
        isTagPass=false
        isRssiPass=false
        isCurrentPass=false
        isVoltagePass=false
        isWattPass=false
        isPFPass=false

        isLED1Pass=null
        isLED2Pass=null
        isLED3Pass=null
        isLED4Pass=null

    }
    private fun setButtonClickable(clickable:Boolean){
        fail_led_1.isClickable=clickable
        fail_led_2.isClickable=clickable
        fail_led_3.isClickable=clickable
        fail_led_4.isClickable=clickable
        pass_led_1.isClickable=clickable
        pass_led_2.isClickable=clickable
        pass_led_3.isClickable=clickable
        pass_led_4.isClickable=clickable
        btn_test.isClickable=clickable
        btn_save.isClickable=clickable
    }
    private fun checkAllPass(){

        if (isLED1Pass!=null&&isLED2Pass!=null&&isLED3Pass!=null&&isLED4Pass!=null){

            if (isLED1Pass!!&&isLED2Pass!!&&isLED3Pass!!&&isLED4Pass!!&&isFirmwarePass&&isTagPass&&isRssiPass&&isCurrentPass&&isVoltagePass&&isWattPass&&isPFPass){

                isResultPass=true
                text_result.setTextColor(Color.GREEN)
                text_result.setText(R.string.pass)
            }else{

                isResultPass=false
                text_result.setTextColor(Color.RED)
                text_result.setText(R.string.fail)
            }
        }
    }
}