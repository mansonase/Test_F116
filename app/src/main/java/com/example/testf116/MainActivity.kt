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
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.encoder.QRCode
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.dialog_export.*
import kotlinx.android.synthetic.main.dialog_header.*
import kotlinx.android.synthetic.main.dialog_header.cancel_button
import kotlinx.android.synthetic.main.dialog_scan_result.*
import kotlinx.android.synthetic.main.fragment_barcode.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    private lateinit var mLeDeviceListAdapter:BLEDeviceAdapter


    private var deviceName=""
    private var rssi=0
    private var mBluetoothLeService:BluetoothLeService?=null
    private var characteristic:BluetoothGattCharacteristic?=null
    private var isConnected=false
    private var isPowerActivated=false

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var strOrderSerialOne:String
    private lateinit var strOrderSerialTwo:String
    //private lateinit var strLotNumber:String
    private lateinit var strFirmwareVersion:String
    private lateinit var strTagNumber:String
    private var strAddress=""
    private lateinit var strMeter:String
    private lateinit var strRssiSmall:String
    private lateinit var strRssiLarge:String
    private lateinit var strProducingTime:String
    private var intTestDepartment=0
    private var powerOnEpoch=0L
    private var powerOffEpoch=0L
    private var aa15Epoch=0L

    private lateinit var calendar:Calendar
    private var testCounts=0
    private var serial=0

    private var isFirmwarePass:Boolean?=null
    private var isTagPass:Boolean?=null
    private var isMeterpass:Boolean?=null
    private var isRssiPass:Boolean?=null
    private var isCurrentPass:Boolean?=null
    private var isVoltagePass:Boolean?=null
    private var isWattPass:Boolean?=null
    private var isPFPass:Boolean?=null
    private var isLED1Pass:Boolean?=null
    private var isLED2Pass:Boolean?=null
    private var isLED3Pass:Boolean?=null
    private var isLED4Pass:Boolean?=null
    private var isResultPass:Boolean?=null

    private val lock=Object()
    private var reConnectCount=0
    private var isAllowedSaving=false


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
        initStandard()

/*
        val array=ArrayList<Int>()
        array.add(2)
        array.add(8)
        array.add(1)
        array.sortDescending()

        for (i in 0 until array.size){
            Log.d("smartOrder","${array[i]}")
        }

 */
        if (deviceArrayList!=null&&rssiArrayList!=null) {
            createDialogScanResult()
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()

        registerReceiver(mReceiver, makeGattUpdateIntentFilter())


    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onStop() {
        super.onStop()
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

                    //doDeActivePower()
                    mBluetoothLeService?.disconnect()
                }else{

                    mBluetoothLeService?.connect(strAddress)
                }
                progressbar.visibility=View.VISIBLE
            }
            R.id.btn_test->{
                //progressbar.visibility=View.VISIBLE
                //resetAfterSetup()
                doCharacteristic()
                Log.d("writesu","$reConnectCount, step-5")
                Log.d("btnTest","test btn")
            }
            R.id.btn_save -> {
                Log.d("btnTest", "save btn")

                if (isAllowedSaving) {
                    doSave()
                    resetAfterSetup()
                    savingToast()
                    checkSerial()
                    isAllowedSaving=false
                } else {
                    Toast.makeText(this,getString(R.string.unable_save),Toast.LENGTH_SHORT).show()
                }
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

                //toolbar?.setTitle(R.string.app_name)
            scanLeDevice(true)
            Toast.makeText(this,"hihihi",Toast.LENGTH_SHORT).show()

            createDialogScanResult()
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

        val mManifest:MutableList<String> = mutableListOf()
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){

            mManifest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            mManifest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            mManifest.add(Manifest.permission.BLUETOOTH_SCAN)
            mManifest.add(Manifest.permission.BLUETOOTH_CONNECT)
            mManifest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }else{
            mManifest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            mManifest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val isPermissionGranted:Boolean

        if ((ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)||
                (ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)||
            (ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){

                AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setTitle(getString(R.string.need_fine_permission))
                        .setMessage(getString(R.string.need_fine_permission_to_use_ble))
                        .setPositiveButton(getString(R.string.ok_i_know))
                        { _, _ ->
                            ActivityCompat.requestPermissions(
                                    this,
                                    mManifest.toTypedArray(),
                                    1
                            )
                        }.show()
            }else{
                ActivityCompat.requestPermissions(
                        this,
                        mManifest.toTypedArray(),
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
        //strLotNumber=sharedPreferences.getString(GattAttributes.LOT_NUMBER,"0").toString()
        strFirmwareVersion=sharedPreferences.getString(GattAttributes.FIRMWARE,"0").toString()
        strTagNumber=sharedPreferences.getString(GattAttributes.TAG,"00000000").toString()
        strMeter=sharedPreferences.getString(GattAttributes.METER,"000000").toString()
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
        //dialog.order_serial_lot.hint=strLotNumber
        dialog.firmware_number.hint=strFirmwareVersion
        dialog.tag_number.hint=strTagNumber
        dialog.meter.hint=strMeter
        dialog.rssi_small.hint=strRssiSmall
        dialog.rssi_large.hint=strRssiLarge
        calendar= Calendar.getInstance()
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
            /*
            strLotNumber=if (dialog.order_serial_lot.text.isNotEmpty()){
                dialog.order_serial_lot.text.toString()
            }else{
                dialog.order_serial_lot.hint.toString()
            }
             */
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
            strMeter=if (dialog.meter.text.isNotEmpty()){
                dialog.meter.text.toString()
            }else{
                dialog.meter.hint.toString()
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
                    //.putString(GattAttributes.LOT_NUMBER,strLotNumber)
                    .putString(GattAttributes.FIRMWARE,strFirmwareVersion)
                    .putString(GattAttributes.TAG,strTagNumber)
                    .putString(GattAttributes.DEVICE_ADDRESS,strAddress)
                    .putString(GattAttributes.METER,strMeter)
                    .putString(GattAttributes.RSSI_SMALL,strRssiSmall)
                    .putString(GattAttributes.RSSI_LARGE,strRssiLarge)
                    .putString(GattAttributes.PRODUCING_TIME,strProducingTime)
                    .putInt(GattAttributes.TEST_DEP,intTestDepartment)
                    .apply()
            dialog.dismiss()
            resetAfterSetup()
            initStandard()
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
        initStandard()
        val dialog=Dialog(this)
        dialog.setContentView(R.layout.dialog_export)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val lps=WindowManager.LayoutParams()
        lps.copyFrom(dialog.window?.attributes)
        lps.width=WindowManager.LayoutParams.MATCH_PARENT
        lps.height=WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes=lps
        dialog.export_btn.setOnClickListener {

            makingCSV()
            /*
            val realm=Realm.getDefaultInstance()
            Log.d("showresult","click export btn")
            val result= realm.where(ExamItem::class.java)
                .findAll()
            realm.close()

            if (result==null){
                return@setOnClickListener
            }

            val iteration=result.iterator()
            while (iteration.hasNext()){

                val item=iteration.next()

                Log.d("show_result","${item.aa24Timestamp},${item.tagNumber},${item.voltage}")
            }

             */
        }
        dialog.cancel_button.setOnClickListener {
            dialog.dismiss()
        }

        val realm=Realm.getDefaultInstance()
        val mDevice=realm.where(ExamItem::class.java)
                .equalTo("testNumber",0.toInt())
                .count()
        val mTest=realm.where(ExamItem::class.java)
                .count()
        dialog.total_device.text=("${getString(R.string.total_devices)} $mDevice")
        dialog.total_tests.text=("${getString(R.string.total_tests)} $mTest")

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

            val address=strAddress.split(":")
            var nameAndMac="F100"
            for (i in address.indices){
                nameAndMac+=address[i]
            }

            dialog.barcode_name_mac.setImageBitmap(
                codeAsBitmap(
                    nameAndMac,
                    BarcodeFormat.CODE_128
                ,900,
                    200
                )
            )
            dialog.text_name_mac.text=nameAndMac
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
    private fun createDialogScanResult(){

        val dialog=Dialog(this)
        dialog.setContentView(R.layout.dialog_scan_result)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        val lps=WindowManager.LayoutParams()
        lps.copyFrom(dialog.window?.attributes)
        lps.width=WindowManager.LayoutParams.MATCH_PARENT
        lps.height=WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes=lps

        dialog.cancel_button.setOnClickListener {
            dialog.dismiss()
        }

        mLeDeviceListAdapter = BLEDeviceAdapter(deviceArrayList!!, rssiArrayList!!,dialog)


        val layoutManager=LinearLayoutManager(this)
        layoutManager.orientation=LinearLayoutManager.VERTICAL
        dialog.recyclerview.layoutManager=layoutManager
        dialog.recyclerview.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))
        dialog.recyclerview.adapter=mLeDeviceListAdapter

        dialog.refreshlayout.setColorSchemeColors(resources.getColor(R.color.word_blue))
        dialog.refreshlayout.setOnRefreshListener {
            mLeDeviceListAdapter.clear()
            scanLeDevice(true)
            dialog.refreshlayout.isRefreshing=false
        }

        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog.show()
        dialog.window?.decorView?.systemUiVisibility=this.window?.decorView?.systemUiVisibility!!
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)


        if (adapter?.isEnabled == true){
            if (getPermissionsBLE()){
                if (scanner==null){
                    scanner=adapter?.bluetoothLeScanner
                }
                Log.d("showmac","before scanLeDevice")
                mLeDeviceListAdapter.clear()
                scanLeDevice(true)
            }
        }else{
            adapter?.enable()
        }
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

            //if (result.device.name.substring(0,6).trim()!="eCloud"&&result.device.name.substring(0,4).trim()!="F100")return
            if (!result.device.name.contains("eCloud"))return

            //scanLeDevice(false)

            Log.d("testF1166","address ${result.device.address}, name: ${result.device.name}, rssi ${result.rssi}")

            mLeDeviceListAdapter.addDevice(result.device,result.rssi)
            //mLeDeviceListAdapter.notifyDataSetChanged()

            //supportActionBar?.title = address
            //toolbar?.setTitleTextColor(Color.WHITE)

            /*
            strAddress=result.device.address
            deviceName=result.device.name
            rssi=result.rssi

            toolbar?.title=strAddress


            upper_cover_main.visibility=View.GONE
            btn_connection.isClickable=true
            return

             */
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
                btn_connection.setText(R.string.connected)
                btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                lower_cover_main.visibility=View.VISIBLE
                setButtonClickable(false)
                resetAfterSetup()
                progressbar.visibility=View.GONE
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED==action){
                isConnected=true
                btn_connection.setText(R.string.disconnected)
                btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                lower_cover_main.visibility=View.GONE
                setButtonClickable(true)
                checkSerial()
                progressbar.visibility=View.GONE
                isAllowedSaving=false
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

    private fun doDeActivePower(){
        Thread{

            runOnUiThread {
                val byteArray= byteArrayOf((0x00).toByte())
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL) as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.activate_power))
                if (characteristic!=null){
                    mBluetoothLeService?.writeCharacteristic(characteristic!!,byteArray)
                }
            }
        }.start()
    }

    private fun doRelay(turningOn:Boolean){

        val byteArray=if (turningOn){
            byteArrayOf((0x01).toByte())
        }else{
            byteArrayOf((0x00).toByte())
        }
        characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL)as BluetoothGattService)
            .getCharacteristic(UUID.fromString(GattAttributes.activate_power))
        if (characteristic!=null){
           mBluetoothLeService?.writeCharacteristic(characteristic!!,byteArray)
        }
    }

    private fun doCharacteristic(){

        Thread{
            runOnUiThread {
                progressbar.visibility=View.VISIBLE
                Thread{
                    Thread.sleep(13_000)
                    runOnUiThread {
                        progressbar.visibility=View.GONE
                    }
                }.start()
            }

            while (!isPowerActivated){

                Log.d("writesu","$reConnectCount, step-1")
                if (reConnectCount==3){
                    Log.d("writesu","$reConnectCount, step-2")
                    reConnectCount=0
                    runOnUiThread {
                        progressbar.visibility=View.GONE
                    }
                    return@Thread
                }
                synchronized(lock) {
                    runOnUiThread {
                        doRelay(true)
                    }
                    Log.d("lock","step -1")
                    lock.wait()
                    runOnUiThread {
                        characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL)as BluetoothGattService)
                            .getCharacteristic(UUID.fromString(GattAttributes.activate_power))
                        if (characteristic!=null){
                            mBluetoothLeService?.readCharacteristic(characteristic!!)
                        }
                    }
                    Log.d("lock","step -2")
                    lock.wait()
                }
            }
            //Thread.sleep(1000)
            Log.d("lock","step -3")
            Log.d("writesu","$reConnectCount, step-3")
            runOnUiThread {
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

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.EXTRA_CONTROL)as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.meter_parameter))
                if (characteristic!=null){
                    mBluetoothLeService?.readCharacteristic(characteristic!!)
                }
            }
            Thread.sleep(500)

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.current))
                if (characteristic!=null){
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
                }
            }

            Thread.sleep(500)
            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.voltage))
                if (characteristic!=null){
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
                }
            }
            Thread.sleep(500)

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.watt))
                if (characteristic!=null){
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
                }
            }
            Thread.sleep(500)

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.power_factor))
                if (characteristic!=null){
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
                }
            }
            Thread.sleep(500)

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.load))
                if (characteristic!=null){
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
                }
            }


            Thread.sleep(4000)


            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.current))
                if (characteristic!=null){
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
            }
            Thread.sleep(200)

            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.voltage))
                if (characteristic!=null){
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
            }
            Thread.sleep(200)
            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.watt))
                if (characteristic!=null){
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
            }
            Thread.sleep(200)
            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.power_factor))
                if (characteristic!=null){
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
            }
            Thread.sleep(200)
            runOnUiThread {
                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.load))
                if (characteristic!=null){
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
            }
            Thread.sleep(200)
            synchronized(lock) {
                runOnUiThread {
                    doRelay(false)
                }
                lock.wait()
            }
            Log.d("writesu","$reConnectCount, step-4")

            runOnUiThread {
                calendar=Calendar.getInstance()
                aa15Epoch=calendar.timeInMillis
                checkAllPass()
                progressbar.visibility=View.GONE
            }
        }.start()
        Log.d("writesu","$reConnectCount, step-6")
    }
    private fun doTest(intent: Intent){


        when(intent.getStringExtra(BluetoothLeService.CHARACTERISTIC)){

            GattAttributes.mFirmwareRevision->{
                val deviceFirmwareVersion=if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA)==null){
                    "0"
                }else{
                    intent.getStringExtra(BluetoothLeService.EXTRA_DATA)
                }

                isFirmwarePass= (deviceFirmwareVersion == strFirmwareVersion)

                text_firmware.text=deviceFirmwareVersion

                if (isFirmwarePass!!){
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
                Log.d("tagtag","$deviceNfcTag, $strTagNumber")
                isTagPass=deviceNfcTag==strTagNumber

                text_tag.text=deviceNfcTag

                if (isTagPass!!){
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

                if (isRssiPass!!){
                    show_rssi.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_rssi.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_rssi.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_rssi.setTextColor(Color.RED)
                }
            }
            GattAttributes.mMeterVersion->{
                val deviceMeter=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)

                isMeterpass=(deviceMeter==strMeter)

                text_meter.text=deviceMeter

                if (isMeterpass!!){
                    show_meter.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_meter.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_meter.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_meter.setTextColor(Color.RED)
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

                    if (isCurrentPass!!){
                        show_current.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_current.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_current.setTextColor(Color.RED)
                    }

                    isVoltagePass=(deviceVoltage.toFloat() in GattAttributes.VOLTAGE_LOW .. GattAttributes.VOLTAGE_HIGH)

                    text_voltage.text=deviceVoltage

                    if (isVoltagePass!!){
                        show_voltage.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_voltage.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_voltage.setTextColor(Color.RED)
                    }

                    isWattPass=(deviceWatt.toFloat() in GattAttributes.WATT_LOW .. GattAttributes.WATT_HIGH)

                    text_watt.text=deviceWatt

                    if (isWattPass!!){
                        show_watt.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_watt.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_watt.setTextColor(Color.RED)
                    }

                    isPFPass=(devicePowerFactor.toFloat() in GattAttributes.PF_LOW .. GattAttributes.PF_HIGH)

                    text_power_factor.text=devicePowerFactor

                    if (isPFPass!!){
                        show_power_factor.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                        text_power_factor.setTextColor(Color.parseColor("#050505"))
                    }else{
                        show_power_factor.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                        text_power_factor.setTextColor(Color.RED)
                    }
                    text_wh.text=deviceConsumption
                }
            }
            GattAttributes.mCurrent->{
                val strCurrent= intent.getStringExtra(BluetoothLeService.EXTRA_DATA) ?: return

                text_current.text=strCurrent
                isCurrentPass=(strCurrent.toFloat() in GattAttributes.CURRENT_LOW .. GattAttributes.CURRENT_HIGH)
                if (isCurrentPass!!){
                    show_current.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_current.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_current.setTextColor(Color.RED)
                }
            }
            GattAttributes.mVoltage->{
                val strVoltage=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return

                text_voltage.text=strVoltage
                isVoltagePass=(strVoltage.toFloat() in GattAttributes.VOLTAGE_LOW .. GattAttributes.VOLTAGE_HIGH)
                if (isVoltagePass!!){
                    show_voltage.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_voltage.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_voltage.setTextColor(Color.RED)
                }
            }
            GattAttributes.mWatt->{
                val strWatt=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return

                text_watt.text=strWatt
                isWattPass=(strWatt.toFloat() in GattAttributes.WATT_LOW .. GattAttributes.WATT_HIGH)
                if (isWattPass!!){
                    show_watt.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_watt.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                    text_watt.setTextColor(Color.RED)
                }
            }
            GattAttributes.mPowerFactor->{
                val strPowerFactor=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return

                text_power_factor.text=strPowerFactor
                isPFPass=(strPowerFactor.toFloat() in GattAttributes.PF_LOW .. GattAttributes.PF_HIGH)
                if (isPFPass!!){
                    show_power_factor.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_power_factor.setTextColor(Color.parseColor("#050505"))
                }else{
                    show_power_factor.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                    text_power_factor.setTextColor(Color.RED)
                }
            }
            GattAttributes.mLoad->{
                text_wh.text=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return
            }
            GattAttributes.mPowerOn->{
                synchronized(lock) {
                    Log.d("lock","step get ON")
                    isPowerActivated=true
                    calendar=Calendar.getInstance()
                    powerOnEpoch=calendar.timeInMillis
                    lock.notify()
                }
            }
            GattAttributes.mPowerOff->{
                synchronized(lock) {
                    Log.d("lock","step get OFF")
                    isPowerActivated=false
                    calendar=Calendar.getInstance()
                    powerOffEpoch=calendar.timeInMillis
                    //mBluetoothLeService?.disconnect()
                    lock.notify()
                }
            }
        }
        //checkAllPass()
    }
    private fun resetAfterSetup(){

        show_firmware.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_tag.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_rssi.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_power_factor.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_meter.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
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

        text_wh.setText(R.string.na)
        text_wh.setTextColor(Color.parseColor("#050505"))
        text_meter.setText(R.string.na)
        text_meter.setTextColor(Color.parseColor("#050505"))

        isFirmwarePass=null
        isTagPass=null
        isRssiPass=null
        isCurrentPass=null
        isVoltagePass=null
        isWattPass=null
        isPFPass=null
        isMeterpass=null

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

        if (isLED1Pass!=null&&isLED2Pass!=null&&isLED3Pass!=null&&isLED4Pass!=null&&
            isFirmwarePass!=null&&isTagPass!=null&&isRssiPass!=null&&isCurrentPass!=null&&isVoltagePass!=null&&isWattPass!=null&&isPFPass!=null&&isMeterpass!=null){

            isAllowedSaving=true

            if (isLED1Pass!!&&isLED2Pass!!&&isLED3Pass!!&&isLED4Pass!!&&isFirmwarePass!!&&isTagPass!!&&isRssiPass!!&&isCurrentPass!!&&isVoltagePass!!&&isWattPass!!&&isPFPass!!&&isMeterpass!!){

                isResultPass=true
                text_result.setTextColor(Color.GREEN)
                text_result.setText(R.string.pass)
                doSave()
                resetAfterSetup()
                savingPassToast()

                mBluetoothLeService?.disconnect()

            }else{

                isResultPass=false
                text_result.setTextColor(Color.RED)
                text_result.setText(R.string.fail)
            }
        }else{
            isAllowedSaving=false
        }
    }
    private fun doSave(){

        val realm=Realm.getDefaultInstance()

        calendar=Calendar.getInstance()
        realm.beginTransaction()
        val item=ExamItem()
        item.keyIndex=calendar.timeInMillis

        item.serialNumber=serial
        item.testNumber=testCounts

        //item.productLotNumber=strLotNumber
        item.firmwareNumber=strFirmwareVersion
        item.macAddress=strAddress
        item.tagNumber=text_tag.text.toString()
        item.rssi=rssi

        item.current=text_current.text.toString().toFloat()
        item.voltage=text_voltage.text.toString().toFloat()
        item.watt=text_watt.text.toString().toFloat()
        item.powerFactor=text_power_factor.text.toString().toFloat()
        item.wattHour=text_wh.text.toString().toFloat()

        item.isLEDBlueFlash=isLED1Pass?:false
        item.isLEDBlueOn=isLED2Pass?:false
        item.isLEDGreenOn=isLED3Pass?:false
        item.isLEDRedOn=isLED4Pass?:false
        item.meter=text_meter.text.toString()
        item.result=isResultPass==true

        item.startTime=powerOnEpoch
        item.endTime=powerOffEpoch
        item.aa24Timestamp=aa15Epoch

        realm.copyToRealm(item)
        realm.commitTransaction()

        realm.close()
    }

    private fun createSerialNo(iSerial:Int,iTestCounts:Int,strSerialTwo:String):String{

        val strSerial="%04d".format(iSerial)
        val strTest="%02d".format(iTestCounts)

        return ("$strSerialTwo-$strSerial-$strTest")
    }

    private fun checkSerial(){

        val realm=Realm.getDefaultInstance()

        //檢查之前有沒有用此address的測試記錄
        val previous=realm.where(ExamItem::class.java)
                .equalTo("macAddress",strAddress)
                .findFirst()


        if (previous==null){
            //若記錄為null 表示
                // A.是第一次測試,
                    // B.是連了很多次,但未測過此address
            //testCounts都設為0
            val lastItem=realm.where(ExamItem::class.java)
                    .sort("serialNumber",Sort.DESCENDING)
                    .findFirst()

            serial = if (lastItem!=null){
                //case B, 則serial+1
                lastItem.serialNumber+1
            }else{
                //case A, serial=1
                1
            }
            testCounts=0
            //A or B, testCounts皆0

        }else{
            //若記錄非null, 則之前有測過該address, 從記錄取出serial
                //testCounts要抓出記錄再+1
            serial=previous.serialNumber
            testCounts=checkTestCount()
        }
        Log.d("show_result","serial=$serial, test counts=$testCounts")

        realm.close()
    }
    private fun checkTestCount():Int{

        val realm=Realm.getDefaultInstance()

        val result=realm.where(ExamItem::class.java)
                .equalTo("macAddress",strAddress)
                .sort("testNumber",Sort.DESCENDING)
                .findFirst()

        if (result==null)return 0

        val count=result.testNumber+1

        realm.close()
        return count
    }

    private fun savingToast(){
        val toast=Toast(this)
        val toastView=LayoutInflater.from(this).inflate(R.layout.custom_toast,null)
        toastView.alpha=0.7f

        toast.setView(toastView)
        toast.setGravity(Gravity.FILL, 0, 0)
        toast.duration = Toast.LENGTH_SHORT
        toast.show()

    }

    private fun savingPassToast(){
        val toast=Toast(this)
        val toastView=LayoutInflater.from(this).inflate(R.layout.custom_toast,null)
        toastView.alpha=0.7f
        toastView.text_toast.text=getString(R.string.pass_saved_successfully)

        toast.view=toastView
        toast.setGravity(Gravity.FILL,0,0)
        toast.duration=Toast.LENGTH_SHORT
        toast.show()

    }

    private fun booleanToString(boolean: Boolean):String{
        return if (boolean){
            getString(R.string.pass)
        }else{
            getString(R.string.fail)
        }
    }
    private fun makingCSV(){

        Thread{

            runOnUiThread {
                progressbar.visibility=View.VISIBLE
            }

            val realm=Realm.getDefaultInstance()
            val count=realm.where(ExamItem::class.java)
                    .equalTo("testNumber",0.toInt())
                    .count().toInt()

            if (count==0){
                realm.close()
                runOnUiThread {
                    progressbar.visibility=View.GONE
                }
                return@Thread
            }

            ///////////////////////////////////
            val date=SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())
            val format=SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val fileName="[$strOrderSerialOne-$strOrderSerialTwo].csv"

            val title= arrayOf(
                    "Serial Number",
                    "Firmware:$strFirmwareVersion",
                    "MAC address",
                    "TAG:$strTagNumber",
                    "BLE Rssi:-$strRssiSmall~-$strRssiLarge",
                    "Current:${GattAttributes.CURRENT_LOW}~${GattAttributes.CURRENT_HIGH}",
                    "Voltage:${GattAttributes.VOLTAGE_LOW}~${GattAttributes.VOLTAGE_HIGH}",
                    "Watt:${GattAttributes.WATT_LOW}~${GattAttributes.WATT_HIGH}",
                    "PF:${GattAttributes.PF_LOW}~${GattAttributes.PF_HIGH}",
                    "Watt/Hour",
                    "LED1:Blue Flash",
                    "LED2:Blue",
                    "LED3:Green Flash",
                    "LED4:Red",
                    "Meter",
                    "Result",
                    "TimeStamp")

            val csvText=StringBuffer()
            csvText.append("Order Number:$strOrderSerialOne-$strOrderSerialTwo\n")
            csvText.append("Producing Date:$date\n")
            val department=when(intTestDepartment){
                1->"QA"
                2->"RD"
                else->"IPQC"
            }
            csvText.append("Test department:$department\n\n")

            for (i in title.indices){
                csvText.append("${title[i]},")
            }
            csvText.append("\n")

            for (i in 1 .. count){

                val result=realm.where(ExamItem::class.java)
                        .equalTo("serialNumber",i)
                        .sort("testNumber",Sort.ASCENDING)
                        .findAll()

                val iterator=result.iterator()
                while (iterator.hasNext()){
                    val mItem=iterator.next()

                    val serialNumber=createSerialNo(mItem.serialNumber,mItem.testNumber,strOrderSerialTwo)
                    val firmware=mItem.firmwareNumber
                    val macAddress=mItem.macAddress
                    val tag=mItem.tagNumber
                    val rssi=mItem.rssi.toString()
                    val current=mItem.current.toString()
                    val voltage=mItem.voltage.toString()
                    val watt=mItem.watt.toString()
                    val pf=mItem.powerFactor.toString()
                    val wh=mItem.wattHour.toString()
                    val led1=booleanToString(mItem.isLEDBlueFlash)
                    val led2=booleanToString(mItem.isLEDBlueOn)
                    val led3=booleanToString(mItem.isLEDGreenOn)
                    val led4=booleanToString(mItem.isLEDRedOn)
                    val meter=mItem.meter
                    val finalResult=booleanToString(mItem.result)
                    calendar.timeInMillis=mItem.aa24Timestamp
                    val timestamp=format.format(calendar.time)

                    csvText.append("$serialNumber,")
                            .append("$firmware,")
                            .append("$macAddress,")
                            .append("$tag,")
                            .append("$rssi,")
                            .append("$current,")
                            .append("$voltage,")
                            .append("$watt,")
                            .append("$pf,")
                            .append("$wh,")
                            .append("$led1,")
                            .append("$led2,")
                            .append("$led3,")
                            .append("$led4,")
                            .append("$meter,")
                            .append("$finalResult,")
                            .append("$timestamp\n")

                }
            }
            if (!realm.isClosed){
                realm.close()
            }
            Log.d("all","makingCSV:\n$csvText")
            runOnUiThread {

                /*
                val builder=StrictMode.VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                builder.detectFileUriExposure()

                 */

                progressbar.visibility=View.GONE
                try {
                    val out=openFileOutput(fileName,Context.MODE_PRIVATE)
                    out.write(csvText.toString().toByteArray())
                    out.close()
                    //////////////////////////////////////////
                    //val cw=ContextWrapper(applicationContext)
                    //val directory=cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    //val fileLocation=File(directory,fileName)
                    //val fileLocation=File(Environment.getExternalStorageDirectory().absolutePath,fileName)
                    val fileLocation=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),fileName)
                    val fos=FileOutputStream(fileLocation)
                    fos.write(csvText.toString().toByteArray())
                    fos.close()



                    //val path=Uri.fromFile(fileLocation)
                    val authority=packageName.plus(".fileProvider")
                    val path=FileProvider.getUriForFile(this,authority,fileLocation)

                    val fileIntent=Intent(Intent.ACTION_SEND)
                    fileIntent.type="text/csv"
                    fileIntent.data=path
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT,fileName)
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    fileIntent.putExtra(Intent.EXTRA_STREAM,path)
                    startActivity(Intent.createChooser(fileIntent,"output files"))



                    //savingToast()
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }.start()
    }

    inner class BLEDeviceAdapter(private var mBleDevices:ArrayList<BluetoothDevice>,private var mRssiList:ArrayList<Int>,private val dialog: Dialog):RecyclerView.Adapter<ViewHolder>(){

        fun clear(){
            mBleDevices.clear()
            mRssiList.clear()
        }

        fun addDevice(device:BluetoothDevice,rssi:Int){
            if (device.name==null)return

            //(device.name.substring(0,6).trim()=="eCloud")||(device.name.substring(0,4).trim()=="F100")
            if (device.name.contains("eCloud")){

                if (mBleDevices.contains(device)){
                    return
                }
                Log.d("testF1166","address ${device.address}, name:${device.name}, other")

                mRssiList.add(rssi)
                mRssiList.sortDescending()
                val int=mRssiList.indexOf(rssi)
                mBleDevices.add(int,device)

                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v=LayoutInflater.from(parent.context).inflate(R.layout.list_item,parent,false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Log.d("testF1166","....$position")
            holder.txName.text=mBleDevices[position].name
            holder.txAddress.text=mBleDevices[position].address
            holder.txRssi.text=mRssiList[position].toString()
            holder.clDevice.setOnClickListener {

                //strAddress=mBleDevices[position].address
                strAddress=holder.txAddress.text.toString()
                //deviceName=mBleDevices[position].name
                deviceName=holder.txName.text.toString()
                //rssi=mRssiList[position]
                rssi=holder.txRssi.text.toString().toInt()
                toolbar?.title=strAddress

                upper_cover_main.visibility=View.GONE
                btn_connection.isClickable=true
                dialog.dismiss()
                if (isScanning){
                    scanner?.stopScan(mLeScanCallback)
                    isScanning=false
                }
            }
        }
        override fun getItemCount(): Int {
            return mBleDevices.size
        }
    }

    class ViewHolder(v:View):RecyclerView.ViewHolder(v){
        val txName=v.findViewById<TextView>(R.id.device_name)
        val txAddress=v.findViewById<TextView>(R.id.device_address)
        val txRssi=v.findViewById<TextView>(R.id.device_rssi)
        val clDevice=v.findViewById<ConstraintLayout>(R.id.device_item)
    }

}