package com.hkr.sensorcenter.Bluetooth

import android.app.Activity
import android.content.Context

class BasicArgs(
    private val btContext : Context,
    // I'll assign as I go
    private val btActivity: Activity
) {
    fun getContext ():Context{
        return btContext
    }
    // I'm a little hesitant on this just because I don't really understand activity yet but i guess once this crashes ill learn more

    fun btActivity (): Activity{
        return btActivity
    }
    // i think there is something about intent but that can be dealt with late rbecause i think that there is a work-around for the intent part, and the fact that it is not really stable (as in it has variables different things have different intents)
    // same thing for socket I need to think about how to deal with it if the socket part of the problem is empty since this will be initialized even before the connection
}