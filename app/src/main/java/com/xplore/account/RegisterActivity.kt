package com.xplore.account

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.soundcloud.android.crop.Crop
import com.squareup.picasso.Picasso
import com.xplore.General
import com.xplore.ImageUtil
import com.xplore.R
import com.xplore.TimeManager.Companion.globalTimeStamp
import com.xplore.TimeManager.Companion.refreshGlobalTimeStamp
import com.xplore.base.BaseActivity
import com.xplore.user.User
import kotlinx.android.synthetic.main.register_layout.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
* Created by Nikaoto on 3/11/2017.
* TODO write description of this class - what it does and why.
*/

open class RegisterActivity : BaseActivity(), DatePickerDialog.OnDateSetListener {

    /* Request Codes */
    val NONE = 0
    val CAMERA_PERMISSION_REQUEST_CODE = 1;
    val GALLERY_PERMISSION_REQUEST_CODE = 2;
    val ACTION_SNAP_IMAGE = 3

    //
    private val DEFAULT_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/xplore-a4aa3.appspot.com/o/user_default_profile_image.jpg?alt=media&token=9ef3891f-4525-414d-8039-061cdc65654e"
    val PIC_KILOBYTE_LIMIT = 25

    // Firebase
    val storageRef = FirebaseStorage.getInstance().reference
    fun firebaseStorageProfilePicUri(userId: String) = "users/$userId/profile_picture.jpg"

    // File provider authority string
    val authority = "com.xplore.fileprovider"

    // Users profile image url
    var imagePath: Uri? = null
    //TODO AI check for face in photo?

    //TODO add age restriction constant to resources
    private val ageRestriction: Int = 15
    var bYear: Int = 0
    var bMonth: Int = 0
    var bDay: Int = 0

    private val DBref = FirebaseDatabase.getInstance().reference

    // User data
    open val userId: String by lazy {
        intent.getStringExtra("userId")
    }
    open val userProfilePicUrl: String by lazy {
        intent.getStringExtra("photoUrl")
    }
    private val userFullName: String by lazy {
        intent.getStringExtra("fullName")
    }
    private val userEmail: String by lazy {
        intent.getStringExtra("email")
    }
    //

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context, userId: String, fullName: String?, email: String?,
                           photoUrl: Uri?): Intent
                = Intent(context, RegisterActivity::class.java)
                    .putExtra("userId", userId)
                    .putExtra("fullName", fullName)
                    .putExtra("email", email)
                    .putExtra("photoUrl", photoUrl.toString())
    }

    init {
        refreshGlobalTimeStamp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLayout()
        fillFields()
        initClickEvents()
    }

    open fun initLayout() {
        setContentView(R.layout.register_layout)
    }

    open fun fillFields() {
        // Loading data into views
        fnameEditText.safeSetText(separateFullName(userFullName, 0))
        lnameEditText.safeSetText(separateFullName(userFullName, 1))
        emailEditText.safeSetText(userEmail)
    }

    open fun initClickEvents() {
        initProfileImage(userProfilePicUrl)

        birthDateTextView.setOnClickListener {
            onBirthDateSelected(globalTimeStamp)
        }
        doneButton.setOnClickListener {
            onDoneButtonClick()
        }
    }

    open fun onBirthDateSelected(timeStamp: Long, offSet: Int = ageRestriction) {
        val fragment = com.xplore.DatePickerFragment(this, timeStamp, offSet)
        fragment.show(fragmentManager, "datePicker")
    }

    open fun initProfileImage(userProfilePicUrl: String?) {
        Picasso.with(this@RegisterActivity).invalidate(userProfilePicUrl)

        if (userProfilePicUrl == null || userProfilePicUrl == "null"
                || userProfilePicUrl.isEmpty()) {
            Picasso.with(this@RegisterActivity)
                    .load(R.drawable.user_default_profile_image)
                    .transform(ImageUtil.mediumCircle(this))
                    .into(profileImageView)
        } else {
            Picasso.with(this@RegisterActivity)
                    .load(userProfilePicUrl)
                    .transform(ImageUtil.mediumCircle(this))
                    .into(profileImageView)
        }

        profileImageView.setOnClickListener {
            val dialog = AlertDialog.Builder(this).setTitle(R.string.take_picture_from)
                    .setNegativeButton(R.string.camera, takeFromCamera)
                    .setPositiveButton(R.string.gallery, takeFromGallery)
                    .setNeutralButton(R.string.cancel, null)
                    .create()
            dialog.show()
        }
    }

    open fun onDoneButtonClick() {
        if (fieldsValid()) {
            val ref = storageRef.child(firebaseStorageProfilePicUri(userId))
            val newUser = UploadUser(
                    userId,
                    fnameEditText.str(),
                    lnameEditText.str(),
                    numEditText.str(),
                    emailEditText.str(),
                    General.getDateInt(bYear, bMonth, bDay),
                    userProfilePicUrl)
            if (imagePath != null) {
                uploadUserData(newUser, imagePath as Uri, ref)
            } else {
                addUserEntryToDataBase(newUser)
            }
        }
    }

    // Uploads user profile pic and saves new link. Finally, uploads the user data to db
    private fun uploadUserData(user: UploadUser, input: Uri, output: StorageReference) {
        output.putFile(input)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                        user.profile_picture_url = taskSnapshot.downloadUrl.toString()
                        addUserEntryToDataBase(user)
                }
                .addOnFailureListener {
                    Toast.makeText(this, R.string.fail_profile_picture_upload, Toast.LENGTH_SHORT)
                            .show()
                }
    }

    // Adds zero to Day or Month number if needed
    fun addZero(num: Int) = if(num < 10) "0$num" else "$num"

    override fun onDateSet(datePicker: DatePicker, year: Int, receivedMonth: Int, day: Int) {
        val month  = receivedMonth + 1 //+1 is necessary because 0 is January
        if (General.isNetConnected(this@RegisterActivity)) {

            //Checking if age is OK
            if (General.calculateAge(globalTimeStamp, year, month, day) >= ageRestriction) {
                bYear = year
                bMonth = month
                bDay = day

                birthDateTextView.text = "$bYear/${addZero(bMonth)}/${addZero(bDay)}"
            } else {
                val res = this@RegisterActivity.resources
                Toast.makeText(this@RegisterActivity, res.getString(R.string.you_must_be_at_least) +
                        " " + ageRestriction + " " + res.getString(R.string.years_to_use_xplore),
                        Toast.LENGTH_SHORT).show()
            }
        } else
            General.createNetErrorDialog(this@RegisterActivity)
    }

    private fun addUserEntryToDataBase(user: UploadUser) {
        if (user.profile_picture_url == null || user.profile_picture_url.isEmpty()) {
            user.profile_picture_url = DEFAULT_IMAGE_URL
        }
        val userData = user.toMap()

        val childUpdates = HashMap<String, Any>()
        childUpdates.put("/users/" + user.uid, userData)
        DBref.updateChildren(childUpdates)
        setResult(Activity.RESULT_OK)
        finish()
    }

    //TODO change init after converting User class to kotlin
    private inner class UploadUser(val uid: String, fname: String, lname: String, tel_num: String,
                                   email: String, birth_date: Int,
                                   profile_picture_url: String? = "") : User() {
        init {
            this.id = uid
            this.fname = fname
            this.lname = lname
            this.tel_num = tel_num;
            this.email = email
            this.birth_date = birth_date

            //Setting profile picture URL
            if (profile_picture_url != null) {
                this.profile_picture_url = profile_picture_url
            }

            //Every new user starts with 0 reputation
            this.reputation = 0
        }

        @Exclude
        fun toMap(): Map<String, Any> {
            val result = HashMap<String, Any>()
            result.put("fname", this.fname)
            result.put("lname", this.lname)
            result.put("profile_picture_url", this.profile_picture_url)
            result.put("birth_date", this.birth_date)
            result.put("tel_num", this.tel_num)
            result.put("reputation", this.reputation)
            result.put("email", this.email)
            return result
        }
    }

    fun makeBorderGreen(v: View) =  v.setBackgroundResource(R.drawable.edit_text_border)
    fun makeBorderRed(v: View) =  v.setBackgroundResource(R.drawable.edit_text_border_red)

    private fun fieldsValid(): Boolean {
        makeBorderGreen(fnameEditText)
        makeBorderGreen(lnameEditText)
        makeBorderGreen(emailEditText)
        makeBorderGreen(numEditText)
        makeBorderGreen(birthDateTextView)

        if (fnameEditText.text.isEmpty()) {
            return fieldError(fnameEditText)
        } else if (lnameEditText.text.isEmpty()) {
            return fieldError(lnameEditText)
        } else if (emailEditText.text.isEmpty()) {
            return fieldError(emailEditText)
        } else if (!General.isValidEmail(emailEditText.text)) {
            makeBorderRed(emailEditText)
            Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show()
            return false
        } else if (numEditText.text.isEmpty()) {
            return fieldError(numEditText)
        } else if (birthDateTextView.text.isEmpty()) {
            return fieldError(birthDateTextView)
        } else if (bYear == 0 || bMonth == 0 || bDay == 0) {
            return fieldError(birthDateTextView)
        }
        return true
    }

    private fun scrollToView(v: View) {
        scrollView.post{
            scrollView.smoothScrollTo(0, v.bottom)
        }
    }

    private fun fieldError(v: View): Boolean {
        makeBorderRed(v)
        scrollToView(v)

        Toast.makeText(this, R.string.error_field_required, Toast.LENGTH_SHORT).show()
        return false
    }

    //Splits a full name and returns the i-th part of it. Returns fullName back if it has no space
    private fun separateFullName(fullName: String?, i: Int): String {
        if (fullName == null) {
            return ""
        } else {
            var name = arrayOf<String>(fullName,"")
            if (fullName.contains(" "))
                name = fullName.split(" ".toRegex(), 2).toTypedArray()

            return name[i]
        }
    }

    fun EditText.str() = this.text.trim().toString()

    /* Everything below is code needed for profile picture choosing or taking functionality */

    // Requests permissions for given module with the passed request code and permissionType
    fun requestModulePermission(activity: Activity, permission: String, requestCode: Int,
                                module: () -> Unit) {
        //Checking if not allowed
        if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {
            //Open dialogue and request the permission
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        } else {
            module()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>?,
                                            grantResults: IntArray?) {
        if (grantResults != null && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
            when (requestCode) {
                CAMERA_PERMISSION_REQUEST_CODE -> prepareCamera()
                GALLERY_PERMISSION_REQUEST_CODE -> Crop.pickImage(this)
            }
        }
    }

    val takeFromCamera = DialogInterface.OnClickListener {
        _, _ ->
            requestModulePermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    CAMERA_PERMISSION_REQUEST_CODE, { prepareCamera() })

    }

    // Prepares extra permissions for camera and then opens it (kind of hacky)
    fun prepareCamera() {
        requestModulePermission(this, Manifest.permission.CAMERA,
                CAMERA_PERMISSION_REQUEST_CODE, { openCamera() })
    }

    fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoURI = createImageFile().getUri()
            imagePath = photoURI
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, ACTION_SNAP_IMAGE);
        }
    }

    val takeFromGallery = DialogInterface.OnClickListener {
        _, _ ->
            requestModulePermission(this, Manifest.permission.READ_EXTERNAL_STORAGE,
                    GALLERY_PERMISSION_REQUEST_CODE, { Crop.pickImage(this) })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            super.onActivityResult(requestCode, resultCode, data)
        }

        /* Picking or Taking picture */
        when (requestCode) {
            ACTION_SNAP_IMAGE -> {
                if(resultCode == Activity.RESULT_OK && imagePath != null) {
                    val temp = createImageFile()
                    cropImage(imagePath as Uri, temp.getUri())
                    imagePath = Uri.parse(getPicturePath(temp.name))
                    addPictureToGallery(imagePath.toString())
                }
            }
            Crop.REQUEST_PICK -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val tempFile = createImageFile()
                    imagePath = Uri.parse(getPicturePath(tempFile.name))
                    cropImage(data.data, tempFile.getUri())
                }
            }
            Crop.REQUEST_CROP -> {
                if (data != null) {
                    val tempPath = resizeAndCompressImage(imagePath.toString())
                    val tempFile = File(tempPath)
                    imagePath = tempFile.getUri()
                    Picasso.with(this)
                            .load(tempFile)
                            .transform(ImageUtil.mediumCircle(this))
                            .into(profileImageView)
                }
            }
        }
    }

    fun getPicturePath(picName: String)
            = getExternalFilesDir(Environment.DIRECTORY_PICTURES).absolutePath + "/" + picName

    fun File.getUri() = FileProvider.getUriForFile(this@RegisterActivity, authority, this)

    //Starts cropping activity with code Crop.REQUEST_CROP
    fun cropImage(input: Uri, output: Uri) = Crop.of(input, output).asSquare().start(this)

    // Returns temporary file for storing cropped picture
    @Throws(IOException::class)
    fun createImageFile(): File {
        val imageFileName = "profile_pic_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return imageFile
    }

    /* add picture to gallery */
    private fun addPictureToGallery(picturePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(File(picturePath))
        this.sendBroadcast(mediaScanIntent)
    }

    fun resizeAndCompressImage(filePath: String): String {
        val MAX_IMAGE_SIZE = PIC_KILOBYTE_LIMIT * 1024 // 10 kilobytes

        //Decode with inJustDecodeBounds so we can resize it first
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        //
        options.inSampleSize = calculateInSampleSize(options, 300, 300)
        //
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bmp = BitmapFactory.decodeFile(filePath, options)

        //Compressing
        var compressQuality = 100 //Decreases by 5 every loop
        var streamLength = 0

        do {
            val bmpStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
            val bmpByteAarray = bmpStream.toByteArray()
            streamLength = bmpByteAarray.size
            compressQuality -= 5
        } while (streamLength >= MAX_IMAGE_SIZE)

        val tempFile = createImageFile()
        try {
            val tempBmpFile = FileOutputStream(tempFile)
            bmp.compress(Bitmap.CompressFormat.JPEG, compressQuality, tempBmpFile)
            tempBmpFile.flush()
            tempBmpFile.close()
        } catch (e: Exception) {
            throw e
        }
        return tempFile.absolutePath
    }

    //Calculates largest sample size that is the power of 2
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val width = options.outWidth
        val height = options.outHeight
        var sampleSize = 1

        if (width > reqWidth || height > reqHeight) {
            val halfWidth = width /2
            val halfHeight = height / 2

            while ((halfWidth / sampleSize) > reqWidth && (halfHeight / sampleSize) > reqHeight) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    override fun onBackPressed() {
        // Leave empty, so user doesn't accidentally exit during registration
    }

    fun TextView.safeSetText(s: String?) {
        if (s != null) {
            this.text = s
        } else {
            this.text = ""
        }
    }
}
