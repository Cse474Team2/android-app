package app.bluetooth.proximity.proximitybluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.util.Log
import java.io.InputStream
import java.util.*


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

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

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
        // \n as a byte
        val delimiter: Byte = 10

        // Just choosing 128 as the max length of a line for now
        var readBuffer = ByteArray(128)
        var bufferPosition = 0

        threadRun = true
        Thread({
            while(true) {
                // Stop thread
                if (!threadRun) break

                val bytesAvailable = bluetoothInputStream.available()
                // Skip if there is nothing to read
                if (bytesAvailable == 0) continue

                val buffer = ByteArray(bytesAvailable)
                bluetoothInputStream.read(buffer)

                for (byte in buffer) {
                    if (byte == delimiter) {
                        Log.d("Proximity Measurement", readBuffer.copyOf(bufferPosition).contentToString())
                        bufferPosition = 0
                        readBuffer = ByteArray(128)
                    } else {
                        Log.d("BYTE", byte.toString())
                        readBuffer[bufferPosition++] = byte
                    }
                }
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
