
import `in`.callific.androidr.IOnDocumentStreamCallBacks
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object AndroidRHelper {

    var deniedCount = 0

    /** Number of bytes to read at a time from an open stream */
    private const val FILE_BUFFER_SIZE_BYTES = 1024

    fun checkIfHasMediaPermissions(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun checkIfHasCameraAndMediaPermissions(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun openDocumentExample(context: Context?, inputStream: InputStream, documentFile: DocumentFile, callBack: IOnDocumentStreamCallBacks) {

        val resolver = context?.contentResolver
        val values = ContentValues()
// save to a folder

        val stamg = "" + System.currentTimeMillis()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, stamg + "_" + documentFile.name)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents" + "/" + "Android_R" + "/")

        val uri = resolver?.insert(MediaStore.Files.getContentUri("external"), values)
// You can use this outputStream to write whatever file you want:
        val outputStream = resolver?.openOutputStream(uri!!)

        var read: Int = -1
        read = inputStream.read()

        while (read != -1) {
            outputStream?.write(read)
            read = inputStream.read()
        }

        outputStream?.flush()
        outputStream?.close()

        Handler(Looper.getMainLooper()).post {
            callBack.onSuccess(uri)
        }
    }

    suspend fun openDocumentExample(inputStream: InputStream): String {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            inputStream.use { stream ->
                val messageDigest = MessageDigest.getInstance("SHA-256")

                val buffer = ByteArray(FILE_BUFFER_SIZE_BYTES)
                var bytesRead = stream.read(buffer)
                while (bytesRead > 0) {
                    messageDigest.update(buffer, 0, bytesRead)
                    bytesRead = stream.read(buffer)
                }
                val hashResult = messageDigest.digest()
                hashResult.joinToString(separator = "")
            }
        }
    }

    fun fetchPath(activity: AppCompatActivity, data: Intent, callBack: IOnDocumentStreamCallBacks) {
        try {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                DocumentFile.fromSingleUri(activity, data.data!!)?.let {
                    val documentStream =
                        activity.contentResolver.openInputStream(data.data!!)
                    openDocumentExample(activity, documentStream!!, it, callBack)
                }
            }
        } catch (e: Exception) {
            callBack.onFail()
        }
    }

    fun openDirectory(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun checkIfUserDeniedMoreThanOnce(): Boolean {
        deniedCount++
        if (deniedCount >= 2) {
            return true
        }
        return false
    }

    fun hasUserDeniedMoreThanOnce(): Boolean {
        if (deniedCount >= 2) {
            return true
        }
        return false
    }


    var isRedirectedToSettings = false

    fun showRequireDialog(activity: Activity, title: String, message: String, positiveText: String) {
        if (!isRedirectedToSettings) {
            val builder = AlertDialog.Builder(activity)
            builder.setCancelable(false)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(positiveText) { dialog, which ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                activity.startActivity(intent)
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }

    fun isAndroidROrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }


}