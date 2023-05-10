package com.dhj.singlingservice.bean

class MemberMsgBean : MsgBean() {
    val memberList = ArrayList<Member>()
}

data class Member(
    var id: String = "",
    var name: String = ""
)