package net.nponsard.wallchanger

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL


class Wallpaper : WallpaperService() {


    lateinit var mcontext: Context;   //reference to the current context

    override fun onCreateEngine(): Engine {

        mcontext = this

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)



        return LiveWall(mcontext)

    }


    inner class LiveWall : WallpaperService.Engine {

        lateinit var mcontext: Context;   //reference to the current context
        var canvas: Canvas? = null // canvas


        private var width = 0
        private var height = 0
        private var mVisible = false
        private var token = ""
        private lateinit var handler: Handler
        val client = OkHttpClient()

        private var url = ""

        private var mDrawFrame = Runnable {
            drawFrame()
        }

        constructor(mcontext: Context) : super() {
            this.mcontext = mcontext
            this.handler = Handler(Looper.getMainLooper())
        }


        //Called when the surface is created
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)

            //update  the matrix variables
            width = desiredMinimumWidth
            height = desiredMinimumHeight


            //call the draw method
            // this is where you must call your draw code

            handler.post(mDrawFrame)

        }

        // remove thread
        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(mDrawFrame)
        }

        //called when varaible changed
        override fun onVisibilityChanged(visible: Boolean) {
            mVisible = visible
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(mcontext)
            token = sharedPref.getString("token", "").toString()
            if (visible) {
                drawFrame()
            } else {
                handler.removeCallbacks(mDrawFrame)

            }
        }

        //called when surface destroyed
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            mVisible = false
            handler.removeCallbacks(mDrawFrame)
        }

        //this function contain the the main draw call
        /// this function need to call every time the code is executed
        // the thread call this functioin with some delay "drawspeed"
        fun drawFrame() {
            //getting the surface holder

            //getting the surface holder
            val holder = surfaceHolder


            try {
                canvas = holder.lockCanvas() //get the canvas
                if (canvas != null) {

                    // some matrix variable
                    // though not needed
                    val paint = Paint()
                    paint.color = Color.rgb(0, 0, 0)
                    paint.alpha = 255 //set the alpha

                    paint.style = Paint.Style.FILL
                    canvas!!.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                    val gfgThread = Thread {
                        try {

                            Log.println(Log.INFO, "draw", "thread" + canvas.toString())

                            val request =
                                Request.Builder().url("https://api.wall.nponsard.net/wallpaper")
                                    .header("api-key", token)
                                    .build()
                            val response = client.newCall(request).execute()
                            val body = response.body?.string()
                            if (body != null) {
                                Log.println(Log.INFO, "BODY", body)
                                val jsonObject = JSONObject(body)
                                val newUrl = jsonObject.getString("url")
                                // draw image if different


                                this.url = newUrl
                                val url =
                                    URL(this.url)
                                val image =
                                    BitmapFactory.decodeStream(
                                        url.openConnection().getInputStream()
                                    )

                                val matrix = Matrix()

                                val scaleX = width.toFloat() / image.width.toFloat()
                                val scaleY = height.toFloat() / image.height.toFloat()

                                if (scaleX < scaleY) {
                                    matrix.preScale(scaleX, scaleX)
                                    matrix.postTranslate(
                                        0F, (height - image.height * scaleX) / 2F
                                    )
                                } else {
                                    matrix.preScale(scaleY, scaleY)
                                    matrix.postTranslate(
                                        (width - image.width * scaleY) / 2F, 0F
                                    )
                                }


                                canvas?.drawBitmap(image, matrix, null)

                            }


                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    gfgThread.start()
                    gfgThread.join()

                }
                Log.println(Log.INFO, "canvas", canvas.toString())
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }

            Log.println(Log.INFO, "drawframe", "drawing frame" + token)

            handler.removeCallbacks(mDrawFrame)

            Log.println(Log.INFO, "visible", mVisible.toString())
            if (mVisible) {
                handler.postDelayed(mDrawFrame, 60000)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
        }
    }


}
