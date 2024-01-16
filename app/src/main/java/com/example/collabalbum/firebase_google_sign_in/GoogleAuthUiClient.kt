package com.example.collabalbum.firebase_google_sign_in

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException
import com.google.firebase.firestore.firestore

//class to sign in and out and get signed in user's information
class GoogleAuthUiClient(private val context: Context, private val oneTapClient: SignInClient)
{
    val user_db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun signIn(): IntentSender? //suspendable because sign in might take some time and freeze the UI otherwise
    {
        val result = try{
            oneTapClient.beginSignIn(buildSignInRequest()).await() //await to suspend the coroutine until this is done
        }
        catch(e : Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
            null //so theres no inent sender if an exception is caugth
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent /*intent returned by firebase*/) : SignInResult
    {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)

        val user = auth.signInWithCredential(googleCredentials).await().user

        //update our email-uid database for uid retrieval from email, needed in merge account feature
        updateUserDB(user!!.email.toString(), user.uid) //!! op safe here because .await() on signIn method above

        return SignInResult(data = user?.run { UserData(userId = uid, username = displayName, email = email) }, errorMessage = null)
    }

    suspend fun updateUserDB(email: String, userID: String)
    {
        user_db.collection("UserDatabase").document(email).get().addOnSuccessListener {
            documentSnapshot -> if (!documentSnapshot.exists()){
            val db_entry = hashMapOf("Email" to email, "UserID" to userID, "MergedWith" to "none")
            user_db.collection("UserDatabase").document(email).set(db_entry)
            }

        }
    }

    suspend fun getMergedWith() : String?
    {
        val documentSnapshot = user_db.collection("UserDatabase").document((auth.currentUser!!.email).toString()).get().await()
        if(documentSnapshot.exists())
        {
            return documentSnapshot.data?.get("MergedWith").toString()
        }
        else return null
    }
    fun updateMergedWith(emailToMergeWith: String)
    {
        val dataToUpdate = hashMapOf<String, Any>("MergedWith" to emailToMergeWith)
        user_db.collection("UserDatabase").document(auth.currentUser!!.email.toString()).update(dataToUpdate)
    }

    suspend fun retrieveIDFromEmail(email: String?): String?
    {
        var return_ID: String? = null
        if (email != null)
        {
            val documentSnapshot = user_db.collection("UserDatabase").document(email).get().await()

            if(documentSnapshot.exists())
            {
                return_ID = (documentSnapshot.data?.get("UserID")).toString()
            }
        }

        return return_ID
    }

    private fun buildSignInRequest(): BeginSignInRequest
    {
        return BeginSignInRequest.Builder().setGoogleIdTokenRequestOptions(
            GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("298460711180-gnplfrv19j9oo7dp7ba4bkkr83n08qel.apps.googleusercontent.com")
                .build()
        ).build()
    }

    fun signOut()
    {
        oneTapClient.signOut()
        auth.signOut()
    }

    fun getSignedInUser(): UserData? = auth.currentUser?.run{
        return UserData(userId = uid, username = displayName, email = email)
    }


}
