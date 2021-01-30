package com.ahmed.receiver.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ahmed.receiver.R
import com.ahmed.receiver.databinding.ActivityReceiverBinding
import com.ahmed.receiver.viewmodels.ReceiverViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class ReceiverActivity : AppCompatActivity() {

    private lateinit var middleManReceiver: MiddleManReceiver
    private val viewModel: ReceiverViewModel by lazy {
        ViewModelProvider(this, ReceiverViewModel.Factory(application)).get(ReceiverViewModel::class.java)
    }
    private var id = ""
    private var userName = ""
    private var bindingView: ActivityReceiverBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingView = DataBindingUtil.setContentView(this, R.layout.activity_receiver)
        middleManReceiver = MiddleManReceiver()
        val intentFilter = IntentFilter(RECEIVER_APP_ACTION)
        registerReceiver(middleManReceiver, intentFilter)

        bindingView!!.loadButton.setOnClickListener {
            if (id.isNotEmpty()) {
                viewModel.getUserById(id).observe(this, {
                    bindingView!!.user = it
                })
            }
        }

        viewModel.eventDatabaseError.observe(this, {
            if (it) {
                showErrorToast()
            }
        })

    }

    override fun onResume() {
        super.onResume()
        val isNewRequest = intent.getBooleanExtra(EXTRA_NEW_REQUEST_KEY, false)
        if (intent.hasExtra(EXTRA_USER_KEY) && isNewRequest) {
            lifecycleScope.launch {
                bindingView?.progressSpinner?.visibility = View.VISIBLE
                intent.putExtra(EXTRA_NEW_REQUEST_KEY, false)
                val user = viewModel.convertStringJsonToUser(intent.getStringExtra(EXTRA_USER_KEY)!!)
                id = user.id
                userName = user.name
                viewModel.saveUser(user)
                listenToInsertingResult()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { this.intent = it }
    }

    private fun listenToInsertingResult() {
        viewModel.insertingResult.observe(this, {
            when (it) {
                ReceiverViewModel.DatabaseResult.OK -> {
                    bindingView?.cardView?.visibility = View.VISIBLE
                }

                ReceiverViewModel.DatabaseResult.NOK -> {
                    showErrorToast()
                }

                else -> { }
            }

            bindingView?.progressSpinner?.visibility = View.GONE

            val receiverAppIntent = Intent().apply {
                action = MIDDLE_MAN_APP_ACTION
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_INSERTING_RESULT_KEY, it.name)
                putExtra(EXTRA_USER_NAME_KEY, userName)
            }

            sendBroadcast(receiverAppIntent)
        })
    }

    private fun showErrorToast() {
        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::middleManReceiver.isInitialized) {
            unregisterReceiver(middleManReceiver)
        }
    }

    class MiddleManReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val isNotificationClosed = intent.getBooleanExtra(EXTRA_CLOSE_NOTIFICATION_KEY, false)
            if (!isNotificationClosed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    showNotification(context)
                    Timber.i("called")
                }
                val userJson = intent.getStringExtra(EXTRA_USER_KEY)
                val activityIntent = Intent(context, ReceiverActivity::class.java)
                activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activityIntent.putExtra(EXTRA_USER_KEY, userJson)
                activityIntent.putExtra(EXTRA_NEW_REQUEST_KEY, true)
                context.startActivity(activityIntent)
            } else {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_MAIN_ID_NUMBER)
            }
        }

        private fun showNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(NOTIFICATION_MAIN_CHANNEL_ID, "h", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(NOTIFICATION_MAIN_ID_NUMBER, createNotification(context))
        }

        private fun createNotification(context: Context): Notification {
            val closeIntent = Intent(context, MiddleManReceiver::class.java)
            closeIntent.putExtra(NOTIFICATION_MAIN_ID_KEY, NOTIFICATION_MAIN_ID)
            closeIntent.putExtra(EXTRA_CLOSE_NOTIFICATION_KEY, true)
            val closePendingIntent = PendingIntent.getBroadcast(context, 0, closeIntent, 0)

            val builder = NotificationCompat.Builder(context, NOTIFICATION_MAIN_ID)
                    .setSmallIcon(R.drawable.ic_baseline_control_point_24)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.main_notification_content))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(0, context.getString(R.string.close_button), closePendingIntent)

            return builder.build()
        }

    }

    companion object {
        const val MIDDLE_MAN_APP_ACTION = "com.ahmed.middleman.ACTION"
        const val RECEIVER_APP_ACTION = "com.ahmed.receiver.ACTION"
        const val EXTRA_USER_KEY = "com.ahmed.emitter.EXTRA_USER"
        const val EXTRA_INSERTING_RESULT_KEY = "com.ahmed.emitter.EXTRA_INSERTING_RESULT"
        const val EXTRA_USER_NAME_KEY = "com.ahmed.emitter.EXTRA_USER_NAME"
        const val EXTRA_NEW_REQUEST_KEY = "com.ahmed.receiver.EXTRA_NEW_REQUEST"
        const val NOTIFICATION_MAIN_ID =  "com.ahmed.receiver.NOTIFICATION_MAIN_ID"
        const val NOTIFICATION_MAIN_ID_KEY =  "com.ahmed.receiver.NOTIFICATION_MAIN_ID_KEY"
        const val NOTIFICATION_MAIN_ID_NUMBER =  10
        const val NOTIFICATION_MAIN_CHANNEL_ID =  "com.ahmed.receiver.NOTIFICATION_MAIN_CHANNEL_ID"
        const val EXTRA_CLOSE_NOTIFICATION_KEY =  "com.ahmed.receiver.EXTRA_CLOSE_NOTIFICATION_KEY"
    }
}