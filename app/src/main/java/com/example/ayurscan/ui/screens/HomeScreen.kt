package com.example.ayurscan.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ayurscan.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onHomeClick: () -> Unit = {},
    onHeartClick: () -> Unit,
    onSmileClick: () -> Unit,
    onBellClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDosha by remember { mutableStateOf<DoshaDetail?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Sample Data
    val foodCategories = remember {
        listOf(
            FoodCategory("1", "Vata-friendly Foods", R.drawable.ayurfirstimage, "Vata"),
            FoodCategory("2", "Pitta Cooling Foods", R.drawable.ayurfirstimage, "Pitta"),
            FoodCategory("3", "Kapha Light Meals", R.drawable.ayurfirstimage, "Kapha"),
            FoodCategory("4", "Tridoshic Balance", R.drawable.ayurfirstimage, "All"),
            FoodCategory("5", "Vata Breakfasts", R.drawable.ayurfirstimage, "Vata"),
            FoodCategory("6", "Pitta Teas", R.drawable.ayurfirstimage, "Pitta")
        )
    }

    // Filter Logic
    val filteredRecommendations = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            foodCategories
        } else {
            foodCategories.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.doshaType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = onHomeClick,
                onHeartClick = onHeartClick,
                onSmileClick = onSmileClick,
                onBellClick = onBellClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF7D9E46)) // Olive Green background
                .padding(innerPadding)
        ) {
            // Header
            HeaderSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )

            // Main Content Area with rounded top corners
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(Color(0xFFFCFEEA)) // Light/White background
                    .padding(16.dp)
            ) {
                // Banner
                BannerSection()

                Spacer(modifier = Modifier.height(16.dp))

                // Dosha Chips
                DoshaChipsSection(
                    onDoshaClick = { doshaName ->
                        selectedDosha = getDoshaDetails(doshaName)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recommendation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recommendation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "See All >",
                        fontSize = 14.sp,
                        color = Color.Blue,
                        modifier = Modifier.clickable { }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recommendation Grid
                RecommendationGrid(items = filteredRecommendations)
            }
        }

        // Dosha Detail Bottom Sheet
        if (selectedDosha != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedDosha = null },
                sheetState = sheetState
            ) {
                DoshaDetailContent(dosha = selectedDosha!!)
            }
        }
    }
}

@Composable
fun HeaderSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile Image (Replaced Placeholder)
            Surface(
                modifier = Modifier
                    .size(50.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(50.dp),
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hi, Swati",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Welcome!",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search Dosha/Food", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .width(160.dp) // Slightly wider for utility
                .height(50.dp)
        )
    }
}

@Composable
fun BannerSection() {
    // Pulse Animation for Text
    val infiniteTransition = rememberInfiniteTransition(label = "bannerPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA726)), // Orange
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Your Diet Journey,",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Starts Here!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    modifier = Modifier.scale(scale) // Apply pulse animation
                )
            }
            // 3D Character Placeholder (Can be replaced with Image later)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Face, 
                    contentDescription = null, 
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun DoshaChipsSection(onDoshaClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DoshaChip(text = "Vata", onClick = { onDoshaClick("Vata") }, modifier = Modifier.weight(1f))
        DoshaChip(text = "Pitta", onClick = { onDoshaClick("Pitta") }, modifier = Modifier.weight(1f))
        DoshaChip(text = "Kapha", onClick = { onDoshaClick("Kapha") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DoshaChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF8DA358)) // Olive Green
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecommendationGrid(items: List<FoodCategory>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            FoodCategoryCard(item)
        }
    }
}

@Composable
fun FoodCategoryCard(item: FoodCategory) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(160.dp)
            .clickable { /* Navigate to detail */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            Text(
                text = item.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun DoshaDetailContent(dosha: DoshaDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text(
            text = dosha.name,
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF2C3E28),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = dosha.description, style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SectionHeader("Key Characteristics")
        BulletList(dosha.characteristics)
        
        SectionHeader("Foods to Eat")
        BulletList(dosha.foodsToEat)
        
        SectionHeader("Foods to Avoid")
        BulletList(dosha.foodsToAvoid)
        
        SectionHeader("Lifestyle Tips")
        BulletList(dosha.lifestyleTips)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF5D7052),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun BulletList(items: List<String>) {
    items.forEach { item ->
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(text = "•", modifier = Modifier.padding(end = 8.dp), color = Color.DarkGray)
            Text(text = item, color = Color.Black)
        }
    }
}

// Data Models & Helpers

data class DoshaDetail(
    val name: String,
    val description: String,
    val characteristics: List<String>,
    val foodsToEat: List<String>,
    val foodsToAvoid: List<String>,
    val lifestyleTips: List<String>
)

data class FoodCategory(
    val id: String,
    val name: String,
    val imageRes: Int,
    val doshaType: String
)

fun getDoshaDetails(name: String): DoshaDetail {
    return when (name) {
        "Vata" -> DoshaDetail(
            "Vata Dosha",
            "Vata embodies the energy of movement and is composed of Ether and Air. It governs breathing, blinking, tissue movement, and pulsation of the heart.",
            listOf("Creative, quick learner", "Tendency towards dry skin", "Sensitive to cold", "Irregular appetite"),
            listOf("Warm soups and stews", "Oats, rice, wheat", "Avocados, dairy, nuts", "Warm spices like ginger"),
            listOf("Dry foods (crackers, popcorn)", "Cold raw salads", "Beans causing gas", "Bitter tastes"),
            listOf("Maintain a regular routine", "Keep warm and stay hydrated", "Gentle exercise like Yoga")
        )
        "Pitta" -> DoshaDetail(
            "Pitta Dosha",
            "Pitta represents the energy of transformation and digestion, composed of Fire and Water. It governs digestion, absorption, assimilation, and body temperature.",
            listOf("Intelligent, focused", "Medium build", "Sensitive to heat", "Strong appetite"),
            listOf("Sweet fruits (melons, grapes)", "Cucumber, leafy greens", "Dairy products", "Mild spices like coriander"),
            listOf("Spicy foods", "Sour fruits", "Fermented foods", "Excessive salt"),
            listOf("Stay cool", "Avoid skipping meals", "Spend time in nature (moonlight)")
        )
        "Kapha" -> DoshaDetail(
            "Kapha Dosha",
            "Kapha supplies the water for all body parts and systems. It lubricates joints, moisturizes the skin, and maintains immunity.",
            listOf("Calm, grounded", "Detailed memory", "Tendency to gain weight", "Dislikes cold/damp weather"),
            listOf("Light fruits (apples, pears)", "Grains like barley, quinoa", "Bitter vegetables", "Pungent spices"),
            listOf("Heavy nuts and seeds", "Excessive dairy", "Sweet and salty foods", "Fried foods"),
            listOf("Get plenty of exercise", "Variety in routine", "Keep warm and dry")
        )
        else -> DoshaDetail("Unknown", "", emptyList(), emptyList(), emptyList(), emptyList())
    }
}

@Composable
fun BottomNavigationBar(
    onHomeClick: () -> Unit,
    onHeartClick: () -> Unit,
    onSmileClick: () -> Unit,
    onBellClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFFCFEEA),
        contentColor = Color.Black,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(30.dp), tint = Color(0xFFFFA726)) }, // Active color
            selected = true,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Details", modifier = Modifier.size(30.dp)) },
            selected = false,
            onClick = onHeartClick,
             colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Face, contentDescription = "Score", modifier = Modifier.size(30.dp)) },
            selected = false,
            onClick = onSmileClick,
             colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Notifications, contentDescription = "Quiz", modifier = Modifier.size(30.dp)) },
            selected = false,
            onClick = onBellClick,
             colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}
