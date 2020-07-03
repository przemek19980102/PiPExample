package com.example.pipexample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.rxjava3.subjects.PublishSubject

class MyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (it.action == ACTION_PLAY_PAUSE) playPauseIntent.onNext(Unit)
            if (it.action == ACTION_RESET) resetIntent.onNext(Unit)
        }
    }

    companion object {
        @JvmStatic
        val playPauseIntent: PublishSubject<Unit> = PublishSubject.create()
        @JvmStatic
        val resetIntent: PublishSubject<Unit> = PublishSubject.create()
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_RESET = "ACTION_RESET"
    }
}