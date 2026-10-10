package com.hkr.datalogger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.bluetooth.BluetoothDevice
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException

/** A single RFCOMM connection. Closing sockets also cancels blocking connect/read/accept. */
@SuppressLint("MissingPermission")
class BluetoothSession(context: Context, private val adapter: BluetoothAdapter,
                       private val onStatus: (String, Boolean) -> Unit,
                       private val onLine: (String) -> Unit) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val writer = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue<Runnable>(64))
    private val lock = Any()
    private var generation = 0
    private var socket: BluetoothSocket? = null
    private var server: BluetoothServerSocket? = null
    private var closed = false

    private fun publish(id: Int, status: String, connected: Boolean = false) {
        main.post { if (synchronized(lock) { !closed && generation == id }) onStatus(status, connected) }
    }

    fun connect(address: String) = start { id ->
        val device = adapter.getRemoteDevice(address)
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            publish(id, "Pair the device before connecting")
            return@start
        }
        if (Build.VERSION.SDK_INT < 31 || appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            adapter.cancelDiscovery()
        }
        val candidate = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
        synchronized(lock) {
            if (generation != id || closed) { candidate.close(); return@start }
            socket = candidate
        }
        publish(id, "Connecting…")
        candidate.connect()
        receive(id, candidate)
    }

    fun listen() = start { id ->
        val listener = adapter.listenUsingRfcommWithServiceRecord("DataLogger", SERVICE_UUID)
        synchronized(lock) {
            if (generation != id || closed) { listener.close(); return@start }
            server = listener
        }
        publish(id, "Waiting for a master…")
        val accepted = listener.accept()
        synchronized(lock) {
            if (generation != id || closed) { accepted.close(); return@start }
            socket = accepted
            listener.close()
            server = null
        }
        receive(id, accepted)
    }

    private fun start(action: (Int) -> Unit) {
        val id = synchronized(lock) {
            if (closed) return
            generation++
            closeSockets()
            generation
        }
        worker.execute {
            try { action(id) }
            catch (e: IOException) { publish(id, "Connection ended: ${e.message}") }
            catch (e: SecurityException) { publish(id, "Bluetooth permission is required") }
            catch (e: IllegalArgumentException) { publish(id, "Invalid Bluetooth address") }
            finally { synchronized(lock) { if (generation == id) closeSockets() } }
        }
    }

    private fun receive(id: Int, connection: BluetoothSocket) {
        publish(id, "Connected", true)
        val input = connection.inputStream
        val framer = AsciiLineFramer()
        while (true) {
            val byte = input.read()
            if (byte == -1) { publish(id, "Peer disconnected"); return }
            val text = framer.accept(byte)
            if (text != null) {
                // File I/O stays on the worker, never the UI thread.
                appContext.openFileOutput("received.log", Context.MODE_APPEND).use {
                    it.write("${System.currentTimeMillis()}\t$text\n".toByteArray(Charsets.UTF_8))
                }
                main.post { if (synchronized(lock) { !closed && generation == id }) onLine(text) }
            }
        }
    }

    fun send(text: String) {
        require(text.length <= 8192 && text.all { it.code in 32..126 }) { "Use one ASCII command (max 8192 characters)" }
        val (id, connection) = synchronized(lock) { generation to socket }
        if (connection == null) { publish(id, "Connect before sending"); return }
        try { writer.execute {
            try {
                synchronized(lock) { if (closed || id != generation) return@execute }
                connection.outputStream.write("$text\n".toByteArray(Charsets.US_ASCII))
                connection.outputStream.flush()
            } catch (e: IOException) {
                synchronized(lock) { if (id == generation) closeSockets() }
                publish(id, "Send failed: ${e.message}")
            }
        } } catch (_: RejectedExecutionException) {
            disconnect()
        }
    }

    fun discardPendingWrites() { writer.queue.clear() }

    fun disconnect() {
        val id = synchronized(lock) { generation++; closeSockets(); generation }
        publish(id, "Disconnected")
    }

    private fun closeSockets() {
        writer.queue.clear()
        try { socket?.close() } catch (_: IOException) { }
        try { server?.close() } catch (_: IOException) { }
        socket = null
        server = null
    }

    fun close() {
        synchronized(lock) { closed = true; generation++; closeSockets() }
        worker.shutdownNow()
        writer.shutdownNow()
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("c61467fb-8db7-4793-b5bb-42cfacb0184e")
    }
}
