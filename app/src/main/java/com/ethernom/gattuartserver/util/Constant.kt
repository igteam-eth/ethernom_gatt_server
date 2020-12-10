package com.ethernom.gattuartserver.util

val SERVER_STATUS = "com.ethernom.gattuartserver.server_status"
val DEVICE_CHANGE = "com.ethernom.gattuartserver.device_change"
val DATA_SEND = "com.ethernom.gattuartserver.data_send"
val DATA_RECEIVE = "com.ethernom.gattuartserver.data_receive"

object INTENT {
    val DEVICE = "device"
    val STATUS = "status"
    val DATA = "data"
}

enum class State {
    INIT_0000,
    CARD_1000,
    APP_4000
}