package com.example.testf116

data class F116Data(//22個
    val keyIndex:Long=0L,
    val serialNumber:Int=-1,
    val testNumber:Int=-1,
    val productionNumber:String="",
    val firmwareNumber:String="",
    val macAddress:String="",
    val tagNumber:String="",
    val rssi:Int=0,
    val current:Float=0f,
    val voltage:Float=0f,
    val watt:Float=0f,
    val powerFactor:Float=0f,
    val wattHour:Float=0f,
    val isLEDBlueFlash:Boolean=false,
    val isLEDBlueOn:Boolean=false,
    val isLEDGreenOn:Boolean=false,
    val isLEDRedOn:Boolean=false,
    val meter:String="",
    val result: Boolean=false,
    val startTime:Long=0L,
    val endTime:Long=0L,
    val aa24Timestamp:Long=0L
)
