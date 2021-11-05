package com.example.testf116

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_header.*
import kotlinx.android.synthetic.main.fragment_barcode.*
import kotlinx.android.synthetic.main.fragment_barcode.show_rssi


class MainActivity : AppCompatActivity(),View.OnClickListener {

    private var adapter:BluetoothAdapter?=null
    private var isScanning:Boolean=false
    private var deviceArrayList:ArrayList<BluetoothDevice>?=null
    private var rssiArrayList:ArrayList<Int>?=null
    private var scanner:BluetoothLeScanner?=null
    private var toolbar:Toolbar?=null

    private var address=""
    private var deviceName=""
    private var rssi=0
    private var mBluetoothLeService:BluetoothLeService?=null
    private var isConnected=false

    private lateinit var sharedPreferences: SharedPreferences

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

        sharedPreferences=getSharedPreferences("f116_db", MODE_PRIVATE)

        setToolbar()
        initView()
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()

        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED))

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

                    isConnected=false
                    btn_connection.setText(R.string.disconnected)
                    btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                    mBluetoothLeService?.disconnect()
                }else{
                    isConnected=true
                    btn_connection.setText(R.string.connected)
                    btn_connection.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                    mBluetoothLeService?.connect(address)
                }
            }
            R.id.btn_test->{

            }
            R.id.btn_save->{

            }
            R.id.fail_led_1->{

            }
            R.id.fail_led_2->{

            }
            R.id.fail_led_3->{

            }
            R.id.fail_led_4->{

            }
            R.id.pass_led_1->{

            }
            R.id.pass_led_2->{

            }
            R.id.pass_led_3->{

            }
            R.id.pass_led_4->{

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
    }

    private fun menuIconWithText(drawable: Drawable, title: String): CharSequence {
        drawable.setBounds(0,0,drawable.intrinsicWidth,drawable.intrinsicHeight)
        val sb=SpannableString("   $title")
        val imageSpan=ImageSpan(drawable,ImageSpan.ALIGN_BOTTOM)
        sb.setSpan(imageSpan,0,1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun createDialogHeader() {

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
        dialog.cancel_button.setOnClickListener {
            dialog.dismiss()
        }

        dialog.save_button.setOnClickListener {
            sharedPreferences.edit().putString(GattAttributes.ORDER_SERIAL_1,dialog.order_serial_1.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.ORDER_SERIAL_2,dialog.order_serial_2.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.LOT_NUMBER,dialog.order_serial_lot.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.FIRMWARE,dialog.firmware_number.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.TAG,dialog.tag_number.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.DEVICE_ADDRESS,address).apply()
            sharedPreferences.edit().putString(GattAttributes.RSSI_SMALL,dialog.rssi_small.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.RSSI_LARGE,dialog.rssi_large.text.toString()).apply()
            sharedPreferences.edit().putString(GattAttributes.PRODUCING_TIME,"xxxx").apply()
            sharedPreferences.edit().putString(GattAttributes.TEST_DEP,"1").apply()

            
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

        if (address.isNotEmpty()) {
            dialog.barcode_mac.setImageBitmap(
                codeAsBitmap(
                    address,
                    BarcodeFormat.CODE_128,
                    900,
                    200
                )
            )
            dialog.text_mac.text=address
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
            dialog.show_rssi.text = rssi_text
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

            Log.d("testF116","address ${result.device.address}, name: ${result.device.name}, rssi ${result.rssi}")

            //supportActionBar?.title = address
            //toolbar?.setTitleTextColor(Color.WHITE)

            address=result.device.address
            deviceName=result.device.name
            rssi=result.rssi

            toolbar?.title=address

            scanLeDevice(false)
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
}