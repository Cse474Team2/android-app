package app.bluetooth.proximity.proximitybluetooth

import android.app.*
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
import android.content.Context
import android.graphics.Color
import kotlinx.android.synthetic.main.content_main.*
import java.time.LocalDateTime
import java.time.ZoneOffset


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var bluetoothInputStream: InputStream
    private lateinit var mNotificationManager: NotificationManager
    private var threadRun = false

    private val CHANNEL_ID = "pre_bump_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = "Pre-Bump Notification"
        val description = "You're about to bump into something!"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)

        // Configure the notification channel.
        mChannel.description = description
        mChannel.enableLights(true)
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(true)
        mChannel.vibrationPattern = longArrayOf(200)
        mNotificationManager.createNotificationChannel(mChannel)

        textView.text = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toString()
        mNotificationManager.notify(1001, getNotification("Bump", "Welcome to the bump app!").build())
        textView.textSize = 48.0F
        textView.text = "I'm ready to start recording data!"
        textView.textSize = 48.0F

        reconnect.setOnClickListener {
            connectBluetooth()
        }
    }

    fun connectBluetooth() {
        try {
            findBluetoothDevice()
            openBluetoothConnection()
            listenBluetoothConnection()
            reconnect.text = "Connected"
            reconnect.isEnabled = false
        } catch (e: Exception) {
            Log.e("ERROR", e.message)
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        connectBluetooth()
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
                var postTime = 0L
                runOnUiThread({
                    mNotificationManager.cancelAll()
                    mNotificationManager.notify(1001, getNotification("Bump", "You're about to bump into something!!").build())
                    textView.setTextColor(Color.RED)
                    textView.text = line
                })
            }
        }).start()
    }

    private fun getNotification(title: String, body: String): Notification.Builder {
        return Notification.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
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
