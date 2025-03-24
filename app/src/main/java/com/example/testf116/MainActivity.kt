package com.example.testf116

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.set
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.btn_connection
import kotlinx.android.synthetic.main.activity_main.btn_no_load_test
import kotlinx.android.synthetic.main.activity_main.btn_reset
import kotlinx.android.synthetic.main.activity_main.btn_save
import kotlinx.android.synthetic.main.activity_main.btn_with_load_test
import kotlinx.android.synthetic.main.activity_main.fail_led_1
import kotlinx.android.synthetic.main.activity_main.fail_led_2
import kotlinx.android.synthetic.main.activity_main.fail_led_3
import kotlinx.android.synthetic.main.activity_main.fail_led_4
import kotlinx.android.synthetic.main.activity_main.led_test_group
import kotlinx.android.synthetic.main.activity_main.lower_cover_main
import kotlinx.android.synthetic.main.activity_main.pass_led_1
import kotlinx.android.synthetic.main.activity_main.pass_led_2
import kotlinx.android.synthetic.main.activity_main.pass_led_3
import kotlinx.android.synthetic.main.activity_main.pass_led_4
import kotlinx.android.synthetic.main.activity_main.progressbar
import kotlinx.android.synthetic.main.activity_main.show_firmware
import kotlinx.android.synthetic.main.activity_main.show_led_1
import kotlinx.android.synthetic.main.activity_main.show_led_2
import kotlinx.android.synthetic.main.activity_main.show_led_3
import kotlinx.android.synthetic.main.activity_main.show_led_4
import kotlinx.android.synthetic.main.activity_main.show_no_load_current
import kotlinx.android.synthetic.main.activity_main.show_no_load_voltage
import kotlinx.android.synthetic.main.activity_main.show_rssi
import kotlinx.android.synthetic.main.activity_main.show_tag
import kotlinx.android.synthetic.main.activity_main.show_with_load_current
import kotlinx.android.synthetic.main.activity_main.show_with_load_power_factor
import kotlinx.android.synthetic.main.activity_main.show_with_load_voltage
import kotlinx.android.synthetic.main.activity_main.show_with_load_watt
import kotlinx.android.synthetic.main.activity_main.test_no_load_data_card
import kotlinx.android.synthetic.main.activity_main.test_phase_text
import kotlinx.android.synthetic.main.activity_main.test_with_load_data_card
import kotlinx.android.synthetic.main.activity_main.text_firmware
import kotlinx.android.synthetic.main.activity_main.text_meter
import kotlinx.android.synthetic.main.activity_main.text_no_load_current
import kotlinx.android.synthetic.main.activity_main.text_no_load_voltage
import kotlinx.android.synthetic.main.activity_main.text_result
import kotlinx.android.synthetic.main.activity_main.text_rssi
import kotlinx.android.synthetic.main.activity_main.text_tag
import kotlinx.android.synthetic.main.activity_main.text_with_load_current
import kotlinx.android.synthetic.main.activity_main.text_with_load_power_factor
import kotlinx.android.synthetic.main.activity_main.text_with_load_voltage
import kotlinx.android.synthetic.main.activity_main.text_with_load_watt
import kotlinx.android.synthetic.main.activity_main.text_with_load_wh
import kotlinx.android.synthetic.main.activity_main.upper_cover_main
import kotlinx.android.synthetic.main.custom_toast.view.text_toast
import kotlinx.android.synthetic.main.dialog_export.export_btn
import kotlinx.android.synthetic.main.dialog_export.total_device
import kotlinx.android.synthetic.main.dialog_export.total_tests
import kotlinx.android.synthetic.main.dialog_header.cancel_button
import kotlinx.android.synthetic.main.dialog_header.expandable_section
import kotlinx.android.synthetic.main.dialog_header.firmware_number
import kotlinx.android.synthetic.main.dialog_header.header_text
import kotlinx.android.synthetic.main.dialog_header.load_current
import kotlinx.android.synthetic.main.dialog_header.load_voltage
import kotlinx.android.synthetic.main.dialog_header.meter
import kotlinx.android.synthetic.main.dialog_header.no_load_voltage
import kotlinx.android.synthetic.main.dialog_header.order_serial_1
import kotlinx.android.synthetic.main.dialog_header.order_serial_2
import kotlinx.android.synthetic.main.dialog_header.producing_time
import kotlinx.android.synthetic.main.dialog_header.rssi_large
import kotlinx.android.synthetic.main.dialog_header.rssi_small
import kotlinx.android.synthetic.main.dialog_header.save_button
import kotlinx.android.synthetic.main.dialog_header.tag_number
import kotlinx.android.synthetic.main.dialog_header.testing_department
import kotlinx.android.synthetic.main.dialog_scan_result.recyclerview
import kotlinx.android.synthetic.main.dialog_scan_result.refreshlayout
import kotlinx.android.synthetic.main.fragment_barcode.barcode_mac
import kotlinx.android.synthetic.main.fragment_barcode.barcode_name
import kotlinx.android.synthetic.main.fragment_barcode.barcode_name_mac
import kotlinx.android.synthetic.main.fragment_barcode.cancel_show_rssi
import kotlinx.android.synthetic.main.fragment_barcode.content_rssi
import kotlinx.android.synthetic.main.fragment_barcode.text_mac
import kotlinx.android.synthetic.main.fragment_barcode.text_name
import kotlinx.android.synthetic.main.fragment_barcode.text_name_mac
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID


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

    private var toleranceNoLoadVoltage=0f
    private var toleranceWithLoadCurrent=0f
    private var toleranceWithLoadVoltage=0f

    private var intTestDepartment=0
    private var powerOnEpoch=0L
    private var powerOffEpoch=0L
    private var aa15Epoch=0L

    private lateinit var calendar:Calendar
    private var testCounts=0
    private var serial=0

    private var isFirmwarePass:Boolean?=null
    private var isTagPass:Boolean?=null
    //private var isMeterpass:Boolean?=null
    private var isRssiPass:Boolean?=null
    private var isCurrentPassNoLoad:Boolean?=null
    private var isVoltagePassNoLoad:Boolean?=null
    private var isCurrentPassWithLoad:Boolean?=null
    private var isVoltagePassWithLoad:Boolean?=null
    private var isWattPassWithLoad:Boolean?=null
    private var isPFPassWithLoad:Boolean?=null
    private var isLED1Pass:Boolean?=null
    private var isLED2Pass:Boolean?=null
    private var isLED3Pass:Boolean?=null
    private var isLED4Pass:Boolean?=null
    private var isResultPass:Boolean?=null

    private val lock=Object()
    private val channel= Channel<Unit>()
    private var reConnectCount=0
    private var isAllowedSaving=false

    private var loadType=0// 0:no-load, 1:with-load

    private var clickCount=0

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
                    mBluetoothLeService?.disconnect()
                }else{

                    mBluetoothLeService?.connect(strAddress)
                }
                progressbar.visibility=View.VISIBLE
            }
            R.id.btn_no_load_test->{
                updateUIForTestPhase(TestPhase.NO_LOAD)
                doCharacteristicMeasurement(0)
            }
            R.id.btn_with_load_test->{
                updateUIForTestPhase(TestPhase.WITH_LOAD)
                doCharacteristicMeasurement(1)
            }
            R.id.btn_save -> {
                Log.d("btnTest", "save btn")

                if (isAllowedSaving) {
                    doSave()
                    resetAfterSetup(true)
                    savingToast()
                    checkSerial()
                    isAllowedSaving=false
                } else {
                    Toast.makeText(this,getString(R.string.unable_save),Toast.LENGTH_SHORT).show()
                }
            }
            R.id.btn_reset->{
                updateUIForTestPhase(TestPhase.RESET)
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
                    val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                        ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
                    }else{
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                    }
                    if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
                        adapter!!.enable()
                    }
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
        //btn_test.setOnClickListener(this)
        btn_no_load_test.setOnClickListener(this)
        btn_with_load_test.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        btn_reset.setOnClickListener(this)

        fail_led_1.setOnClickListener(this)
        fail_led_2.setOnClickListener(this)
        fail_led_3.setOnClickListener(this)
        fail_led_4.setOnClickListener(this)
        pass_led_1.setOnClickListener(this)
        pass_led_2.setOnClickListener(this)
        pass_led_3.setOnClickListener(this)
        pass_led_4.setOnClickListener(this)

        /*
        if (isConnected){
            lower_cover_main.visibility=View.GONE
            setButtonClickable(true)
        }else{
            lower_cover_main.visibility=View.VISIBLE
            setButtonClickable(false)
        }

         */

        /*
        if (strAddress.isEmpty()){
            upper_cover_main.visibility=View.VISIBLE
            btn_connection.isClickable=false
        }else{
            upper_cover_main.visibility=View.GONE
            btn_connection.isClickable=true
        }

         */
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

        toleranceNoLoadVoltage=sharedPreferences.getFloat(GattAttributes.TOLERANCE_NO_LOAD_VOLTAGE,GattAttributes.DEFAULT_TOLERANCE_NO_LOAD_VOLTAGE)
        toleranceWithLoadCurrent=sharedPreferences.getFloat(GattAttributes.TOLERANCE_WITH_LOAD_CURRENT,GattAttributes.DEFAULT_TOLERANCE_WITH_LOAD_CURRENT)
        toleranceWithLoadVoltage=sharedPreferences.getFloat(GattAttributes.TOLERANCE_WITH_LOAD_VOLTAGE,GattAttributes.DEFAULT_TOLERANCE_WITH_LOAD_VOLTAGE)
        Log.d("testTolerance","no-load V $toleranceNoLoadVoltage, with-load C $toleranceWithLoadCurrent, with-load V $toleranceWithLoadVoltage")
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
            toleranceNoLoadVoltage=dialog.no_load_voltage.selectedItem.toString().toFloat()
            toleranceWithLoadCurrent=dialog.load_current.selectedItem.toString().toFloat()
            toleranceWithLoadVoltage=dialog.load_voltage.selectedItem.toString().toFloat()


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
                    .putFloat(GattAttributes.TOLERANCE_NO_LOAD_VOLTAGE,toleranceNoLoadVoltage)
                    .putFloat(GattAttributes.TOLERANCE_WITH_LOAD_CURRENT,toleranceWithLoadCurrent)
                    .putFloat(GattAttributes.TOLERANCE_WITH_LOAD_VOLTAGE,toleranceWithLoadVoltage)
                    .apply()
            dialog.dismiss()
            resetAfterSetup(false)
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
        dialog.header_text.setOnClickListener {
            clickCount++
            if (clickCount==7){
                dialog.expandable_section.visibility=View.VISIBLE
                clickCount=0
            }
        }
        // Set up no-load voltage spinner
        val voltageValues= arrayOf("1.0","1.5","2.0","2.5","3.0")
        val noLoadVoltageAdapter=ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            voltageValues
        )
        dialog.no_load_voltage.adapter=noLoadVoltageAdapter
        val strNoLoadVoltage=toleranceNoLoadVoltage.toString()
        val noLoadVoltagePos=voltageValues.indexOf(strNoLoadVoltage)
        dialog.no_load_voltage.setSelection(noLoadVoltagePos)

        // Set up current spinner
        val currentValues = arrayOf("1.0", "2.0", "3.0")
        val currentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            currentValues
        )
        dialog.load_current.adapter = currentAdapter
        val strWithLoadCurrent=toleranceWithLoadCurrent.toString()
        val withLoadCurrentPos=currentValues.indexOf(strWithLoadCurrent)
        dialog.load_current.setSelection(withLoadCurrentPos)

        // Set up voltage spinner
        val voltageAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            voltageValues
        )
        dialog.load_voltage.adapter = voltageAdapter
        val strWithLoadVoltage=toleranceWithLoadVoltage.toString()
        val withLoadVoltagePos=voltageValues.indexOf(strWithLoadVoltage)
        dialog.load_voltage.setSelection(withLoadVoltagePos)


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
                codeAsBitmap(strAddress, BarcodeFormat.CODE_128, 900, 200)
            )


            dialog.text_mac.text=strAddress

            val address=strAddress.split(":")
            var nameAndMac="F100"
            for (i in address.indices){
                nameAndMac+=address[i]
            }


            dialog.barcode_name_mac.setImageBitmap(
                codeAsBitmap(nameAndMac, BarcodeFormat.CODE_128,900, 200)
            )


            dialog.text_name_mac.text=nameAndMac
        }

        if (deviceName.isNotEmpty()) {
            dialog.barcode_name.setImageBitmap(
                codeAsBitmap(deviceName, BarcodeFormat.CODE_128, 900, 200)
            )



            Log.d("testName","device name is -$deviceName-")

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
            val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
                adapter?.enable()
            }
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
                val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                    ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
                }else{
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                }
                if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
                    scanner?.stopScan(mLeScanCallback)
                }
            },5000)

            isScanning=true
            val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
                scanner?.startScan(mLeScanCallback)
            }
        }else{

            isScanning=false
            val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
                scanner?.stopScan(mLeScanCallback)
            }
        }
    }


    private val mLeScanCallback:ScanCallback= object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result==null)return

            if (result.device==null)return

            val permissionCheck1=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck1==PackageManager.PERMISSION_GRANTED) {
                if (result.device.name == null) return
            }

            //if (result.device.name.substring(0,6).trim()!="eCloud"&&result.device.name.substring(0,4).trim()!="F100")return
            if (!result.device.name.contains("eCloud"))return

            //scanLeDevice(false)

            val permissionCheck2=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck2==PackageManager.PERMISSION_GRANTED) {
                Log.d(
                    "testF1166",
                    "address ${result.device.address}, name: ${result.device.name}, rssi ${result.rssi}"
                )
            }

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
                updateUIForTestPhase(TestPhase.DISCONNECT)
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED==action){
                isConnected=true
                btn_connection.setText(R.string.disconnected)
                btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                //upper_cover_main.visibility=View.GONE
                checkSerial()
                //progressbar.visibility=View.GONE
                doCharacteristicDeviceInfo()
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
                setFirmwareResult(intent)
            }
            GattAttributes.mNfcTagId->{
                setTagResult(intent)
            }
            GattAttributes.mRssi->{
                setRssiResult(intent)
            }
            GattAttributes.mMeterVersion->{
                setMeterResult(intent)
            }

            GattAttributes.mReadRecordedData->{
                setRecordResult(intent)
            }
            GattAttributes.mCurrent->{
                setCurrentResult(intent)
            }
            GattAttributes.mVoltage->{
                setVoltageResult(intent)
            }
            GattAttributes.mWatt->{
                setWattResult(intent)
            }
            GattAttributes.mPowerFactor->{
                setPowerFactorResult(intent)
            }
            GattAttributes.mLoad->{
                setLoadResult(intent)
            }
            GattAttributes.mPowerOn->{
                CoroutineScope(Dispatchers.IO).launch {
                    isPowerActivated=true
                    calendar=Calendar.getInstance()
                    powerOnEpoch=calendar.timeInMillis
                    channel.send(Unit)
                }
            }
            GattAttributes.mPowerOff->{
                CoroutineScope(Dispatchers.IO).launch {
                    isPowerActivated=false
                    calendar=Calendar.getInstance()
                    powerOffEpoch=calendar.timeInMillis
                    channel.send(Unit)
                }
            }
        }
        //checkAllPass()
    }

    private fun doCharacteristicDeviceInfo(){
        CoroutineScope(Dispatchers.Main).launch {
            //progress
            progressbar.visibility = View.VISIBLE
            //讀firmware version
            characteristic =
                (mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_INFORMATION) as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.firmware_revision_string))
            if (characteristic != null) {
                mBluetoothLeService?.readCharacteristic(characteristic!!)
            }
            //等500ms
            withContext(Dispatchers.IO){
                delay(500)
            }
            //讀tag No.
            characteristic =
                (mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL) as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.nfc_tag_id))
            if (characteristic != null) {
                mBluetoothLeService?.readCharacteristic(characteristic!!)
            }
            //等500ms
            withContext(Dispatchers.IO){
                delay(500)
            }
            //讀Rssi
            mBluetoothLeService?.readRemoteRssii()
            //等500ms
            withContext(Dispatchers.IO){
                delay(500)
            }
            //讀meter
            characteristic =
                (mBluetoothLeService?.getSupportedGattService(GattAttributes.EXTRA_CONTROL) as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.meter_parameter))
            if (characteristic != null) {
                mBluetoothLeService?.readCharacteristic(characteristic!!)
            }
        }
    }
    private fun doCharacteristicMeasurement(testType:Int){
        loadType=testType

        isPowerActivated=false
        progressbar.visibility=View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            //啟動relay3次
            while (!isPowerActivated) {
                if (reConnectCount==3){
                    reConnectCount=0
                    progressbar.visibility=View.GONE
                    return@launch
                }
                doRelay(true)

                channel.receive()

                characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.DEVICE_CONTROL)as BluetoothGattService)
                    .getCharacteristic(UUID.fromString(GattAttributes.activate_power))
                if (characteristic!=null){
                    mBluetoothLeService?.readCharacteristic(characteristic!!)
                }

                channel.receive()
            }
            characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                .getCharacteristic(UUID.fromString(GattAttributes.current))
            if (characteristic!=null){
                mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
            }
            withContext(Dispatchers.IO){
                delay(500)
            }
            characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                .getCharacteristic(UUID.fromString(GattAttributes.voltage))
            if (characteristic!=null){
                mBluetoothLeService?.setCharacteristicNotification(characteristic!!,true)
            }
            if (loadType==1) {
                withContext(Dispatchers.IO) {
                    delay(500)
                }
                characteristic =
                    (mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT) as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.watt))
                if (characteristic != null) {
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!, true)
                }
                withContext(Dispatchers.IO) {
                    delay(500)
                }
                characteristic =
                    (mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT) as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.power_factor))
                if (characteristic != null) {
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!, true)
                }
                withContext(Dispatchers.IO) {
                    delay(500)
                }
                characteristic =
                    (mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT) as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.load))
                if (characteristic != null) {
                    mBluetoothLeService?.setCharacteristicNotification(characteristic!!, true)
                }
            }
            withContext(Dispatchers.IO){
                delay(4000)
            }
            characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                .getCharacteristic(UUID.fromString(GattAttributes.current))
            if (characteristic!=null){
                mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
            }
            withContext(Dispatchers.IO){
                delay(200)
            }
            characteristic=(mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT)as BluetoothGattService)
                .getCharacteristic(UUID.fromString(GattAttributes.voltage))
            if (characteristic!=null){
                mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
            }
            withContext(Dispatchers.IO){
                delay(200)
            }
            if (loadType==1) {
                characteristic =
                    (mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT) as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.power_factor))
                if (characteristic != null) {
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
                withContext(Dispatchers.IO) {
                    delay(200)
                }
                characteristic =
                    (mBluetoothLeService?.getSupportedGattService(GattAttributes.POWER_MEASUREMENT) as BluetoothGattService)
                        .getCharacteristic(UUID.fromString(GattAttributes.load))
                if (characteristic != null) {
                    mBluetoothLeService?.disableCharacteristicNotification(characteristic!!)
                }
                withContext(Dispatchers.IO) {
                    delay(200)
                }
            }
            doRelay(false)
            channel.receive()

            calendar=Calendar.getInstance()
            aa15Epoch=calendar.timeInMillis
            checkAllPass()
            progressbar.visibility=View.GONE

            if (loadType==0){
                updateUIForTestPhase(TestPhase.NO_LOAD_DONE)
            }else if (loadType==1){
                updateUIForTestPhase(TestPhase.WITH_LOAD_DONE)
            }
        }
    }
    private fun resetAfterSetup(isSoftReset:Boolean){

        if (!isSoftReset) {
            show_firmware.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
            show_tag.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
            show_rssi.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        }

        show_no_load_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_no_load_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)

        show_with_load_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_with_load_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_with_load_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_with_load_power_factor.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)

        show_led_1.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_2.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_3.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
        show_led_4.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)

        if (!isSoftReset) {
            text_firmware.setText(R.string.na)
            text_firmware.setTextColor(Color.parseColor("#050505"))
            text_tag.setText(R.string.na)
            text_tag.setTextColor(Color.parseColor("#050505"))
            text_rssi.setText(R.string.na)
            text_rssi.setTextColor(Color.parseColor("#050505"))
            text_meter.setText(R.string.na)
            text_meter.setTextColor(Color.parseColor("#050505"))
        }

        text_no_load_current.setText(R.string.na)
        text_no_load_current.setTextColor(Color.parseColor("#050505"))
        text_no_load_voltage.setText(R.string.na)
        text_no_load_voltage.setTextColor(Color.parseColor("#050505"))

        text_with_load_current.setText(R.string.na)
        text_with_load_current.setTextColor(Color.parseColor("#050505"))
        text_with_load_voltage.setText(R.string.na)
        text_with_load_voltage.setTextColor(Color.parseColor("#050505"))
        text_with_load_watt.setText(R.string.na)
        text_with_load_watt.setTextColor(Color.parseColor("#050505"))
        text_with_load_power_factor.setText(R.string.na)
        text_with_load_power_factor.setTextColor(Color.parseColor("#050505"))
        text_with_load_wh.setText(R.string.na)
        text_with_load_wh.setTextColor(Color.parseColor("#050505"))

        text_result.setText(R.string.na)
        text_result.setTextColor(Color.parseColor("#050505"))

        isFirmwarePass=null
        isTagPass=null
        isRssiPass=null

        isCurrentPassNoLoad=null
        isVoltagePassNoLoad=null

        isLED1Pass=null
        isLED2Pass=null
        isLED3Pass=null
        isLED4Pass=null

        isAllowedSaving=false
    }
    private fun checkAllPass(){
        //6/15去掉 isMeterpass
        if (isLED1Pass!=null&&isLED2Pass!=null&&isLED3Pass!=null&&isLED4Pass!=null&&
            isFirmwarePass!=null&&isTagPass!=null&&isRssiPass!=null&&isCurrentPassNoLoad!=null&&isVoltagePassNoLoad!=null){

            updateUIForTestPhase(TestPhase.COMPLETED)
            isAllowedSaving=true

            //6/15去掉 isMeterpass
            if (isLED1Pass!!&&isLED2Pass!!&&isLED3Pass!!&&isLED4Pass!!&&isFirmwarePass!!&&isTagPass!!&&isRssiPass!!&&isCurrentPassNoLoad!!&&isVoltagePassNoLoad!!){

                isResultPass=true
                text_result.setTextColor(Color.GREEN)
                text_result.setText(R.string.pass)
                doSave()
                resetAfterSetup(true)
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

        item.current=text_no_load_current.text.toString().toFloat()
        item.voltage=text_no_load_voltage.text.toString().toFloat()

        item.currentWithLoad=text_with_load_current.text.toString().toFloat()
        item.voltageWithLoad=text_with_load_voltage.text.toString().toFloat()
        item.wattWithLoad=text_with_load_watt.text.toString().toFloat()
        item.powerFactorWithLoad=text_with_load_power_factor.text.toString().toFloat()
        item.wattHourWithLoad=text_with_load_wh.text.toString().toFloat()

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
                    "Current No-Load:<=${GattAttributes.MAX_NO_LOAD_CURRENT}A",
                    "Voltage No-Load:${
                        calculatePercentage(GattAttributes.TARGET_VOLTAGE, toleranceNoLoadVoltage, false)
                    }~${
                        calculatePercentage(GattAttributes.TARGET_VOLTAGE, toleranceNoLoadVoltage, true)}",

                    "Current With-Load:${
                        calculatePercentage(GattAttributes.TARGET_CURRENT,toleranceWithLoadCurrent,false)
                    }~${
                        calculatePercentage(GattAttributes.TARGET_CURRENT,toleranceWithLoadCurrent,true)}",
                    "Voltage With-Load:${
                        calculatePercentage(GattAttributes.TARGET_VOLTAGE,toleranceWithLoadVoltage,false)
                    }~${
                        calculatePercentage(GattAttributes.TARGET_VOLTAGE,toleranceWithLoadVoltage,true)}",
                    "Watt With-Load:${
                        calculatePercentage(GattAttributes.TARGET_VOLTAGE*GattAttributes.TARGET_CURRENT,toleranceWithLoadVoltage+toleranceWithLoadCurrent,false)
                    }~${
                        calculatePercentage(GattAttributes.TARGET_VOLTAGE*GattAttributes.TARGET_CURRENT,toleranceWithLoadVoltage+toleranceWithLoadCurrent,true)}",
                    "PF With-Load:${GattAttributes.PF_LOW}~${GattAttributes.PF_HIGH}",
                    "Watt/Hour With-Load",

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

                    val currentNoLoad=mItem.current.toString()
                    val voltageNoLoad=mItem.voltage.toString()

                    val currentWithLoad=mItem.currentWithLoad.toString()
                    val voltageWithLoad=mItem.voltageWithLoad.toString()
                    val wattWithLoad=mItem.wattWithLoad.toString()
                    val pfWithLoad=mItem.powerFactorWithLoad.toString()
                    val whWithLoad=mItem.wattHourWithLoad.toString()

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
                            .append("$currentNoLoad,")
                            .append("$voltageNoLoad,")
                            .append("$currentWithLoad,")
                            .append("$voltageWithLoad,")
                            .append("$wattWithLoad,")
                            .append("$pfWithLoad,")
                            .append("$whWithLoad,")
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
            val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck!=PackageManager.PERMISSION_GRANTED) {
                return
            }
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
            val permissionCheck=if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
                ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.BLUETOOTH_CONNECT)
            }else{
                ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
                holder.txName.text = mBleDevices[position].name
            }
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

                //upper_cover_main.visibility=View.GONE
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

    private fun updateUIForTestPhase(phase: TestPhase){
        test_phase_text.visibility=View.VISIBLE
        when(phase){
            TestPhase.IDLE->{
                // 初始狀態UI設置
                test_phase_text.text=getText(R.string.choose_process)
                test_phase_text.setTextColor(ContextCompat.getColor(this,R.color.word_black))

                // 啟用空載測試按鈕，禁用其他按鈕
                btn_no_load_test.isEnabled=true
                btn_with_load_test.isEnabled=false

                // 隱藏所有檢查區域
                test_no_load_data_card.visibility=View.GONE
                test_with_load_data_card.visibility=View.GONE
                led_test_group.visibility=View.GONE

            }
            TestPhase.NO_LOAD->{
                // 空載測試階段UI設置
                test_phase_text.text=getText(R.string.no_load_testing)
                test_phase_text.setTextColor(ContextCompat.getColor(this,R.color.word_blue))

                // 禁用所有按鈕，直到測試完成
                btn_no_load_test.isEnabled=false
                btn_with_load_test.isEnabled=false

                // show no load area
                test_no_load_data_card.visibility=View.VISIBLE

            }
            TestPhase.NO_LOAD_DONE->{
                // 空載測試階段UI設置
                test_phase_text.text=getText(R.string.no_load_test_done)
                test_phase_text.setTextColor(ContextCompat.getColor(this,R.color.word_black))

                // no test測試完成 開放2個
                btn_no_load_test.isEnabled=true
                btn_with_load_test.isEnabled=true

            }
            TestPhase.WITH_LOAD->{
                // 帶載測試階段UI設置
                test_phase_text.text =getText(R.string.with_load_testing)
                test_phase_text.setTextColor(ContextCompat.getColor(this, R.color.word_blue))

                // 空載按鈕可用，其他禁用
                btn_no_load_test.isEnabled = false
                btn_with_load_test.isEnabled = false

                // show with load area
                test_with_load_data_card.visibility=View.VISIBLE
            }
            TestPhase.WITH_LOAD_DONE->{
                // 帶載測試結束階段 UI設置

                val withLoadTestDone=getText(R.string.with_load_test_done)
                val checkLED=getText(R.string.please_check_led_status)

                val spannableString=SpannableString("$withLoadTestDone\n$checkLED")

                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this,R.color.word_black)),
                    0, withLoadTestDone.length,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this,R.color.word_blue)),
                    withLoadTestDone.length+1,spannableString.length,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                test_phase_text.text=spannableString

                // with test測試完成 開放2個
                btn_no_load_test.isEnabled=true
                btn_with_load_test.isEnabled=true

                // 顯示LED檢查區域
                led_test_group.visibility = View.VISIBLE
            }
            TestPhase.COMPLETED->{
                // 測試完成階段UI設置
                test_phase_text.text = getText(R.string.test_finish)
                test_phase_text.setTextColor(ContextCompat.getColor(this, R.color.word_black))

                // 所有按鈕可用，可以重新測試
                btn_no_load_test.isEnabled = true
                btn_with_load_test.isEnabled = true

                // 顯示result&save區域
                lower_cover_main.visibility=View.GONE
            }
            TestPhase.RESET->{
                resetAfterSetup(true)
            }
            TestPhase.DISCONNECT->{
                resetAfterSetup(false)
                upper_cover_main.visibility=View.VISIBLE
                lower_cover_main.visibility=View.VISIBLE
                progressbar.visibility=View.GONE
            }
        }
    }
    private fun setFirmwareResult(intent: Intent){
        val version=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:"0"
        isFirmwarePass=(version==strFirmwareVersion)

        text_firmware.text=version
        if (isFirmwarePass!!){
            show_firmware.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
            text_firmware.setTextColor(Color.parseColor("#050505"))
        }else{
            show_firmware.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
            text_firmware.setTextColor(Color.RED)
        }
    }
    private fun setTagResult(intent: Intent){
        val tag=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:"0000"
        isTagPass=(tag==strTagNumber)

        text_tag.text=tag

        if (isTagPass!!){
            show_tag.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
            text_tag.setTextColor(Color.parseColor("#050505"))
        }else{
            show_tag.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
            text_tag.setTextColor(Color.RED)
        }
    }
    private fun setRssiResult(intent: Intent){
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
    private fun setMeterResult(intent: Intent){
        val meter=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:"000000"
        text_meter.text=meter

        upper_cover_main.visibility=View.GONE
        progressbar.visibility=View.GONE
        updateUIForTestPhase(TestPhase.IDLE)
    }
    private fun setCurrentResult(intent: Intent){
        val strCurrent= intent.getStringExtra(BluetoothLeService.EXTRA_DATA) ?: return

        if (loadType==0) {// no load
            text_no_load_current.text = strCurrent
            isCurrentPassNoLoad = strCurrent.toFloat()<=GattAttributes.MAX_NO_LOAD_CURRENT
            if (isCurrentPassNoLoad!!) {
                show_no_load_current.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_no_load_current.setTextColor(Color.parseColor("#050505"))
            } else {
                show_no_load_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_no_load_current.setTextColor(Color.RED)
            }
        }else if (loadType==1){// with load
            text_with_load_current.text=strCurrent
            isCurrentPassWithLoad = isWithInTolerance(GattAttributes.TARGET_CURRENT,toleranceWithLoadCurrent,strCurrent)
            if (isCurrentPassWithLoad!!){
                show_with_load_current.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_with_load_current.setTextColor(Color.parseColor("#050505"))
            }else{
                show_with_load_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_with_load_current.setTextColor(Color.RED)
            }
        }
    }
    private fun setVoltageResult(intent: Intent){
        val strVoltage=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return
        if (loadType==0) {// no load
            text_no_load_voltage.text = strVoltage
            isVoltagePassNoLoad = isWithInTolerance(GattAttributes.TARGET_VOLTAGE,toleranceNoLoadVoltage,strVoltage)
            if (isVoltagePassNoLoad!!) {
                show_no_load_voltage.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_no_load_voltage.setTextColor(Color.parseColor("#050505"))
            } else {
                show_no_load_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_no_load_voltage.setTextColor(Color.RED)
            }
        }else if(loadType==1){// with load
            text_with_load_voltage.text=strVoltage
            isVoltagePassWithLoad= isWithInTolerance(GattAttributes.TARGET_VOLTAGE,toleranceWithLoadVoltage,strVoltage)
            if (isVoltagePassWithLoad!!){
                show_with_load_voltage.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_with_load_voltage.setTextColor(Color.parseColor("#050505"))
            } else {
                show_with_load_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_with_load_voltage.setTextColor(Color.RED)
            }
        }
    }
    private fun setWattResult(intent: Intent){
        val strWatt=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return
        if (loadType==1){// with load
            text_with_load_watt.text = strWatt
            isWattPassWithLoad = isWithInTolerance(
                GattAttributes.TARGET_VOLTAGE*GattAttributes.TARGET_CURRENT,
                toleranceWithLoadVoltage+toleranceWithLoadCurrent,
                strWatt)

            if (isWattPassWithLoad!!) {
                show_with_load_watt.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_with_load_watt.setTextColor(Color.parseColor("#050505"))
            } else {
                show_with_load_watt.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_with_load_watt.setTextColor(Color.RED)
            }
        }
    }
    private fun setPowerFactorResult(intent: Intent){
        val strPowerFactor=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return
        if (loadType==1){// with load
            text_with_load_power_factor.text = strPowerFactor
            isPFPassWithLoad =
                (strPowerFactor.toFloat() in GattAttributes.PF_LOW..GattAttributes.PF_HIGH)
            if (isPFPassWithLoad!!) {
                show_with_load_power_factor.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_with_load_power_factor.setTextColor(Color.parseColor("#050505"))
            } else {
                show_with_load_power_factor.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_with_load_power_factor.setTextColor(Color.RED)
            }
        }
    }
    private fun setLoadResult(intent: Intent){
        if (loadType==1){// with load
            text_with_load_wh.text=intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?:return
        }
    }

    //應該用不到
    private fun setRecordResult(intent: Intent){
        val array=intent.getStringArrayListExtra(BluetoothLeService.EXTRA_DATA)
        if (array!=null){
            val deviceCurrent=array[2]
            val deviceVoltage=array[3]
            val deviceWatt=array[4]
            val devicePowerFactor=array[5]
            val deviceConsumption=array[6]

            isCurrentPassNoLoad=(deviceCurrent.toFloat() in GattAttributes.CURRENT_LOW .. GattAttributes.CURRENT_HIGH)

            text_no_load_current.text=deviceCurrent

            if (isCurrentPassNoLoad!!){
                show_no_load_current.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_no_load_current.setTextColor(Color.parseColor("#050505"))
            }else{
                show_no_load_current.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_no_load_current.setTextColor(Color.RED)
            }

            isVoltagePassNoLoad=(deviceVoltage.toFloat() in GattAttributes.VOLTAGE_LOW .. GattAttributes.VOLTAGE_HIGH)

            text_no_load_voltage.text=deviceVoltage

            if (isVoltagePassNoLoad!!){
                show_no_load_voltage.setImageResource(R.drawable.ic_baseline_check_circle_outline_24)
                text_no_load_voltage.setTextColor(Color.parseColor("#050505"))
            }else{
                show_no_load_voltage.setImageResource(R.drawable.ic_baseline_do_not_disturb_24)
                text_no_load_voltage.setTextColor(Color.RED)
            }

        }
    }
    private fun calculatePercentage(value:Int, percentage:Float, isPlus:Boolean):String{
        val result=if (isPlus) {
            value * (1f + percentage / 100f)
        }else{
            value*(1f-percentage/100f)
        }

        return if (result%1==0.0f){
            result.toInt().toString()
        }else{
            String.format("%.2f",result).trimEnd('0').trimEnd('.')
        }
    }
    private fun isWithInTolerance(targetValue:Int, tolerancePercentage:Float, input:String):Boolean{
        val inputValue=input.toFloatOrNull()?:return false
        val tolerance=targetValue*(tolerancePercentage/100)

        return kotlin.math.abs(inputValue-targetValue)<=tolerance
    }
}