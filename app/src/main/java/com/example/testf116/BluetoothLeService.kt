package com.example.testf116

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class BluetoothLeService():Service() {

    private val TAG:String=BluetoothLeService::class.java.simpleName
    private var manager:BluetoothManager?=null
    private var adapter:BluetoothAdapter?=null
    private var mAddress:String?=null
    private var gatt:BluetoothGatt? = null

    companion object{
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

        const val CHARACTERISTIC="com.example.bluetooth.le.characteristic"
    }


    inner class LocalBinder : Binder() {
        fun getService():BluetoothLeService{
            return this@BluetoothLeService
        } }
    private val mBinder=LocalBinder()
    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    fun initialize():Boolean{
        if (manager==null){
            manager=getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (manager==null){
                Log.e(TAG,"unable to initialize manager")
                return false
            }
        }
        adapter=manager!!.adapter
        if (adapter==null){
            Log.e(TAG,"unable to obtain an adapter")
            return false
        }
        return true
    }


    private val mGattCallback= object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            var intentAction:String
            if (newState==BluetoothProfile.STATE_CONNECTED){
                intentAction= ACTION_GATT_CONNECTED
                broadcastUpdate(intentAction)
                Log.d(TAG,"connect to GATT server")
                Log.d(TAG,"start to service discovery : ${gatt!!.discoverServices()}")

                /*
                Handler(Looper.getMainLooper()).postDelayed(
                    Runnable {
                        Log.d(TAG,"start to service discovery : ${gatt!!.discoverServices()}")
                    },600
                )

                 */

            }else if (newState==BluetoothProfile.STATE_DISCONNECTED){

                intentAction= ACTION_GATT_DISCONNECTED
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            Log.d(TAG,"in onServicesDiscovered, $status")
            if (status==BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Log.d(TAG," on services discovered, after start to service discovery")
            }else{
                Log.d(TAG," onServicesDiscovered received : $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (characteristic==null)return

            if (characteristic.uuid== UUID.fromString(GattAttributes.set_time)){
                broadcastUpdateTime(ACTION_DATA_AVAILABLE,characteristic)
                Log.d(TAG,"write successfully , goes to update time")
                return
            }
            if (characteristic.uuid== UUID.fromString(GattAttributes.charging_latency)){
                broadcastUpdateLatency(ACTION_DATA_AVAILABLE,characteristic)
                return
            }


            broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic)
            Log.d(TAG,"write successfully , ${characteristic.value?.get(0)?.toInt()}")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status==BluetoothGatt.GATT_SUCCESS){
                broadcastRssi(ACTION_DATA_AVAILABLE,rssi)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            if (characteristic==null)return

            if (status==BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic)
                //Log.d(TAG,"read successfully, ${characteristic.getStringValue(0)}")
                Log.d(TAG,"read successfully, ${ characteristic.value.size}")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)

            if (characteristic==null){
                Log.d(TAG,"characteristic is....null!!!")
                return
            }

            Log.d(TAG,"characteristic changed")
            broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic)
        }
    }

    private fun broadcastUpdate(action: String){
        val intent=Intent(action)
        sendBroadcast(intent)
    }
    private fun broadcastUpdateTime(action: String,characteristic: BluetoothGattCharacteristic){

        val intent=Intent(action)


        val data = ((characteristic.value[0].toLong() and (0xFF))*256*65536+
                    (characteristic.value[1].toLong() and (0xFF))*256*256+
                    (characteristic.value[2].toLong() and (0xFF))*256+
                    (characteristic.value[3].toLong() and (0xFF)))

        val time=getDate(data)
        intent.putExtra(EXTRA_DATA, time)
        intent.putExtra(CHARACTERISTIC,GattAttributes.mSetTime)
        sendBroadcast(intent)
        Log.d(TAG, time)
    }
    private fun broadcastUpdateLatency(action: String,characteristic: BluetoothGattCharacteristic){
        val intent=Intent(action)

        val data=((characteristic.value[0].toInt() and (0xFF))*5).toString()

        intent.putExtra(EXTRA_DATA,data)
        intent.putExtra(CHARACTERISTIC,GattAttributes.mChargingLatencySend)
        sendBroadcast(intent)
    }
    private fun broadcastRssi(action: String,rssi:Int){
        val intent=Intent(action)
        intent.putExtra(EXTRA_DATA,rssi)
        intent.putExtra(CHARACTERISTIC,GattAttributes.mRssi)
        sendBroadcast(intent)
    }
    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic){
        val intent=Intent(action)

        Log.d(TAG,characteristic.uuid.toString())
        when(characteristic.uuid.toString()){

            GattAttributes.device_name->{
                val data= characteristic.getStringValue(0)
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mDeviceName)
            }
            GattAttributes.appearance->{
                var data="0x "+characteristic.value.toHexString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mAppearance)
                Log.d(TAG, data)
            }
            GattAttributes.peripheral_preferred_connection_parameters->{
//16,0,60,0,0,0,-112,1
                val data="0x "+characteristic.value.toHexString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mPeripheralParameters)
                Log.d(TAG, data)
            }
            GattAttributes.device_id->{

                var data="0x "+characteristic.value.toHexString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mDeviceId)
                Log.d(TAG, data)
            }
            GattAttributes.manufacturer_name_string->{
                val data=characteristic.getStringValue(0)
                val size=characteristic.value.size
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mManufacturerNameString)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.model_number_string->{
                val data=characteristic.getStringValue(0)
                val size=characteristic.value.size
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mModelNumberString)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.serial_number_string->{
                val data=characteristic.getStringValue(0)
                val size=characteristic.value.size
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mSerialNumberString)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.firmware_revision_string->{
                val data=characteristic.getStringValue(0)
                val size=characteristic.value.size
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mFirmwareRevision)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.hardware_revision_string->{
                val data=characteristic.getStringValue(0)
                val size=characteristic.value.size
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mHardwareRevision)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.software_revision_string->{
                val data=characteristic.getStringValue(0)
                val size=characteristic.value.size
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mSoftwareRevision)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.current->{

                val data =  (((characteristic.value[0].toInt() and (0xFF))*256*256+
                             (characteristic.value[1].toInt() and (0xFF))*256+
                             (characteristic.value[2].toInt() and (0xFF))).toFloat()/1000f).toString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mCurrent)
                Log.d(TAG, "$data, current")
            }
            GattAttributes.voltage->{

                //Log.d(TAG,(characteristic.value[0].toString(16))+","+(characteristic.value[1].toString(16))+","+(characteristic.value[2].toString(16)))
                //Log.d(TAG,(characteristic.value[0].toInt()and (0xFF)).toString(16)+","+(characteristic.value[1].toInt()and (0xFF)).toString(16)+","+(characteristic.value[2].toInt()and (0XFF)).toString(16))
                val data = (((characteristic.value[0].toInt() and (0xFF)).toDouble()*256*256+
                             (characteristic.value[1].toInt() and (0xFF)).toDouble()*256+
                             (characteristic.value[2].toInt() and (0xFF)).toDouble())/1000).toString()

                Log.d(TAG, "$data, voltage")

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mVoltage)

            }
            GattAttributes.watt->{

                val data = (((characteristic.value[0].toInt() and (0xFF)).toDouble()*256*256+
                             (characteristic.value[1].toInt() and (0xFF)).toDouble()*256+
                             (characteristic.value[2].toInt() and (0xFF)).toDouble())/1000).toString()

                Log.d(TAG, "$data, watt")
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mWatt)
            }
            GattAttributes.power_factor->{
                val data = (((characteristic.value[0].toInt() and (0xFF)).toDouble()*256+
                             (characteristic.value[1].toInt() and (0xFF)).toDouble())/10_000).toString()

                Log.d(TAG, "$data, power factor")
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mPowerFactor)
            }
            GattAttributes.load->{
                val data=((characteristic.value[0].toInt() and (0xFF)).toDouble()*256*256+
                          (characteristic.value[1].toInt() and (0xFF)).toDouble()*256+
                          (characteristic.value[2].toInt() and (0xFF)).toDouble()).toString()
                Log.d(TAG, "$data, load")
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mLoad)
            }
            GattAttributes.load_detected->{
                val data=(characteristic.value[0].toInt() and (0xFF))
                Log.d(TAG, "$data, load detected")
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mLoadDetected)
            }
            GattAttributes.activate_power->{
                val data=(characteristic.value[0].toInt() and (0xFF))

                if (data==1){

                    intent.putExtra(EXTRA_DATA, "On")
                    intent.putExtra(CHARACTERISTIC,GattAttributes.mPowerOn)
                    Log.d(TAG, "$data, activate power")
                }else if (data==0){
                    intent.putExtra(EXTRA_DATA, "Off")
                    intent.putExtra(CHARACTERISTIC,GattAttributes.mPowerOff)
                    Log.d(TAG, "$data, activate power")

                }

            }
            GattAttributes.set_time->{
                val data = ((characteristic.value[0].toLong() and (0xFF))*256*65536+
                            (characteristic.value[1].toLong() and (0xFF))*256*256+
                            (characteristic.value[2].toLong() and (0xFF))*256+
                            (characteristic.value[3].toLong() and (0xFF)))

                val time=getDate(data)
                intent.putExtra(EXTRA_DATA, time)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mGetTime)
                Log.d(TAG, time)
            }
            GattAttributes.download_request -> {

                val data = (characteristic.value[0].toInt() and (0xFF))

                if (data==1){

                    intent.putExtra(EXTRA_DATA,"On")
                    intent.putExtra(CHARACTERISTIC,GattAttributes.mDownloadOn)
                }
                Log.d(TAG," data is $data")
            }
            GattAttributes.read_recorded_data -> {

                val array=ArrayList<String>()

                val startTimeData=getFlatDate((characteristic.value[0].toLong() and (0xFF))*256*65536+
                                                       (characteristic.value[1].toLong() and (0xFF))*256*256+
                                                       (characteristic.value[2].toLong() and (0xFF))*256+
                                                       (characteristic.value[3].toLong() and (0xFF)))

                val endTimeData=getFlatDate((characteristic.value[4].toLong() and (0xFF))*256*65536+
                                                     (characteristic.value[5].toLong() and (0xFF))*256*256+
                                                     (characteristic.value[6].toLong() and (0xFF))*256+
                                                     (characteristic.value[7].toLong() and (0xFF)))

                val currentData=(((characteristic.value[8].toLong() and (0xFF))*256*256+
                                  (characteristic.value[9].toLong() and (0xFF))*256+
                                  (characteristic.value[10].toLong() and (0xFF))).toFloat()/1000f).toString()

                val voltageData=(((characteristic.value[11].toLong() and (0xFF))*256*256+
                                  (characteristic.value[12].toLong() and (0xFF))*256+
                                  (characteristic.value[13].toLong() and (0xFF))).toFloat()/1000f).toString()

                val powerData=(((characteristic.value[14].toLong() and (0xFF))*256*256+
                                (characteristic.value[15].toLong() and (0xFF))*256+
                                (characteristic.value[16].toLong() and (0xFF))).toFloat()/1000f).toString()

                val powerFactorData=(((characteristic.value[17].toLong() and (0xFF))*256+
                                      (characteristic.value[18].toLong() and (0xFF))).toFloat()/10_000f).toString()

                val consumptionData=(((characteristic.value[19].toLong() and (0xFF))*256*256+
                                      (characteristic.value[20].toLong() and (0xFF))*256+
                                      (characteristic.value[21].toLong() and (0xFF))).toFloat()/1000f).toString()

                val noneData=((characteristic.value[22].toLong() and (0xFF))*256+
                              (characteristic.value[23].toLong() and (0xFF))).toString()

                array.add(startTimeData)
                array.add(endTimeData)
                array.add(currentData)
                array.add(voltageData)
                array.add(powerData)
                array.add(powerFactorData)
                array.add(consumptionData)
                array.add(noneData)

                intent.putStringArrayListExtra(EXTRA_DATA,array)

                intent.putExtra(CHARACTERISTIC, GattAttributes.mReadRecordedData)

                var data=characteristic.value.toHexString()

                Log.d(TAG,"${characteristic.value.size}")
                Log.d(TAG,data)
                Log.d(TAG, "$startTimeData, $endTimeData, $currentData, $voltageData, $powerData, $powerFactorData, $consumptionData, $noneData")
            }
            GattAttributes.nfc_tag_id->{


                var data=""
                for (i in characteristic.value.indices){
                    if (characteristic.value[i].toInt()!=0) {
                        data += characteristic.value[i].toChar()
                    }
                }

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mNfcTagId)

                Log.d(TAG,"newTAG $data")
            }
            GattAttributes.charging_latency->{
                //for read only
                var data=""
                val size=characteristic.value.size
                for (i in 0 until size){
                    data+= ((characteristic.value[i].toInt() and (0xFF))*5).toString()
                }
                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mChargingLatencyRead)
                Log.d(TAG, "$data, size is $size")
            }
            GattAttributes.hardware_status->{
                var data="0x "+characteristic.value.toHexString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mHardwareStatus)
                Log.d(TAG, data)
            }
            GattAttributes.software_status->{
                var data="0x "+characteristic.value.toHexString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mSoftwareStatus)
                Log.d(TAG, data)
            }
            GattAttributes.error_code->{
                var data="0x "+characteristic.value.toHexString()

                intent.putExtra(EXTRA_DATA, data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mErrorCode)
                Log.d(TAG, data)
            }
            GattAttributes.machine_status->{
                val data=characteristic.value.toHexString()
                intent.putExtra(EXTRA_DATA,data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mMachineStatus)
            }
            GattAttributes.meter_parameter->{
                //val data=characteristic.value.toHexString()
                var data=""
                val format=DecimalFormat("00")
                for (i in 0 until 3){
                    data+=format.format(characteristic.value[i].toString(16).toInt())
                }
                Log.d("meterparameter","$data......")
                intent.putExtra(EXTRA_DATA,data)
                intent.putExtra(CHARACTERISTIC,GattAttributes.mMeterVersion)
            }
        }
        sendBroadcast(intent)
    }

    fun connect(address:String):Boolean{
        if (adapter==null||address==null){
            return false
        }
        if (mAddress!=null&& address == mAddress &&gatt!=null){
            Log.d(TAG,"trying to use an existing gatt for connection, $mAddress")
            return gatt!!.connect()
        }

        val device=adapter!!.getRemoteDevice(address)
        if (device==null){
            Log.d(TAG," no devices")
            return false
        }
        Log.d(TAG,"finally , we need to connect here ");
        gatt=device.connectGatt(this,false,mGattCallback)
        mAddress=address

        Log.d(TAG,"$mAddress, connect")
        return true
    }
    fun disconnect(){
        if (adapter==null||gatt==null){
            return
        }
        gatt!!.disconnect()

    }

    private fun close(){
        if (gatt==null){
            return
        }
        gatt!!.close()
        gatt = null
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, array:ByteArray):Boolean{
        if (adapter==null||gatt==null){
            return false
        }
        //characteristic.value = array
        val isSet=characteristic.setValue(array)
        Log.d(TAG,"in writeC,set? $isSet")
        val isSend=gatt!!.writeCharacteristic(characteristic)
        Log.d(TAG,"in writeC,send? $isSend")
        return true
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic){
        if (adapter==null||gatt==null){
            return
        }
        gatt!!.readCharacteristic(characteristic)
    }

    fun readRemoteRssii(){
        if (adapter==null||gatt==null){
            return
        }
        gatt!!.readRemoteRssi()
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,enabled:Boolean){
        if (adapter==null||gatt==null){
            return
        }
        gatt!!.setCharacteristicNotification(characteristic,enabled)
        if (characteristic.uuid!=null){
            val descriptor=characteristic.getDescriptor(UUID.fromString(GattAttributes.CONFIG))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt!!.writeDescriptor(descriptor)

        }
    }

    fun disableCharacteristicNotification(characteristic: BluetoothGattCharacteristic){
        if (adapter==null||gatt==null){
            return
        }
        gatt!!.setCharacteristicNotification(characteristic,false)
        if (characteristic.uuid!=null){
            val descriptor=characteristic.getDescriptor(UUID.fromString(GattAttributes.CONFIG))
            descriptor.value=BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt!!.writeDescriptor(descriptor)
        }
    }

    fun getSupportedGattServices():List<BluetoothGattService>?{
        if (gatt==null)
            return null

        return gatt!!.services
    }
    fun getSupportedGattService(strUUID: String): BluetoothGattService? {
        if (gatt==null)return null

        return gatt!!.getService(UUID.fromString(strUUID))
    }
    private fun getNoMoreThanTwoDigits(number:Double):String{
        val format=DecimalFormat("0.##")
        format.roundingMode=RoundingMode.FLOOR
        return format.format(number)
    }

    private fun getDate(seconds :Long):String{
        val formatter=SimpleDateFormat("yyyyMMdd\nHH:mm:ss")

        val calendar=Calendar.getInstance()
        calendar.timeInMillis=seconds*1000
        return formatter.format(calendar.time)
    }

    private fun getFlatDate(seconds: Long):String{
        val format=SimpleDateFormat("yyyyMMdd HH:mm:ss")

        val calendar=Calendar.getInstance()
        calendar.timeInMillis=seconds*1000
        return format.format(calendar.time)
    }

    private fun getTwoDigits(number: Int):String{
        val format=DecimalFormat("00")
        return format.format(number)
    }

    @JvmOverloads
    fun ByteArray.toHexString(separator:CharSequence=" ")=
        this.joinToString(separator) {
            String.format("%02X",it)
        }

}