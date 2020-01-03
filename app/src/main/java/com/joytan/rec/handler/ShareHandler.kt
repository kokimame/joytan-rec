package com.joytan.rec.handler

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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.joytan.rec.data.QRecStorage
import com.joytan.rec.main.MainActivity
import com.joytan.rec.main.MainFragment
import java.io.File
import java.io.IOException

class ShareHandler(private val storage: QRecStorage) {
    private var track: AudioTrack? = null

    private var thread: Thread? = null

    val fStorage = FirebaseStorage.getInstance()
    val fStorageRef = fStorage.reference
    // Create a Firestore database reference from our app
    val db = FirebaseFirestore.getInstance()
    val increment = FieldValue.increment(1)

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
                                val fileItems = fileName.split('/')
                                val dirname = fileItems[1]
                                val index = fileItems[2]
                                val clientUid = fileItems[3]
                                val vRecord = hashMapOf(
                                        "uid" to clientUid,
                                        "admin_use" to "",
                                        "created_at" to FieldValue.serverTimestamp()
                                )
                                db.collection("users/$clientUid/audio/$dirname/entries").document(index)
                                        .set(vRecord)
                                        .addOnSuccessListener { Log.e(MainActivity.INFO_TAG, "db success") }
                                        .addOnFailureListener { exception ->
                                            Log.e(MainActivity.INFO_TAG, "db failure $exception") }
                                        .addOnCompleteListener { MainActivity.pd.dismiss() }
                                db.collection("users").document(clientUid)
                                        .update("voice_add", increment)
                                        .addOnFailureListener { exception ->
                                            // For now, don't use exception.javaClass since Exception class may be too
                                            // broad to know whether the cause is NOT_FOUND or other causes.
                                            if ("NOT_FOUND" in exception.toString()) {
                                                // Initialize audio contribution counter
                                                db.collection("users").document(clientUid)
                                                        .set(hashMapOf("voice_add" to 1))
                                            }
                                        }
                            }
                        })
                        .addOnFailureListener(object : OnFailureListener {
                            override fun onFailure(exception: Exception) {
                                MainActivity.pd.dismiss()
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