/*La Sapienza Universita di Roma - Mobile Application and Cloud Computing 23 - 24
Android application project by students:
  - Laith Alkhaer matricola 2118826
  - Mathieu Nicolas 2119591
Submitted for the 18/01/2024 exam*/

package com.example.collabalbum

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.collabalbum.ui.theme.CollabAlbumTheme
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.DoorBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import coil.compose.rememberImagePainter
import com.example.collabalbum.camera_management.CameraView
import com.example.collabalbum.camera_management.checkCameraPermission
import com.example.collabalbum.firebase_storage_management.FirebaseStorageManager
import com.example.collabalbum.firebase_google_sign_in.GoogleAuthUiClient
import com.example.collabalbum.firebase_google_sign_in.SignInComposable
import com.example.collabalbum.firebase_google_sign_in.SignInViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class MenuItem(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val badgeCount: Int? = null)
class MainActivity : ComponentActivity()
{
    private val googleAuthUiClient by lazy {GoogleAuthUiClient(context = applicationContext, oneTapClient = Identity.getSignInClient(applicationContext))}
    private val storageManagerInstance = FirebaseStorageManager()

    private lateinit var outputDirectory: File //output directory where to store the photo
    private lateinit var cameraExecutor: ExecutorService //to not block the main thread

    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false) //to determine when the camera should be shown
    private val thisMainActivity: ComponentActivity = this

    private lateinit var currentUri: Uri
    private lateinit var currentSelectedPhotoURL: String
    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)
    private lateinit var navController: NavHostController

    private lateinit var currentLastLocation: Location

    private lateinit var lastImageUUID: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var task: Task<Location>? = null
    private val myLocationManager = LocationManager()



    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted ->
        if(isGranted)
        {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            shouldShowCamera.value = true
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "CoroutineCreationDuringComposition") //stuff added by IDE
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        //init last location with default coordinates, in case permission is not given
        currentLastLocation = Location("").apply {
            latitude = 0.0
            longitude = 0.0
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) //init client
        fetchLocation() //to have a last location handy

        super.onCreate(savedInstanceState)

        //UI starts here
        setContent {

            CollabAlbumTheme {
                //list of side drawer buttons, maybe refactor later along with all the drawer stuff
                val items = listOf(
                    MenuItem(
                        title = "home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home
                    ),
                    MenuItem(
                        title = "take photo",
                        selectedIcon = Icons.Filled.Camera,
                        unselectedIcon = Icons.Outlined.Camera
                    ),
                    MenuItem(
                        title = "show last photo",
                        selectedIcon = Icons.Filled.Restore,
                        unselectedIcon = Icons.Outlined.Restore
                    ),
                    MenuItem(
                        title = "show gallery",
                        selectedIcon = Icons.Filled.Photo,
                        unselectedIcon = Icons.Outlined.Photo
                    ),
                    MenuItem(
                        title = "sign out",
                        selectedIcon = Icons.Filled.DoorBack,
                        unselectedIcon = Icons.Outlined.DoorBack
                    )
                )

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)
                {
                    //navcontroller for sign in screen, at least for now
                    navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "Google_sign_in")
                    {
                        composable("Google_sign_in")
                        {
                            AsyncImage(model = "https://firebasestorage.googleapis.com/v0/b/collabalbum-a75ca.appspot.com/o/background%2F1705182426960.png?alt=media&token=b364a042-7345-478e-b474-5a992da0e068" , contentDescription = null )
                            Text(text = "CollabAlbum App", color = Color.Black, fontSize = 59.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Cursive)
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.public_state.collectAsState()///////////////////////////
                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = {result ->
                                    if(result.resultCode == RESULT_OK)
                                    {
                                        lifecycleScope.launch (Dispatchers.IO){ val signInResult = googleAuthUiClient.signInWithIntent(
                                            intent = result.data?: return@launch
                                        )
                                        viewModel.onSignInResult(signInResult) //finally call viewmodel and pass on the result to update
                                        }
                                    }
                                }
                            )

                            LaunchedEffect(key1 = state.isSignInSuccessful)
                            {
                                if(state.isSignInSuccessful)
                                {
                                    Toast.makeText(applicationContext, "sign in successful", Toast.LENGTH_LONG).show()
                                    navController.navigate("business_logic_home") //navigate to business logic when sign in is done
                                    viewModel.resetState()
                                }
                            }

                            SignInComposable(state = state, onSignInClick = {lifecycleScope.launch (Dispatchers.IO){
                                val signInIntentSender = googleAuthUiClient.signIn()
                                launcher.launch(IntentSenderRequest.Builder(signInIntentSender ?: return@launch).build()
                                )
                            }
                            }
                            )
                        } //end first nav host composable

                        composable("business_logic_home") //second composable
                        {

                            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                            val scope = rememberCoroutineScope()
                            var selectedItemIndex by rememberSaveable { mutableStateOf(0) }

                            //Toast.makeText(thisMainActivity, currentLastLocation?.longitude.toString(), Toast.LENGTH_LONG ).show()

                            DismissibleNavigationDrawer(
                                drawerContent = {
                                    DismissibleDrawerSheet {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        //items.forEachIndexed { index, item ->
                                        //home button
                                        NavigationDrawerItem(
                                            label = { Text(text = items[0].title) },
                                            selected = 0 == selectedItemIndex,
                                            onClick =
                                            {
                                                //navController.navigate(item.route)
                                                selectedItemIndex = 0
                                                scope.launch { drawerState.close() }
                                                //on click action to be defined
                                            },
                                            icon = { Icon(imageVector = if (0 == selectedItemIndex) {  items[0].selectedIcon } else  items[0].unselectedIcon, contentDescription =  items[0].title) },
                                            //badge = {  items[0].badgeCount?.let { Text(text =  items[0].badgeCount.toString()) } },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        //open camera button
                                        NavigationDrawerItem(
                                            label = { Text(text = items[1].title) },
                                            selected = 1 == selectedItemIndex,
                                            onClick =
                                            {
                                                selectedItemIndex = 1
                                                scope.launch { drawerState.close() }
                                                navController.navigate("open_camera")
                                            },
                                            icon = { Icon(imageVector = if (1 == selectedItemIndex) {  items[1].selectedIcon } else  items[1].unselectedIcon, contentDescription =  items[1].title) },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        //show last image button
                                        NavigationDrawerItem(
                                            label = { Text(text = items[2].title) },
                                            selected = 2 == selectedItemIndex,
                                            onClick =
                                            {
                                                //navController.navigate(item.route)
                                                selectedItemIndex = 2
                                                scope.launch { drawerState.close() }
                                                //scope.launch { storageManagerInstance.uploadImage(currentUri, applicationContext, googleAuthUiClient.getSignedInUser()?.userId) }
                                                //shouldShowPhoto.value = true
                                                navController.navigate("show_last_photo")
                                            },
                                            icon = { Icon(imageVector = if (2 == selectedItemIndex) {  items[2].selectedIcon } else  items[2].unselectedIcon, contentDescription =  items[2].title) },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        //retrieve cloud gallery button
                                        NavigationDrawerItem(
                                            label = { Text(text = items[3].title) },
                                            selected = 3 == selectedItemIndex,
                                            onClick =
                                            {
                                                selectedItemIndex = 3
                                                navController.navigate("show_cloud_gallery")
                                            },
                                            icon = { Icon(imageVector = if (3 == selectedItemIndex) {  items[3].selectedIcon } else  items[3].unselectedIcon, contentDescription =  items[3].title) },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        //sign out button
                                        NavigationDrawerItem(
                                            label = { Text(text = items[4].title) },
                                            selected = 4 == selectedItemIndex,
                                            onClick =
                                            {
                                                scope.launch (Dispatchers.IO){
                                                    runBlocking{googleAuthUiClient.signOut()}
                                                }
                                                Toast.makeText(applicationContext, "signed out", Toast.LENGTH_LONG).show()
                                                navController.navigate("Google_sign_in")
                                            },
                                            icon = { Icon(imageVector = if (4 == selectedItemIndex) {  items[4].selectedIcon } else  items[4].unselectedIcon, contentDescription =  items[4].title) },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        //}
                                    }
                                },
                                drawerState = drawerState
                            )

                            { //begin ModalNavigationDrawer
                                Scaffold(
                                    topBar = {
                                        TopAppBar(
                                            title =
                                            {
                                                Text(text = "CollabAlbum App")
                                            },
                                            navigationIcon =
                                            {
                                                IconButton(onClick = { scope.launch {drawerState.open() }; shouldShowCamera.value = false; shouldShowPhoto.value = false })
                                                {
                                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                                                }
                                            }
                                        )
                                    }
                                ) { //begin Scaffold
                                    Column(modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally)
                                    {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Welcome! To begin take a new photo or view your cloud photos. Open the side menu for all options.", modifier = Modifier.offset(y = 200.dp))
                                        Button(onClick = {navController.navigate("open_camera")}, modifier = Modifier.offset(y = 202.dp)){Text("Take photo")}
                                        Button(onClick = {navController.navigate("show_cloud_gallery")}, modifier = Modifier.offset(y = 202.dp)){Text("Open cloud gallery")}
                                        Button(onClick = {navController.navigate("merge_gallery_screen")}, modifier = Modifier.offset(y = 202.dp)){Text("Merge gallery with another person")}
                                    }
                                  } //end scaffold
                            } //end ModalNavigationDrawer
                        }//end second nav controller composable

                        composable("open_camera")
                        {
                            checkCameraPermission(shouldShowCamera = shouldShowCamera, launcher = requestPermissionLauncher, activity = thisMainActivity)
                            navController.navigate("open_camera_to_show_last_photo")
                        }

                        //very quick and very dirty fix for a bug where navigating directly from cam to show photo crashes the app, TODO replace with an actual, respectable fix
                        composable("open_camera_to_show_last_photo")
                        {
                            if(shouldShowCamera.value == false)
                            {
                                navController.navigate("show_last_photo")
                            }
                        }

                        composable("show_last_photo")
                        {
                            //Toast.makeText(thisMainActivity, currentLastLocation?.longitude.toString(), Toast.LENGTH_LONG ).show()

                            val scope = rememberCoroutineScope()
                            shouldShowPhoto.value = true
                            if(::currentUri.isInitialized)
                            {
                                Image(
                                    painter = rememberImagePainter(currentUri),
                                    contentDescription = "photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = 750.dp), horizontalArrangement = Arrangement.SpaceBetween)
                                {
                                    Button(onClick = { navController.navigate("open_camera") }, modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp))
                                    {
                                        Icon(imageVector = Icons.Default.Backspace, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Retake")
                                    }
                                    Button(onClick = { scope.launch(Dispatchers.IO) {
                                        lastImageUUID = storageManagerInstance.uploadImage(currentUri, applicationContext, googleAuthUiClient.getSignedInUser()?.userId)
                                        storageManagerInstance.updateLocationDBForImage(lastImageUUID, currentLastLocation!!) //jsp si ca va fonctionner, si la loc va forcement etre dispo à ce moment
                                    }
                                        navController.navigate("business_logic_home")}, modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp))
                                    {
                                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Upload")
                                    }
                                }
                            }
                            else
                            {
                                Column(modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally)
                                {
                                    Text("No recent photo. Open camera to take one:")
                                    Button(onClick = {navController.navigate("open_camera")}){Text("Take photo")}
                                }
                            }


                        } // end nav composable "show_last_photo"
                        composable("show_cloud_gallery")
                        {

                            val scope = rememberCoroutineScope()
                            var imageURLs by remember { mutableStateOf<List<String>>(emptyList()) }
                            var mergedWithEmail : String? = null
                            var mergedWithID: String? = null

                            runBlocking{
                                mergedWithEmail = googleAuthUiClient.getMergedWith()
                            }
                            runBlocking{
                                mergedWithID =  googleAuthUiClient.retrieveIDFromEmail(mergedWithEmail.toString())
                            }
                            scope.launch(Dispatchers.IO) { imageURLs = storageManagerInstance.retrieveImageURLS(applicationContext, googleAuthUiClient.getSignedInUser()!!.userId,
                            googleAuthUiClient.getSignedInUser()!!.email.toString(), mergedWithID, mergedWithEmail)}

                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)
                            {
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp), verticalArrangement = Arrangement.spacedBy(2.dp), horizontalArrangement =  Arrangement.spacedBy(2.dp))
                                {
                                    if(imageURLs.isNotEmpty())
                                    {
                                        for(imageURL in imageURLs )
                                        {
                                            item{ AsyncImage(model = imageURL, contentDescription = null, modifier = Modifier.clickable {
                                                currentSelectedPhotoURL = imageURL
                                                navController.navigate("show_selected_photo")}
                                                )
                                                }
                                        }
                                    }
                                }

                            }
                            //quick fix for a bug in which deleted photos are brought back to life when they previous screen is retrieved from the stack
                            BackHandler {
                                navController.navigate("business_logic_home")
                            }
                        }
                        composable("show_selected_photo")
                        {
                            val scope = rememberCoroutineScope()

                            AsyncImage(model = currentSelectedPhotoURL, contentDescription = null )
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 750.dp), horizontalArrangement = Arrangement.SpaceBetween)
                            {
                                //delete button
                                Button(onClick = {
                                                 scope.launch(Dispatchers.IO){
                                                     storageManagerInstance.deleteImageFromURL(currentSelectedPhotoURL)
                                                 }
                                                 scope.launch{navController.navigate("show_cloud_gallery")
                                                 }},
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp))
                                {
                                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Delete")
                                }
                                //open location ig gmaps
                                Button(onClick = {  scope.launch{
                                                    val retrievedUUID = retrieveUUIDFromImageURL(currentSelectedPhotoURL) //retrieve UUID the simplest way, by extracting from URL
                                                    val locationToShow = storageManagerInstance.getLocationFromImageUUID(retrievedUUID)
                                                    myLocationManager.showLocationOnMap(
                                                        context = thisMainActivity,
                                                        latitude = locationToShow.latitude,
                                                        longitude = locationToShow.longitude,
                                                        label ="")}
                                                 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp))
                                {
                                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Show location on Google Maps")
                                }
                            }
                        }

                        composable("merge_gallery_screen")
                        {
                            var inputEmail by remember { mutableStateOf("")}
                            Column(modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally)
                            {
                                Spacer(modifier = Modifier.height(200.dp))
                                Text("Input the Google email of the person you want to merge cloud galleries with. Galleries will be merged once they do this step on their side.")
                                OutlinedTextField(value = inputEmail, onValueChange = {newInput -> inputEmail = newInput}, label = {Text("email")} )
                                Button(onClick = {
                                    googleAuthUiClient.updateMergedWith(inputEmail)
                                    Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
                                    navController.navigate("business_logic_home")
                                                 },
                                    modifier = Modifier.offset(y = 20.dp)){Text("Merge gallery with another person")}

                            }
                        }
                    } //end nav host


                } //end Surface

            } //end CollabAlbumTheme


            if(shouldShowCamera.value == true) //if camera should be shown, show it
            {
                CameraView(outputDirectory = outputDirectory, executor = cameraExecutor, onImageCaptured = ::handleImageCapture, onError = {})
            }

        } //end setContent

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }  //end onCreate


    //would be better to send these to CameraFunctions.kt but it's too much trouble for now, will see later.
    fun handleImageCapture(uri: Uri) //for when we call the capture image method
    {
        shouldShowCamera.value = false

        currentUri = uri //capture uri

        fetchLocation()

    }

    fun retrieveUUIDFromImageURL(URL: String): String
    {
        var UUID = ""
        val regex = Regex(".{36}(?=\\.jpg)")
        val matchResult = regex.find(URL)

        if (matchResult != null)
        {
            UUID = matchResult.value
        }
        return UUID
    }

    fun getOutputDirectory(): File //simple output dir getter
    {
        val mediaDir = externalMediaDirs.firstOrNull()?.let{
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if(mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun fetchLocation() {
        task = fusedLocationClient.lastLocation

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest
                    .permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }

        task!!.addOnSuccessListener {
            if (it != null) {
                currentLastLocation?.longitude = it.longitude
                currentLastLocation?.latitude = it.latitude
            }
        }
    }


    override fun onDestroy()
    {
        super.onDestroy()
        cameraExecutor.shutdown()

    }
}