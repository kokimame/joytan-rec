package com.joytan.rec.main

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.*
import android.os.Process
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.joytan.rec.data.QRecStorage
import java.io.File
import java.io.IOException

class ShareHandler(private val storage: QRecStorage) {
    private var track: AudioTrack? = null

    private var thread: Thread? = null

    val fStorage = FirebaseStorage.getInstance()
    val fStorageRef = fStorage.reference

    val fDatabase = FirebaseDatabase.getInstance()
    val fDatabaseRef = fDatabase.reference

    val mAuth = FirebaseAuth.getInstance()

    val outputFileName = Environment.getExternalStorageDirectory().absolutePath + "/joytan_rec_temp.wav"
    val outputFile = Uri.fromFile(File(outputFileName))

    fun onResume() {}

    fun share(fileName: String, onEndListener: () -> Unit) {

        val audioRef = fStorageRef.child(fileName)
        stop()

        if (storage.length == 0) {
            // If packet length zero, end immediately
            onEndListener()
            return
        }


        track = AudioTrack(AudioManager.STREAM_MUSIC,
                RecordHandler.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                storage.packetSize * 2, AudioTrack.MODE_STREAM)

        thread = Thread(Runnable {
            // Without this, the first few packets is clipped...
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            try {
                storage.save(outputFileName)

                audioRef.putFile(outputFile)
                        .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                            override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot) {
                                Log.i(MainActivity.INFO_TAG, "user upload audio")
                                val result = mapOf("file" to fileName)

                                val clientUid = MainActivity.clientUid

                                val newKey = fDatabaseRef.child("users").
                                        child(clientUid).child("audio").push().key
                                // First write this will probably be removed
                                val childUpdates = HashMap<String, Any>()
                                childUpdates["users/$clientUid/audio/all/$newKey"] = result
                                fDatabaseRef.updateChildren(childUpdates)
                                        .addOnCompleteListener{
                                            Log.i(MainActivity.INFO_TAG, "DB: Wrote at users/$clientUid/audio/$newKey")
                                        }
                                        .addOnFailureListener {
                                            Log.i(MainActivity.INFO_TAG, "DB: Failed to write users/$clientUid/audio/$newKey")
                                        }
                                // Second write
                                val projectToEntry = fileName.split('/').
                                        subList(0, 3).joinToString (separator = "/")
                                childUpdates["users/$clientUid/audio/$projectToEntry"] = result
                                fDatabaseRef.updateChildren(childUpdates)
                                        .addOnCompleteListener{
                                            Log.i(MainActivity.INFO_TAG, "DB: Write at users/$clientUid/audio/$projectToEntry")
                                        }
                                        .addOnFailureListener {
                                            Log.i(MainActivity.INFO_TAG, "DB: Failed to write at users/$clientUid/audio/$projectToEntry")
                                        }
                            }
                        })
                        .addOnFailureListener(object : OnFailureListener {
                            override fun onFailure(exception: Exception) {
                            }
                        })
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                Thread.sleep((storage.packetSize * 1000 / 44100).toLong())
            } catch (e: InterruptedException) {
            }

        })
        thread?.start()
    }

    /**
     * 再生を終了する
     */
    fun stop() {
        val lthread = thread
        val ltrack = track
        if (lthread != null && ltrack != null) {
            try {
                lthread.join()
            } catch (e: InterruptedException) {
            }

            ltrack.release()
            track = null
        }
    }

}