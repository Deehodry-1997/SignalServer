package com.dhj.singlingservice.bean

class CallMsgBean: MsgBean() {
    var fromId: String = ""
    var toId: String = ""
    //call拨打,byCall被拨打,接听answer
    var type: String = ""
    var status: Boolean = false
    var msg: String = ""
}