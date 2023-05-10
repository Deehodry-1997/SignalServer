package com.dhj.singlingservice.websocket

import com.dhj.singlingservice.bean.*
import com.google.gson.Gson
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint

@Component
@ServerEndpoint("/websocket/{id}")
class WebSocket {
    companion object {
        //存放所有用户连接
        private val connections = ConcurrentHashMap<String, WebSocket>()
        val gson = Gson()
    }
    //与某个用户对话，通过这个发送消息
    private var session: Session? = null
    //连接id
    private var id = ""
    //连接的用户名
    private var name = ""

    //json解析
    @OnOpen
    fun onOpen(session: Session, @PathParam(value = "id") id: String) {
        if(connections.keys.contains(id)){
            session.close(CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "您已登录"))
            return
        }
        this.id = id
        //默认客户端，没有重名
        this.session = session
        connections[id] = this
    }

    @OnClose
    fun onClose() {
        //这里判断不周，仅实验用
        for (name in connections.keys) {
            if (this === connections[name]) {
                connections.remove(name)
                println("【${name}退出成功】当前连接人数为：${ connections.size}")
                connections.values.forEach{
                    it.session?.apply {
                        sendAllMembers(this)
                    }
                }
                break
            }
        }
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        println("error:")
        throwable.printStackTrace()
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        println("【webSocket接收成功】内容为：$message")
        val jsonResult = JSONObject(message)
        when(val act = jsonResult.getString("act")){
            "getMembers" -> {
                sendAllMembers(session)
            }

            "setName" -> {
                name = jsonResult.getString("name")
                println("【webSocket连接成功】当前连接人数为：" + connections.size + "，此人为：" + name)
                connections.values.forEach{
                    it.session?.apply {
                        sendAllMembers(this)
                    }
                }
            }

            "call" -> {
                val callMsgBean = gson.fromJson(message, CallMsgBean::class.java)
                connections.keys.forEach {
                    if(it == callMsgBean.toId){
                        when(callMsgBean.type){
                            "call" -> {
                                callMsgBean.type = "byCall"
                                sendMsg(connections[it]!!.session!!, JSONObject(callMsgBean))
                            }
                            "answer" -> {
                                sendMsg(connections[it]!!.session!!, JSONObject(callMsgBean))
                            }
                            "cancel" -> {
                                sendMsg(connections[it]!!.session!!, JSONObject(callMsgBean))
                            }
                        }
                    }
                }
            }

            "sendSdpMsg" -> {
               val sdpMsgBean = gson.fromJson(message, SdpMsgBean::class.java)
                connections.keys.forEach {
                    if(it == sdpMsgBean.toId){
                        connections[it]?.session?.basicRemote?.sendText(
                            JSONObject().apply {
                                put("act", "receive")
                                put("fromId", id)
                                put("type", sdpMsgBean.type)
                                put("sdp", sdpMsgBean.sdp)
                            }.toString()
                        )
                    }
                }
            }
            "sendCandidate" -> {
                val candidateMsgBean = gson.fromJson(message, CandidateMsgBean::class.java)
                connections.keys.forEach {
                    if(it == candidateMsgBean.toId){
                        connections[it]?.session?.basicRemote?.sendText(
                            JSONObject().apply {
                                put("act", "receiveCandidate")
                                put("fromId", id)
                                put("candidate", candidateMsgBean.candidate)
                                put("sdpMLineIndex", candidateMsgBean.sdpMLineIndex)
                                put("sdpMid", candidateMsgBean.sdpMid)
                            }.toString()
                        )
                    }
                }
            }
        }
    }

    private fun sendAllMembers(session: Session){
        sendMsg(
            session,
            JSONObject(
                MemberMsgBean().apply {
                    this.act = "getMembers"
                    connections.keys.forEach {
                        memberList.add(Member(id = it, name = connections[it]!!.name))
                    }
                }
            )
        )
    }

    private fun sendMsg(session: Session, msg:JSONObject){
        println("给${name}发送${msg}")
        session.basicRemote.sendText(msg.toString())
    }
}