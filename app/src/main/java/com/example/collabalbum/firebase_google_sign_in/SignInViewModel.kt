package com.example.collabalbum.firebase_google_sign_in
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SignInState(val isSignInSuccessful : Boolean = false, val signInError : String? = null)

class SignInViewModel : ViewModel()
{
    private val state = MutableStateFlow(SignInState())
    val public_state = state.asStateFlow()  //to avoid exposing the mutable version of our state

    fun onSignInResult(result: SignInResult)
    {
        state.update {it.copy(isSignInSuccessful = result.data != null, signInError = result.errorMessage)}
    }

    fun resetState()
    {
        state.update { SignInState(false, null) }
    }
}