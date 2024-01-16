package com.example.collabalbum.firebase_google_sign_in

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SignInComposable(state: SignInState, onSignInClick: () -> Unit)
{
    val context = LocalContext.current
    LaunchedEffect(key1 = state.signInError) //launched effect block recomposes whenever the key changes and launches block when recomposed
    {
        if (state.signInError != null)
        {
            Toast.makeText(context, state.signInError, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center)
    {
        Button(onClick = onSignInClick)
        {
            Text(text = "Sign in with Google")
            Icon(imageVector = Icons.Default.Login, contentDescription = null)

        }
    }
}