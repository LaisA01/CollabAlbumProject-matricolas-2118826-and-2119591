package com.example.collabalbum.firebase_google_sign_in

data class SignInResult(val data: UserData?, val errorMessage: String?)

data class UserData(val userId: String, val username: String?, val email: String?)