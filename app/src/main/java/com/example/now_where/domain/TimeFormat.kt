package com.example.now_where.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeNotation{ H12,H24}

object TimeFormat {
    private val h24= DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val h12= DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    fun clock(time: LocalDateTime,notation:TimeNotation):String=
        time.format(
            if(notation == TimeNotation.H24){
            h24
        }else{
            h12
        }
        )

    fun duration (totalMinutes:Long):String{
        val minutes=totalMinutes.coerceAtLeast(0)
        val h=minutes/60
        val m=minutes%60

        return when {
            h==0L -> "${m}m"
            m==0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}