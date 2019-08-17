//package com.joytan.rec.main
//
//import android.app.ProgressDialog
//import android.os.AsyncTask
//
//class AsyncWheel : AsyncTask<Void, Void, Void>() {
//    private val pd = ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT)
//
//    // can use UI thread here
//    override fun onPreExecute() {
//        pd.setMessage("Loading data ...")
//        pd.setCancelable(false)
//        pd.show()
//    }
//
//    override fun doInBackground(vararg p0: Void?): Void {
//        return null!!
//    }
//
//    override fun onPostExecute(result: Void) {
//
//        if (this.pd.isShowing) {
//            this.pd.dismiss()
//        }
//    }
//}
