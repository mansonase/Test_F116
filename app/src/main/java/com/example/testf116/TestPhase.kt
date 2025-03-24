package com.example.testf116

enum class TestPhase {
    IDLE,        // 未開始
    NO_LOAD,     // 空載測試
    NO_LOAD_DONE, //空載測試結束
    WITH_LOAD,   // 帶載測試
    WITH_LOAD_DONE,//帶載測試結束+LED測試
    COMPLETED,    // 完成測試
    RESET,        //仍連線, 但reset所有result
    DISCONNECT    //已斷線,reset所有result,並蓋住大部份區域
}