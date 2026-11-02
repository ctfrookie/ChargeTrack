package com.example.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.tracker.ChargingTracker

class ChargingJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "ChargingJobService onStartJob: device charging detected by system OS")
        val tracker = ChargingTracker.getInstance(applicationContext)
        tracker.checkCurrentHardwareState()
        ChargingForegroundService.start(applicationContext)

        // Reschedule to ensure listener remains registered for subsequent charging cycles
        scheduleJob(applicationContext)
        return false // Work is handed off to foreground service
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "ChargingJobService onStopJob: charging stopped or conditions changed")
        val tracker = ChargingTracker.getInstance(applicationContext)
        tracker.handlePowerDisconnected(intent = null, isManual = false)
        ChargingForegroundService.stop(applicationContext)

        // Reschedule for next plug-in
        scheduleJob(applicationContext)
        return true // Reschedule if needed
    }

    companion object {
        private const val TAG = "ChargingJobService"
        private const val JOB_ID = 2026

        fun scheduleJob(context: Context) {
            try {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                val component = ComponentName(context, ChargingJobService::class.java)

                val jobInfo = JobInfo.Builder(JOB_ID, component)
                    .setRequiresCharging(true) // Native Android OS trigger when charging cable is plugged in!
                    .setPersisted(true) // Survives device reboots!
                    .build()

                val result = jobScheduler.schedule(jobInfo)
                Log.d(TAG, "ChargingJobService scheduled result=$result")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule ChargingJobService: ${e.message}")
            }
        }
    }
}
