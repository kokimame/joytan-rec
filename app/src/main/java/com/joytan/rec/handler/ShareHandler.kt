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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.joytan.rec.data.QRecStorage
import com.joytan.rec.main.MainActivity
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

    fun share(voiceProps : HashMap<String, *>, onEndListener: () -> Unit) {

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
                val dirname = voiceProps["project"] as String
//                val index = voiceProps["index"] as Int
                val clientUid = voiceProps["client_id"] as String
                val entryId = voiceProps["entry_id"] as String
                val userRef = db.collection("users").document(clientUid)
                val voiceRef = db.collection("projects/$dirname/voice").document()
                val projectRef = db.collection("projects").document(dirname)

                val filePath = "voice/$dirname/${voiceRef.id}.wav"
                val audioRef = fStorageRef.child(filePath)
                audioRef.putFile(outputFile)
                        .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                            override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot) {
                                MainActivity.pd.dismiss()
                                db.runTransaction { trans ->
                                    val snap = trans.get(userRef)
                                    if (snap.exists()) {
                                        trans.update(userRef, "voice_add", increment)
                                    } else {
                                        trans.set(userRef, hashMapOf("voice_add" to 1))
                                    }
                                    trans.update(projectRef, "available", FieldValue.arrayUnion(entryId))
                                    trans.update(userRef, dirname, FieldValue.arrayUnion(entryId))
                                    trans.update(userRef, "voice", FieldValue.arrayUnion(voiceRef.id))
                                    trans.set(voiceRef, voiceProps)
                                }.addOnCompleteListener {
                                    Log.e(MainActivity.DEBUG_TAG, it.result!!.toString())
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
                // FIXME: Not clear. Should be better to handle exceptions carefully
                MainActivity.pd.dismiss()
            }
            try {
                Thread.sleep((storage.packetSize * 1000 / 44100).toLong())
            } catch (e: InterruptedException) {
            }

        })
        thread?.start()
    }

    /**
     * Stop playing
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