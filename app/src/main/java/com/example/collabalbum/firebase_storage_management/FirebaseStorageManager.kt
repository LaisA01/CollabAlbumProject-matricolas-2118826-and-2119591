package com.example.collabalbum.firebase_storage_management

import android.content.Context
import android.location.Location
import android.net.Uri
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import java.util.UUID
import kotlinx.coroutines.tasks.await

class FirebaseStorageManager {
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private val db = Firebase.firestore


    fun uploadImage(imageUri: Uri, context: Context, userID: String?): String //needed to update gps location DB
    {
        //give each photo a random UUID to avoid confusion
        val imageUUID = UUID.randomUUID()
        var photoRef: StorageReference = storageRef.child("user_images/$userID/$imageUUID.jpg")

        //convert uri to byte array for upload:

        val byteArray: ByteArray? =
            context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
        if (byteArray != null) {
            var upload = photoRef.putBytes(byteArray) //upload the byteArray to the ref

            //notify user via toast of the upload success/failure via firebase listeners
            upload.addOnFailureListener {
                Toast.makeText(context, "upload failed", Toast.LENGTH_SHORT).show()
                // Handle unsuccessful uploads
            }.addOnSuccessListener { taskSnapshot ->
                // contains file metadata such as size, content-type, etc.
                Toast.makeText(context, "upload succeeded", Toast.LENGTH_SHORT).show()
            }
        }
        return imageUUID.toString()
    }

    fun updateLocationDBForImage(imageUUID: String, location: Location)
    {
        val db_entry = hashMapOf("imageUUID" to imageUUID, "lat" to location.latitude, "lon" to location.longitude)
        db.collection("ImageGeolocationDataBase").document(imageUUID).set(db_entry)
    }

    suspend fun getLocationFromImageUUID(imageUUID: String): Location //return associated coords if a db entry exists, otherwise 0.0 0.0
    {
        val return_loc: Location = Location("").apply {
            latitude = 0.0
            longitude = 0.0
        }

        val documentSnapshot = db.collection("ImageGeolocationDataBase").document(imageUUID).get().await()
        return_loc.latitude =  documentSnapshot.data?.get("lat").toString().toDouble()
        return_loc.longitude =  documentSnapshot.data?.get("lon").toString().toDouble()

        return return_loc
    }


    suspend fun retrieveImageURLS(context: Context, userID: String?, currentUserEmail: String? = null, mergedUserID: String? = null, mergedUserEmail: String? = null) : MutableList<String>
    {
        val userFolderRef: StorageReference = storageRef.child("user_images/$userID")
        val images = userFolderRef.listAll().await()
        var imageURLs = mutableListOf<String>()
        for(image in images.items)
        {
            val URL = image.downloadUrl.await()
            imageURLs.add(URL.toString())
        }

        if(mergedUserID != null) //repeat the process with other account's ID if it is not null
        {
            //first check if other user has us set as merged with them in the user DB
            val user_db = Firebase.firestore
            var theyMergedWith: String? = null
            var weMergedWith: String? = null


            //get the email they are merged with
            var documentSnapshot = user_db.collection("UserDatabase").document(mergedUserEmail!!).get().await()
            if(documentSnapshot.exists())
            {
                theyMergedWith = (documentSnapshot.data?.get("MergedWith")).toString()
            }

            //get the email we are merged with
            documentSnapshot = user_db.collection("UserDatabase").document(currentUserEmail!!).get().await()
            if(documentSnapshot.exists())
            {
                weMergedWith = (documentSnapshot.data?.get("MergedWith")).toString()
            }

            if((currentUserEmail == theyMergedWith) && (mergedUserEmail == weMergedWith)) //if the two match, repeat the url retrieval with their photos
            {

                val mergedFolderRef: StorageReference = storageRef.child("user_images/$mergedUserID")
                val mergedImages = mergedFolderRef.listAll().await()
                for(image in mergedImages.items)
                {
                    val mergedURL = image.downloadUrl.await()
                    imageURLs.add(mergedURL.toString())
                }
            }
        }
        //Toast.makeText(context, imageURLs[0].toString(), Toast.LENGTH_LONG).show()
        return imageURLs
    }

    suspend fun deleteImageFromURL(URL: String)
    {
        val imageRef:StorageReference = storage.getReferenceFromUrl(URL)
        imageRef.delete().await()
    }
}