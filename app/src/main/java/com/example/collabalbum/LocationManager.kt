package com.example.collabalbum

import android.content.Context
import android.content.Intent
import android.net.Uri


class LocationManager
{
    fun showLocationOnMap(context: Context, latitude: Double, longitude: Double, label: String = "Location") {
        val gmmIntentUri: Uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            openMapInBrowser(context, latitude, longitude)
        }
    }

    private fun openMapInBrowser(context: Context, latitude: Double, longitude: Double) {
        val mapUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
        context.startActivity(mapIntent)
    }
}