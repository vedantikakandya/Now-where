package com.example.now_where.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

fun NowTicker(periodMs:Long=10_000L): Flow< LocalDateTime> =flow{
    while(true){
        emit(LocalDateTime.now())
        delay(periodMs)
    }
}

