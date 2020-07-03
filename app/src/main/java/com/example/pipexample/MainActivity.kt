package com.example.pipexample

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val PLAY_PAUSE_INTENT_CODE = 2137
private const val RESET_INTENT_CODE = 2138

class MainActivity : AppCompatActivity() {

    private val timerRunning = AtomicBoolean(false)
    private val timerPaused = AtomicBoolean(false)
    private val isInPiP = AtomicBoolean(false)
    private var approxTimePassed: Long = 0

    private val disposables = CompositeDisposable()

    private val timerTickObservable = Observable.interval(100, TimeUnit.MILLISECONDS)
        .doOnSubscribe {
            timerRunning.set(true)
            approxTimePassed = 0
        }
        .doOnDispose { timerRunning.set(false) }
        .filter { !timerPaused.get() }
        .takeWhile { timerRunning.get() }
        .doOnNext {
            approxTimePassed += 100
        }
        .share()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root.setOnClickListener {
            if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                renderPiPMode()
            }
        }

        start_pause_button.setOnClickListener {
            renderUi()
        }

        restart_button.setOnClickListener {
            approxTimePassed = 0
            if (isInPiP.get()) {
                tv.text = "${approxTimePassed / 1000}"
            } else {
                tv.text = "${approxTimePassed / 1000}\nseconds\npassed"
            }
        }

        stop_button.setOnClickListener {
            timerRunning.set(false)
            start_pause_button.setImageResource(R.drawable.ic_play)
            restart_button.alpha = 0.5f
            restart_button.isClickable = false
        }

        MyBroadcastReceiver.playPauseIntent.subscribeBy {
            if (timerRunning.get()) {
                timerPaused.set(!timerPaused.get())
                if (isInPictureInPictureMode) renderPiPMode()
            }
        }

        MyBroadcastReceiver.resetIntent.subscribeBy {
            approxTimePassed = 0
            tv.text = "${approxTimePassed / 1000}"

        }
    }

    override fun onResume() {
        super.onResume()
        renderUi()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        isInPiP.set(isInPictureInPictureMode)
        start_pause_button.isVisible = !isInPiP.get()
        restart_button.isVisible = !isInPiP.get()
        stop_button.isVisible = !isInPiP.get()
        if (isInPiP.get()) {
            tv.text = "${approxTimePassed / 1000}"
        } else {
            tv.text = "${approxTimePassed / 1000}\nseconds\npassed"
        }
    }

    private fun renderUi() {
        if (!timerRunning.get()) {
            timerPaused.set(false)
            disposables.add(
                timerTickObservable.subscribe {
                    if (isInPiP.get()) {
                        tv.text = "${approxTimePassed / 1000}"
                    } else {
                        tv.text = "${approxTimePassed / 1000}\nseconds\npassed"
                    }
                }
            )
            start_pause_button.setImageResource(R.drawable.ic_pause)
            restart_button.alpha = 1f
            restart_button.isClickable = true
            stop_button.alpha = 1f
            stop_button.isClickable = true
        } else if (timerRunning.get() && !timerPaused.get()) {
            start_pause_button.setImageResource(R.drawable.ic_play)
            timerPaused.set(true)
            restart_button.alpha = 0.5f
            restart_button.isClickable = false
        } else if (timerRunning.get() && timerPaused.get()) {
            start_pause_button.setImageResource(R.drawable.ic_pause)
            timerPaused.set(false)
            restart_button.alpha = 1f
            restart_button.isClickable = true
        }
    }

    private fun renderPiPMode() {
        if (!timerRunning.get()) return

        val playPauseAction = RemoteAction(
            Icon.createWithResource(this, if (timerPaused.get()) R.drawable.ic_play else R.drawable.ic_pause),
            "A title",
            "Content Description",
            PendingIntent.getBroadcast(
                this,
                PLAY_PAUSE_INTENT_CODE,
                Intent(this, MyBroadcastReceiver::class.java).apply {
                    action = MyBroadcastReceiver.ACTION_PLAY_PAUSE
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        val resetAction = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_reset),
            "Reset",
            "Content Description",
            PendingIntent.getBroadcast(
                this,
                RESET_INTENT_CODE,
                Intent(this, MyBroadcastReceiver::class.java).apply {
                    action = MyBroadcastReceiver.ACTION_RESET
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        val params = PictureInPictureParams
            .Builder()
            .setActions(listOf(playPauseAction, resetAction))
            .setAspectRatio(Rational(1, 1))
            .build()

        if (isInPictureInPictureMode) {
            setPictureInPictureParams(params)
        } else {
            enterPictureInPictureMode(params)
        }
    }
}
