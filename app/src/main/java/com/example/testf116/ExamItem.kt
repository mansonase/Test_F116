package com.example.testf116

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ExamItem :RealmObject(){

    @PrimaryKey
    var keyIndex:Long=0
    get() {return field}
    set(value) {field=value}

    //serial number
    var serialNumber:Int=-1
    get() {return field}
    set(value) {field=value}

    //test count
    var testNumber:Int=-1
    get() {return field}
    set(value) {field=value}

    //Lot
    var productLotNumber:String=""
    get() {return field}
    set(value) {field=value}

    //firmware number
    var firmwareNumber:String=""
    get() {return field}
    set(value) {field=value}

    //Mac address
    var macAddress:String=""
    get() {return field}
    set(value) {field=value}

    //Tag Number
    var tagNumber:String=""
    get() {return field}
    set(value) {field=value}

    //dBM
    var rssi:Int=0
    get() {return field}
    set(value) {field=value}

    //current
    var current:Float=0f
    get() {return field}
    set(value) {field=value}

    //voltage
    var voltage:Float=0f
    get() {return field}
    set(value) {field=value}

    //watt
    var watt:Float=0f
    get() {return field}
    set(value) {field=value}

    //power factor
    var powerFactor:Float=0f
    get() {return field}
    set(value) {field=value}

    //watt_hour
    var wattHour:Float=0f
    get() {return field}
    set(value) {field=value}

    //LED blue flash
    var isLEDBlueFlash:Boolean=false
    get() {return field}
    set(value) {field=value}

    //LED blue
    var isLEDBlueOn:Boolean=false
    get() {return field}
    set(value) {field=value}

    //LED green flash
    var isLEDGreenOn:Boolean=false
    get() {return field}
    set(value) {field=value}

    //LED red on
    var isLEDRedOn:Boolean=false
    get() {return field}
    set(value) {field=value}

    //result
    var result:Boolean=false
    get() {return field}
    set(value) {field=value}

    //testing time start
    var startTime:Long=0L
    get() {return field}
    set(value) {field=value}

    //testing time end
    var endTime:Long=0L
    get() {return field}
    set(value) {field=value}

    var aa24Timestamp:Long=0L
    get() {return field}
    set(value) {field=value}
}