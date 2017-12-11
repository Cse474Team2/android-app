package app.bluetooth.proximity.proximitybluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.util.Log
import java.io.InputStream
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import kotlinx.android.synthetic.main.content_main.*
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.provider.Settings
import android.support.v4.app.NotificationCompat


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var bluetoothInputStream: InputStream
    private var threadRun = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = "pre_bump_channel"
        val name = "Pre-Bump Notification"
        val description = "You're about to bump into something!"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val mChannel = NotificationChannel(id, name, importance)

        // Configure the notification channel.
        mChannel.description = description
        mChannel.enableLights(true)
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(true)
        mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        mNotificationManager.createNotificationChannel(mChannel)

        // The id of the channel.
        val CHANNEL_ID = "my_channel_01"
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.new_mail)
                .setContentTitle("My notification")
                .setContentText("Hello World!")
        // Creates an explicit intent for an Activity in your app
        val resultIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your app to the Home screen.
        val stackBuilder = TaskStackBuilder.create(this)
        //  Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        mBuilder.setContentIntent(resultPendingIntent)

        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        mNotificationManager.notify(99, mBuilder.build())

        textView.text = "I'm ready to start recording data!"
        textView.setTextSize(48.0F)
    }

    override fun onResume() {
        super.onResume()

        try {
            findBluetoothDevice()
            openBluetoothConnection()
            listenBluetoothConnection()
        } catch (e: Exception) {
            Log.e("ERROR", e.message)
            e.printStackTrace()
        }
    }

    private fun findBluetoothDevice() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isEnabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
        }

        // TODO: don't hardcode the device name
        val devices = bluetoothAdapter.bondedDevices
        for (device in devices) {
            if (device.name == "TEST") {
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
                break
            }
        }
    }

    private fun openBluetoothConnection() {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
        bluetoothSocket.connect()
        bluetoothInputStream = bluetoothSocket.inputStream
    }

    private fun listenBluetoothConnection() {
        val reader = bluetoothInputStream.bufferedReader()
        threadRun = true
        Thread({
            while (threadRun) {
                val line = reader.readLine()
                Log.d("Proximity Measurement", line)
                runOnUiThread({
                    if (line.toFloat() > 300) {
                        textView.setTextColor(Color.BLUE)
                    } else {
                        textView.setTextColor(Color.RED)
                    }
                    textView.text = line
                })
            }
        }).start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
