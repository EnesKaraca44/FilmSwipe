package com.enes.movyswipe.data

sealed class JoinStatus {
    object IDLE : JoinStatus()
    object LOADING : JoinStatus()
    data class FAILURE(val message: String) : JoinStatus()
}
