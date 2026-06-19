package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewmodel.ChefAiViewModel
import com.example.ui.viewmodel.SearchStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefAiMainContainer(viewModel: ChefAiViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val isDark = activeProfile?.isDarkMode ?: false

    MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    if (currentScreen != "Onboarding") {
                        ChefAiBottomBar(
                            currentScreen = currentScreen,
                            onTabSelected = { screen ->
                                if (screen == "Home") {
                                    viewModel.updateSearchQuery("")
                                }
                                viewModel.navigateTo(screen)
                            },
                            avatarUrl = activeProfile?.profilePicture ?: "",
                            credits = activeProfile?.credits ?: 0,
                            plan = activeProfile?.plan ?: "Free"
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScreenTransition"
                    ) { target ->
                        when (target) {
                            "Onboarding" -> OnboardingScreen(viewModel)
                            "Home" -> HomeScreen(viewModel)
                            "Search" -> SearchScreen(viewModel)
                            "Favorites" -> FavoritesScreen(viewModel)
                            "AiGenerator" -> AiGeneratorScreen(viewModel)
                            "Profile" -> ProfileScreen(viewModel)
                            "RecipeDetails" -> RecipeDetailsScreen(viewModel)
                            "AdminDashboard" -> AdminDashboardScreen(viewModel)
                            "ShoppingList" -> ShoppingListScreen(viewModel)
                            else -> HomeScreen(viewModel)
                        }
                    }

                    // Simple Custom Alert Banner / Snackbar implementation
                    snackbarMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.fontFamilyTypography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearSnackbar() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "CloseAlert",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Auto clear after 4 seconds
                        LaunchedEffect(msg) {
                            delay(4000)
                            viewModel.clearSnackbar()
                        }
                    }
                }
            }
        }
    }
}

// Helper delay function
suspend fun delay(ms: Long) {
    kotlinx.coroutines.delay(ms)
}

// --- Dynamic Typography Theme helper wrapper ---
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    com.example.ui.theme.MyApplicationTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

val MaterialTheme.fontFamilyTypography get() = com.example.ui.theme.Typography

// --- Bottom Navigation Bar Component ---
@Composable
fun ChefAiBottomBar(
    currentScreen: String,
    onTabSelected: (String) -> Unit,
    avatarUrl: String,
    credits: Int,
    plan: String
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 8.dp
    ) {
        val menuItems = listOf(
            Triple("Home", Icons.Default.Home, "home_tab"),
            Triple("Search", Icons.Default.Search, "search_tab"),
            Triple("Favorites", Icons.Default.Favorite, "favorites_tab"),
            Triple("AiGenerator", Icons.Default.AutoAwesome, "ai_generator_tab"),
            Triple("Profile", Icons.Default.Person, "profile_tab")
        )

        menuItems.forEach { (screen, icon, tag) ->
            val isSelected = currentScreen == screen || (screen == "Profile" && currentScreen == "AdminDashboard")
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(screen) },
                label = {
                    Text(
                        text = if (screen == "AiGenerator") "ChefAI" else screen,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == "AiGenerator") {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("AI", fontSize = 9.sp, color = Color.White)
                                }
                            } else if (screen == "Profile" && credits > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                    Text(
                                        if (plan == "Max Plus") "∞" else if (credits > 999) "999+" else "$credits",
                                        fontSize = 8.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

// --- Onboarding & Splash Authentication Screen ---
@Composable
fun OnboardingScreen(viewModel: ChefAiViewModel) {
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var showForgotPasswordState by remember { mutableStateOf(false) }
    var showVerificationNotice by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogleToken(idToken)
                } else {
                    val email = account?.email ?: "google_chef@gmail.com"
                    val displayName = account?.displayName ?: "Chef Guest"
                    viewModel.loginWithGoogle(email, displayName)
                }
            } catch (e: Exception) {
                viewModel.showSnackbar("Google Sign-In offline fallback initialized.")
                viewModel.loginWithGoogle()
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ChefAI",
                style = MaterialTheme.fontFamilyTypography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Smart Recipe Generator",
                style = MaterialTheme.fontFamilyTypography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Forms Area
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    if (showForgotPasswordState) {
                        // Forgot Password mock screen
                        Text(
                            text = "Reset Password",
                            style = MaterialTheme.fontFamilyTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Enter your verified email to receive password reset link guidelines.",
                            style = MaterialTheme.fontFamilyTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, "EmailIcon") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (emailInput.trim().isEmpty()) {
                                    viewModel.showSnackbar("Please enter an email first")
                                } else {
                                    viewModel.showSnackbar("Password reset verification link dispatched to ${emailInput.trim()}")
                                    showForgotPasswordState = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Send Link", fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showForgotPasswordState = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Back to log in")
                        }
                    } else if (showVerificationNotice) {
                        // Mock email verification screen
                        Text(
                            text = "Verify your email",
                            style = MaterialTheme.fontFamilyTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally).padding(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "We have dispatched a verification link to $emailInput. Please click it to complete account settings.",
                            style = MaterialTheme.fontFamilyTypography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Button(
                            onClick = {
                                showVerificationNotice = false
                                isSignUpMode = false
                                viewModel.loginOrSignUpWithEmail(emailInput, nameInput, isSignUp = true, passwordInput)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Verified & Login", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Regular Login / Signup Form
                        Text(
                            text = if (isSignUpMode) "Create Account" else "Welcome Back",
                            style = MaterialTheme.fontFamilyTypography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Badge, null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .testTag("auth_email_input")
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        if (!isSignUpMode) {
                            TextButton(
                                onClick = { showForgotPasswordState = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Forgot Password?", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (emailInput.trim().isEmpty() || passwordInput.trim().isEmpty()) {
                                    viewModel.showSnackbar("Please enter all details.")
                                    return@Button
                                }
                                if (isSignUpMode) {
                                    showVerificationNotice = true
                                } else {
                                    viewModel.loginOrSignUpWithEmail(emailInput, nameInput, isSignUp = false, passwordInput)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_submit_button")
                        ) {
                            Text(
                                text = if (isSignUpMode) "Register" else "Login",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { isSignUpMode = !isSignUpMode }
                            ) {
                                Text(
                                    text = if (isSignUpMode) "Login" else "Sign Up",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "— OR CONTINUES WITH —",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Google Sign In button
            OutlinedButton(
                onClick = {
                    try {
                        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                        val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else null

                        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                        if (webClientId != null) {
                            gsoBuilder.requestIdToken(webClientId)
                        }
                        val gso = gsoBuilder.build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)

                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    } catch (e: Exception) {
                        viewModel.showSnackbar("Google launcher dynamic error: ${e.localizedMessage}")
                        viewModel.loginWithGoogle()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_login_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GTranslate, // Substitute for G Symbol
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- HOME SCREEN ---
@Composable
fun HomeScreen(viewModel: ChefAiViewModel) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val featuredRecipes by viewModel.featuredRecipes.collectAsStateWithLifecycle()
    val trendingRecipes by viewModel.trendingRecipes.collectAsStateWithLifecycle()
    val shoppingListCount by viewModel.shoppingList.collectAsStateWithLifecycle()
    
    // Quick Category filtering results displayed inline if isCategorySelected
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()
    val categoryRecipes = remember(selectedCategory, allRecipes) {
        if (selectedCategory != null) {
            allRecipes.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    val name = activeProfile?.name ?: "Chef"
    val credits = activeProfile?.credits ?: 0
    val plan = activeProfile?.plan ?: "Free"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$name 👋",
                        style = MaterialTheme.fontFamilyTypography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = plan.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (activeProfile != null && activeProfile!!.email.trim().lowercase() == "riponprime42@gmail.com") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clickable { viewModel.navigateTo("AdminDashboard") }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ADMIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shopping Cart pill/badge button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { viewModel.navigateTo("ShoppingList") }
                        .testTag("home_shopping_cart_button"),
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (shoppingListCount.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                ) {
                                    Text("${shoppingListCount.size}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Shopping List",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Credits Pill Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (plan == "Max Plus") "∞ Credits" else if (credits > 999) "999+ Credits" else "$credits Credits",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Avatar Dual-Tone Border
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.navigateTo("Profile") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiPeople,
                            contentDescription = "Avatar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // --- Simulated Search Input Card ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(32.dp)) // Sleek 32dp roundness as in design HTML
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                .clickable { viewModel.navigateTo("Search") }
                .padding(horizontal = 20.dp, vertical = 15.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search recipes or ingredients...",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    style = MaterialTheme.fontFamilyTypography.bodyMedium,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Magic Kitchen Hero Card ---
        Card(
            shape = RoundedCornerShape(32.dp), // Sleek design exact rounded corners
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5), // Indigo 600
                                Color(0xFF4338CA), // Indigo 700
                                Color(0xFF6B21A8)  // Purple 800
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MAGIC KITCHEN",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Empty Fridge?\nLet AI cook.",
                        style = MaterialTheme.fontFamilyTypography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Generate a gourmet recipe using only the ingredients you have left.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.navigateTo("AiGenerator") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Launch AI Generator",
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Recipe Categories Horizontal Scroll ---
        Text(
            text = "Categories",
            style = MaterialTheme.fontFamilyTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        val categoriesList = listOf(
            "Breakfast", "Lunch", "Dinner", "Snacks", "Desserts",
            "Vegetarian", "Vegan", "Seafood", "Healthy", "Quick Meals"
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoriesList) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedCategory = if (isSelected) null else category
                    },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Category Inline Results Section ---
        if (selectedCategory != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Filtered by: $selectedCategory",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.fontFamilyTypography.bodyLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (categoryRecipes.isEmpty()) {
                        Text(
                            text = "No local recipes in this category right now. Use the ChefAI tab to generate a dynamic custom one!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        categoryRecipes.forEach { recipe ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectRecipe(recipe) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = recipe.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recipe.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${recipe.cuisine} • ${recipe.cookTimeMin + recipe.prepTimeMin} mins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Featured Section: Trending Recipes ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trending Recipes",
                    style = MaterialTheme.fontFamilyTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(trendingRecipes) { recipe ->
                RecipeCard(recipe = recipe, onClick = { viewModel.selectRecipe(recipe) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Featured Section: Popular This Week ---
        Text(
            text = "Popular This Week",
            style = MaterialTheme.fontFamilyTypography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(featuredRecipes) { recipe ->
                RecipeCard(recipe = recipe, onClick = { viewModel.selectRecipe(recipe) })
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- RECIPE CARD COMPONENT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
            .testTag("recipe_item_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (recipe.isAI) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AI Custom",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recipe.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.fontFamilyTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${recipe.cuisine} • ${recipe.cookTimeMin + recipe.prepTimeMin} min",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${recipe.rating}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(recipe.difficulty, fontSize = 9.sp) },
                        modifier = Modifier.height(20.dp),
                        enabled = false
                    )
                    Text(
                        text = "${recipe.servings} Servings",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

// --- SEARCH FLOW SCREEN ---
@Composable
fun SearchScreen(viewModel: ChefAiViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Discover Recipes",
            style = MaterialTheme.fontFamilyTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Actual input field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search recipes, ingredients, or cuisines") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recipe_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (searchState) {
                is SearchStatus.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Find delicious culinary recipes. Enter a keyword to start, or go to ChefAI generator!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is SearchStatus.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SearchStatus.SuccessLocal, is SearchStatus.SuccessExternal -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Results Found: ${searchResults.size}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.fontFamilyTypography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = {},
                                label = { Text(if (searchState is SearchStatus.SuccessLocal) "Internal Archive" else "External API Partners") }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults) { recipe ->
                                SearchResultRow(recipe = recipe, onClick = { viewModel.selectRecipe(recipe) })
                            }
                        }
                    }
                }
                is SearchStatus.NoResultsFound -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentVeryDissatisfied,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "We couldn't find your recipe.",
                            style = MaterialTheme.fontFamilyTypography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate a custom recipe with AI for 100 credits.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.fontFamilyTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.setAiIngredients(searchQuery)
                                viewModel.navigateTo("AiGenerator")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_fallback_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Custom Recipe (100 credits)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("search_result_item"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${recipe.cuisine} cuisine • ${recipe.cookTimeMin + recipe.prepTimeMin} mins",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Text("${recipe.rating}", fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp), fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Default.ChevronRight, "View")
        }
    }
}

// --- FAVORITES SCREEN ---
@Composable
fun FavoritesScreen(viewModel: ChefAiViewModel) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, "FavHead", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Favorites",
                style = MaterialTheme.fontFamilyTypography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your favorite recipes shelf is empty. Toggle the heart button inside description pages!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(favorites) { recipe ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRecipe(recipe) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${recipe.cuisine} • ${recipe.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(recipe.id) }) {
                                Icon(Icons.Default.Favorite, "Unfav", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- AI GENERATOR FLOW SCREEN ---
@Composable
fun AiGeneratorScreen(viewModel: ChefAiViewModel) {
    val aiIngredients by viewModel.aiIngredients.collectAsStateWithLifecycle()
    val aiCuisine by viewModel.aiCuisine.collectAsStateWithLifecycle()
    val aiDietary by viewModel.aiDietary.collectAsStateWithLifecycle()
    val aiTime by viewModel.aiTime.collectAsStateWithLifecycle()
    val aiServings by viewModel.aiServings.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    var showConfirmationDialog by remember { mutableStateOf(false) }

    val plan = activeProfile?.plan ?: "Free"
    val credits = activeProfile?.credits ?: 0

    if (isGenerating) {
        // Special aesthetic progress visual with culinary messages
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "ChefAI is cooking...",
                    style = MaterialTheme.fontFamilyTypography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val customPhrases = listOf(
                    "Mixing premium gourmet ingredients...",
                    "Balancing spices and nutritious calories...",
                    "Generating culinary alternative ingredients...",
                    "Formatting cooking details chronologically..."
                )
                var currentPhraseIdx by remember { mutableIntStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(2500)
                        currentPhraseIdx = (currentPhraseIdx + 1) % customPhrases.size
                    }
                }

                Text(
                    text = customPhrases[currentPhraseIdx],
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.fontFamilyTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ChefAI Creator",
                        style = MaterialTheme.fontFamilyTypography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generating dynamic recipes from scratch",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ingredients Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Available Ingredients",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.fontFamilyTypography.titleMedium
                    )
                    Text(
                        text = "List what ingredients are inside your cabinet! (Comma separated)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = aiIngredients,
                        onValueChange = { viewModel.setAiIngredients(it) },
                        placeholder = { Text("e.g. chicken, garlic, onions, tomatoes, spinach") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("ai_ingredients_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preferences Card Options
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Recipe Attributes", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cuisine selection Row
                    Text("Cuisine Preference", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val cuisines = listOf("Italian", "Mexican", "Japanese", "Indian", "American", "French")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        items(cuisines) { cuisine ->
                            val selected = aiCuisine == cuisine
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setAiCuisine(cuisine) },
                                label = { Text(cuisine, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dietary Choices Row
                    Text("Dietary Restriction", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val dietaryTags = listOf("None", "Gluten-Free", "Vegetarian", "Vegan", "Keto", "Dairy-Free")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        items(dietaryTags) { tag ->
                            val selected = aiDietary == tag
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setAiDietary(tag) },
                                label = { Text(tag, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ready in timer row
                    Text("Max Ready Time Limit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val times = listOf("15 Mins", "30 Mins", "45 Mins", "1 Hour")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        items(times) { t ->
                            val selected = aiTime == t
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setAiTime(t) },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Servings Stepper quantity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Serving Size Target", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Ideal counts of plates", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.setAiServings((aiServings - 1).coerceAtLeast(1)) }) {
                                Icon(Icons.Default.RemoveCircleOutline, "Dec")
                            }
                            Text("$aiServings", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { viewModel.setAiServings((aiServings + 1).coerceAtMost(10)) }) {
                                Icon(Icons.Default.AddCircleOutline, "Inc")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action generate button trigger popup or prompt
            Button(
                onClick = {
                    if (aiIngredients.trim().isEmpty()) {
                        viewModel.showSnackbar("Please enter ingredients first!")
                        return@Button
                    }
                    if (plan != "Max Plus" && credits < 100) {
                        viewModel.showSnackbar("You don't have enough credits. Upgrade your plan to continue generating recipes.")
                        return@Button
                    }
                    // Trigger popup confirmation dialog
                    showConfirmationDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_ai_generation_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Custom Recipe (100 credits)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Confirmation Popup Dialog
    if (showConfirmationDialog) {
        Dialog(onDismissRequest = { showConfirmationDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Paid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Confirm Generation?",
                        style = MaterialTheme.fontFamilyTypography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ChefAI requires 100 credits to generate a custom recipe using artificial intelligence.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.fontFamilyTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                viewModel.generateAiRecipe()
                            }
                        ) {
                            Text("Deduct 100 & Cook")
                        }
                    }
                }
            }
        }
    }
}

// --- RECIPE DETAILS SCREEN ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailsScreen(viewModel: ChefAiViewModel) {
    val recipe by viewModel.selectedRecipe.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val userEmail = activeProfile?.email ?: ""
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorited = remember(recipe, favorites) {
        recipe != null && favorites.any { it.id == recipe!!.id }
    }
    // Shopping items checklist state
    val shoppingList by viewModel.shoppingList.collectAsStateWithLifecycle()

    if (recipe == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recipe active. Navigate back.")
        }
        return
    }

    val active = recipe!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- Image Banner Wrapper ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            AsyncImage(
                model = active.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Scrim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.40f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.70f)
                            )
                        )
                    )
            )

            // Actions Overlay (Back, Favorite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { viewModel.navigateBack() },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }

                Row {
                    // Shopping List Button
                    FilledIconButton(
                        onClick = { viewModel.navigateTo("ShoppingList") },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        BadgedBox(
                            badge = {
                                if (shoppingList.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.offset(x = (-2).dp, y = 2.dp)
                                    ) {
                                        Text("${shoppingList.size}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, "Shopping List", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Favorite Toggle
                    FilledIconButton(
                        onClick = { viewModel.toggleFavorite(active.id) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = if (isFavorited) Color.Red else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Simulated Share
                    FilledIconButton(
                        onClick = { viewModel.showSnackbar("Recipe link copied. Share tasty food with your family!") },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Share, "Share", tint = Color.White)
                    }
                }
            }

            // Recipe Title on Image
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                if (active.isAI) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ChefAI Generated", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = active.title,
                    style = MaterialTheme.fontFamilyTypography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            // General Info Badges Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoBadge(imageVector = Icons.Default.AccessTime, value = "${active.cookTimeMin + active.prepTimeMin} min", subLabel = "Total Time")
                InfoBadge(imageVector = Icons.Default.SignalCellularAlt, value = active.difficulty, subLabel = "Level")
                InfoBadge(imageVector = Icons.Default.Restaurant, value = "${active.servings}", subLabel = "Servings")
                InfoBadge(imageVector = Icons.Default.LocalFireDepartment, value = "${active.calories} kcal", subLabel = "Calories")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description Info
            Text(text = "About Recipe", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
            Text(
                text = active.description,
                style = MaterialTheme.fontFamilyTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ingredients Shelf Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ingredients List", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
                TextButton(
                    onClick = {
                        val itemsList = active.ingredients.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                        itemsList.forEach { ing ->
                            viewModel.addIngredientToShopping(active.id, active.title, ing)
                        }
                    }
                ) {
                    Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add all to Shopping List", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            val listIngredients = active.ingredients.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            listIngredients.forEach { ing ->
                val alreadyAdded = shoppingList.any { it.recipeId == active.id && it.ingredient.equals(ing, ignoreCase = true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(6.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = ing, style = MaterialTheme.fontFamilyTypography.bodyMedium)
                    }

                    IconButton(
                        onClick = {
                            if (alreadyAdded) {
                                viewModel.showSnackbar("Ingredient is already in your Shopping list!")
                            } else {
                                viewModel.addIngredientToShopping(active.id, active.title, ing)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (alreadyAdded) Icons.Default.CheckCircle else Icons.Default.AddShoppingCart,
                            contentDescription = "ShopAdd",
                            tint = if (alreadyAdded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step by steps cooking Directions
            Text(text = "Step-By-Step Cooking Directions", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            val dirSteps = active.instructions.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            dirSteps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = step,
                        style = MaterialTheme.fontFamilyTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }

            // Alternatives Option if any
            if (active.alternativeIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapCalls, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Healthy Alternatives", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val alts = active.alternativeIngredients.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            alts.forEach { alt ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(alt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                )
                            }
                        }
                    }
                }
            }

            // Cooking Tips if any
            if (active.tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Chef Tips", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = active.tips,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun InfoBadge(imageVector: androidx.compose.ui.graphics.vector.ImageVector, value: String, subLabel: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(70.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = subLabel,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- PROFILE SCREEN WITH BILLING INTEGRATION ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: ChefAiViewModel) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val shoppingList by viewModel.shoppingList.collectAsStateWithLifecycle()

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var selectedPlanForDialog by remember { mutableStateOf("Pro") }

    if (activeProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please login first or explore as Guest.")
        }
        return
    }

    val active = activeProfile!!
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // --- Inner Avatar details ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = active.name,
                style = MaterialTheme.fontFamilyTypography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = active.email,
                style = MaterialTheme.fontFamilyTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            AssistChip(
                onClick = {},
                label = { Text("Active: ${active.plan} Plan") },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                leadingIcon = { Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Active Shopping List Module ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Shopping List", fontWeight = FontWeight.Bold)
                    }
                    Text("${shoppingList.size} ingredients", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (shoppingList.isEmpty()) {
                    Text(
                        text = "Your shopping list is empty. Add ingredients inside recipes page!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    shoppingList.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { viewModel.toggleShoppingItem(item.id, it) },
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.ingredient} (${item.recipeTitle})",
                                    fontSize = 13.sp,
                                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeShoppingItem(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Del", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (shoppingList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.navigateTo("ShoppingList") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("profile_view_full_shopping_list_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Full Shopping Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Subscriptions Upgrade Options ---
        Text("Modify Subscription Plan", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val plans = listOf(
            Triple("Free", "1,000 Initial Credits • 10 Generations", "Free"),
            Triple("Pro", "10,000 Credits • Priority AI", "$100/mo"),
            Triple("Max", "50,000 Credits • Faster Response", "$1000/mo"),
            Triple("Max Plus", "Unlimited Credits • Dedicated Support", "$2000/mo")
        )

        plans.forEach { (planName, perks, price) ->
            val isActive = active.plan == planName
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "$planName Plan", fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(text = perks, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (isActive) {
                                viewModel.renewPlan()
                            } else {
                                selectedPlanForDialog = planName
                                showUpgradeDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = if (isActive) "Renew ($price)" else "Activate ($price)", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Transaction History ---
        Text("Usage & Billing History", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        if (transactions.isEmpty()) {
            Text("No transactions logged yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    transactions.take(10).forEach { transact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(transact.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = dateFormat.format(Date(transact.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val isAdjustment = transact.amount > 0
                            val isNegative = transact.amount < 0
                            Text(
                                text = if (transact.amount == 0) "Unlimited" else if (isAdjustment) "+${transact.amount}" else "${transact.amount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isAdjustment) MaterialTheme.colorScheme.tertiary else if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Preferences settings values ---
        Text("App Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Dark Theme Aesthetics")
            }
            Switch(
                checked = active.isDarkMode,
                onCheckedChange = { viewModel.toggleDarkMode(it) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Display Language")
            }
            val languages = listOf("English", "Spanish", "French")
            var langExp by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { langExp = true }) {
                    Text(active.language)
                }
                DropdownMenu(expanded = langExp, onDismissRequest = { langExp = false }) {
                    languages.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l) },
                            onClick = {
                                viewModel.changeLanguage(l)
                                langExp = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (active.email.trim().lowercase() == "riponprime42@gmail.com") {
            Button(
                onClick = { viewModel.navigateTo("AdminDashboard") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.AdminPanelSettings, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Admin Dashboard Panel")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- Close logoff out action ---
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout Session")
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // Upgrade Dialog popup
    if (showUpgradeDialog) {
        Dialog(onDismissRequest = { showUpgradeDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCard,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Secure Checkout", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simulate payment checkout flow for $selectedPlanForDialog Plan. Credit Card ending in 4022 will be charged.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = "4111 2222 3333 4022",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Card Details") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { showUpgradeDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                showUpgradeDialog = false
                                viewModel.selectSubscriptionPlan(selectedPlanForDialog)
                            }
                        ) {
                            Text("Authorise & Pay")
                        }
                    }
                }
            }
        }
    }
}

// --- ADMINISTRATIVE PORTAL DASHBOARD SCREEN ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(viewModel: ChefAiViewModel) {
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val allRecipes by viewModel.allRecipes.collectAsStateWithLifecycle()

    var adjustMail by remember { mutableStateOf("") }
    var adjustAmount by remember { mutableStateOf("500") }

    var creatorTitle by remember { mutableStateOf("") }
    var creatorDesc by remember { mutableStateOf("") }
    var creatorCuisine by remember { mutableStateOf("American") }
    var creatorCategory by remember { mutableStateOf("Breakfast") }
    var creatorIngredients by remember { mutableStateOf("2 cups flour | 1 cup milk") }
    var creatorInstructions by remember { mutableStateOf("Mix dry ingredients | Pour milk | Cook on medium heat") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledIconButton(onClick = { viewModel.navigateTo("Profile") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Admin Portal Dashboard", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.headlineSmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Visual Analytics of Database ---
        Text(text = "App Core Engine Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatLabel(value = "${allRecipes.size}", name = "Total Recipes")
                    StatLabel(value = "${allRecipes.count { it.isFeatured }}", name = "Featured Items")
                    StatLabel(value = "${allRecipes.count { it.isAI }}", name = "AI Generated")
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Visual progress simulated bar
                Text("Local database recipe coverage balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (allRecipes.size / 20f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Adjust Credits Form ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Adjust User Credits Balance", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = adjustMail,
                    onValueChange = { adjustMail = it },
                    label = { Text("Target User Email Address") },
                    placeholder = { Text("e.g. google_chef@gmail.com") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = adjustAmount,
                    onValueChange = { adjustAmount = it },
                    label = { Text("Credits Adjustment Delta Amount (Positive or Negative)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        val finalMail = adjustMail.trim()
                        val amt = adjustAmount.toIntOrNull() ?: 0
                        if (finalMail.isEmpty() || amt == 0) {
                            viewModel.showSnackbar("Enter valid target email and non-zero credits delta.")
                        } else {
                            viewModel.adminAdjustCredits(finalMail, amt, "Administrative adjustment delta by Admin")
                            adjustMail = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Build, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Credits Adjustment")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Add Customs Manual Recipe Form ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Create Manual Admin Recipe Entity", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = creatorTitle,
                    onValueChange = { creatorTitle = it },
                    label = { Text("Recipe Title") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = creatorDesc,
                    onValueChange = { creatorDesc = it },
                    label = { Text("Brief Flavor Description") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creatorCuisine,
                        onValueChange = { creatorCuisine = it },
                        label = { Text("Cuisine Style") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = creatorCategory,
                        onValueChange = { creatorCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = creatorIngredients,
                    onValueChange = { creatorIngredients = it },
                    label = { Text("Ingredients (Separated by |)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = creatorInstructions,
                    onValueChange = { creatorInstructions = it },
                    label = { Text("Instructions (Separated by |)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        val title = creatorTitle.trim()
                        if (title.isEmpty()) {
                            viewModel.showSnackbar("Enter recipe title first.")
                        } else {
                            val newRecipe = Recipe(
                                title = title,
                                description = creatorDesc.trim(),
                                cuisine = creatorCuisine.trim(),
                                category = creatorCategory.trim(),
                                ingredients = creatorIngredients.trim(),
                                instructions = creatorInstructions.trim(),
                                imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=500",
                                cookTimeMin = 15,
                                prepTimeMin = 10,
                                difficulty = "Medium",
                                calories = 310,
                                servings = 2,
                                rating = 4.5f
                            )
                            viewModel.adminCreateRecipe(newRecipe)
                            creatorTitle = ""
                            creatorDesc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddCircle, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Recipe to Table")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Delete Recipes Table Management List ---
        Text(text = "Manage Existing Database Recipes", fontWeight = FontWeight.Bold, style = MaterialTheme.fontFamilyTypography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allRecipes) { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${recipe.cuisine} • ${recipe.category}", fontSize = 11.sp)
                    }
                    IconButton(onClick = { viewModel.adminDeleteRecipe(recipe.id) }) {
                        Icon(Icons.Default.Delete, "Del", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun StatLabel(value: String, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.fontFamilyTypography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ShoppingListScreen(viewModel: ChefAiViewModel) {
    val shoppingList by viewModel.shoppingList.collectAsStateWithLifecycle()
    var customItemName by remember { mutableStateOf("") }
    var showDialogToClearAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // --- Back Nav + Page Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.size(36.dp)
                        .testTag("shopping_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Shopping List",
                    style = MaterialTheme.fontFamilyTypography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (shoppingList.isNotEmpty()) {
                TextButton(
                    onClick = { showDialogToClearAll = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("shopping_clear_all_button")
                ) {
                    Icon(Icons.Default.ClearAll, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Custom Ingredient Entry Form ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add Custom Ingredient",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        placeholder = { Text("e.g. Milk, Extra Garlic", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_ingredient_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            if (customItemName.trim().isNotEmpty()) {
                                viewModel.addCustomShoppingItem(customItemName.trim())
                                customItemName = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_custom_item_button")
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (shoppingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your list is empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.fontFamilyTypography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explore culinary recipes and add ingredients directly into your shopping checklist.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo("Home") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("explore_recipes_empty_shopping")
                    ) {
                        Text("Explore Recipes")
                    }
                }
            }
        } else {
            // Group ingredients by Recipe Title
            val groupedByRecipe = shoppingList.groupBy { it.recipeTitle }

            groupedByRecipe.forEach { (recipeTitle, items) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recipeTitle,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.fontFamilyTypography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Clear recipe button
                            val isCustom = recipeTitle == "Custom Items"
                            val recipeId = if (items.isNotEmpty()) items.first().recipeId else 0
                            IconButton(
                                onClick = {
                                    if (isCustom) {
                                        // For custom items, we delete each one
                                        items.forEach { viewModel.removeShoppingItem(it.id) }
                                    } else {
                                        viewModel.clearShoppingListForActiveRecipe(recipeId)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                                    .testTag("delete_recipe_ingredients_${recipeId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Recipe Ingredients",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleShoppingItem(item.id, !item.isChecked) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { viewModel.toggleShoppingItem(item.id, it) },
                                        modifier = Modifier.testTag("shopping_item_checkbox_${item.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.ingredient,
                                        fontSize = 13.sp,
                                        style = if (item.isChecked) {
                                            MaterialTheme.fontFamilyTypography.bodyMedium.copy(
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            MaterialTheme.fontFamilyTypography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeShoppingItem(item.id) },
                                    modifier = Modifier.size(24.dp)
                                        .testTag("delete_shopping_item_icon_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showDialogToClearAll) {
        AlertDialog(
            onDismissRequest = { showDialogToClearAll = false },
            title = { Text("Clear Shopping List?") },
            text = { Text("This will permanently remove all persistent ingredients from your cooking shopping checklist. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllShoppingItems()
                        showDialogToClearAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialogToClearAll = false },
                    modifier = Modifier.testTag("cancel_clear_all_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
