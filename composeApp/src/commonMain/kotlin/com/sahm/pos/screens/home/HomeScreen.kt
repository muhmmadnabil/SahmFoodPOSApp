package com.sahm.pos.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sahm.pos.theme.BorderDefault
import com.sahm.pos.theme.CardBackground
import com.sahm.pos.theme.PrimaryOrange
import com.sahm.pos.theme.ScreenBackground
import com.sahm.pos.theme.TextPrimary
import com.sahm.pos.theme.TextSecondary
import com.sahm.pos.utils.ScreenType
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    screenType: ScreenType,
) {
    data class MenuItem(
        val category: String,
        val id: Int,
        val price: Double,
        val image: String,
        val name: String,
        val description: String,
    )

    val menuItems = remember {
        listOf(
            MenuItem(
                id = 1,
                category = "Burgers",
                price = 115.0,
                image = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=700&q=85",
                name = "King Mo",
                description = "Juicy beef, cheddar, pickles, lettuce, onions, mo sauce.",
            ),
            MenuItem(
                id = 2,
                category = "Burgers",
                price = 95.0,
                image = "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=700&q=85",
                name = "Classic Cheeseburger",
                description = "Beef patty, cheese, lettuce, tomato, onion.",
            ),
            MenuItem(
                id = 3,
                category = "Burgers",
                price = 125.0,
                image = "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?auto=format&fit=crop&w=700&q=85",
                name = "Double Double",
                description = "Two beef patties, double cheese, pickles, onions, special sauce.",
            ),
            MenuItem(
                id = 4,
                category = "Chicken",
                price = 105.0,
                image = "https://images.unsplash.com/photo-1606755962773-d324e0a13086?auto=format&fit=crop&w=700&q=85",
                name = "Chicken Buster",
                description = "Crispy chicken, lettuce, cheddar, mayo.",
            ),
            MenuItem(
                id = 5,
                category = "Pizza",
                price = 100.0,
                image = "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=700&q=85",
                name = "Chicken Pizza",
                description = "Grilled chicken, cheese, peppers.",
            ),
            MenuItem(
                id = 6,
                category = "Sides",
                price = 55.0,
                image = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=700&q=85",
                name = "Cheesy Fries",
                description = "Crispy fries, cheese sauce, mo seasoning.",
            ),
            MenuItem(
                id = 7,
                category = "Sides",
                price = 45.0,
                image = "https://images.unsplash.com/photo-1639024471283-03518883512d?auto=format&fit=crop&w=700&q=85",
                name = "Onion Rings",
                description = "Crispy golden onion rings served with dip.",
            ),
            MenuItem(
                id = 8,
                category = "Drinks",
                price = 40.0,
                image = "https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=700&q=85",
                name = "Orange Juice",
                description = "Freshly squeezed orange juice.",
            ),
            MenuItem(
                id = 9,
                category = "Desserts",
                price = 50.0,
                image = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=700&q=85",
                name = "Chocolate Mousse",
                description = "Rich chocolate mousse with crunchy crisp pearls.",
            ),
        )
    }
    val categories = remember(menuItems) { listOf("All") + menuItems.map { it.category }.distinct() }
    val paymentTypes = remember { listOf("Cash", "Card") }
    val quantities = remember {
        mutableStateMapOf(
            2 to 1,
            6 to 1,
            8 to 2,
        )
    }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPaymentType by remember { mutableStateOf(paymentTypes.first()) }
    var searchText by remember { mutableStateOf("") }
    val isTablet = screenType == ScreenType.Tablet
    val spacing = if (isTablet) 20.dp else 12.dp
    val pagePadding = if (isTablet) 32.dp else 18.dp
    val filteredItems = menuItems.filter { item ->
        (selectedCategory == "All" || item.category == selectedCategory) &&
            item.name.contains(searchText, ignoreCase = true)
    }
    val orderItems = menuItems.filter { item -> (quantities[item.id] ?: 0) > 0 }
    val subtotal = orderItems.sumOf { item -> item.price * (quantities[item.id] ?: 0) }
    val tax = subtotal * 0.14
    val total = subtotal + tax

    fun formatPrice(value: Double): String {
        val cents = (value * 100).roundToInt()
        val pounds = cents / 100
        val piasters = (cents % 100).toString().padStart(2, '0')
        return "EGP $pounds.$piasters"
    }

    fun updateQuantity(itemId: Int, quantity: Int) {
        if (quantity <= 0) {
            quantities.remove(itemId)
        } else {
            quantities[itemId] = quantity
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .safeContentPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pagePadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = PrimaryOrange)) { append("POS") }
                            append(" System")
                        },
                        modifier = Modifier.weight(0.22f),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        modifier = Modifier.weight(0.56f),
                        shape = RoundedCornerShape(10.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, BorderDefault),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Search",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            BasicTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchText.isBlank()) {
                                        Text(
                                            text = "Search menu items...",
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(0.16f),
                        shape = RoundedCornerShape(10.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, BorderDefault),
                    ) {
                        Text(
                            text = "Settings",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = PrimaryOrange)) { append("POS") }
                            append(" System")
                        },
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, BorderDefault),
                    ) {
                        Text(
                            text = "Settings",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CardBackground,
                    border = BorderStroke(1.dp, BorderDefault),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Search",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 18.sp,
                            ),
                            decorationBox = { innerTextField ->
                                if (searchText.isBlank()) {
                                    Text(
                                        text = "Search items...",
                                        color = TextSecondary,
                                        fontSize = 18.sp,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 10.dp),
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier.clickable { selectedCategory = category },
                        shape = RoundedCornerShape(if (isTablet) 8.dp else 12.dp),
                        color = if (isSelected) PrimaryOrange else CardBackground,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryOrange else BorderDefault),
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(
                                horizontal = if (isTablet) 28.dp else 18.dp,
                                vertical = if (isTablet) 12.dp else 11.dp,
                            ),
                            color = if (isSelected) Color.White else TextPrimary,
                            fontSize = if (isTablet) 14.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardBackground,
                                    border = BorderStroke(1.dp, BorderDefault),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                    ) {
                                        AsyncImage(
                                            model = item.image,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.35f)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = item.name,
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = item.description,
                                            modifier = Modifier.padding(top = 6.dp),
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = formatPrice(item.price),
                                                modifier = Modifier.weight(1f),
                                                color = PrimaryOrange,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Surface(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        updateQuantity(
                                                            item.id,
                                                            (quantities[item.id] ?: 0) + 1,
                                                        )
                                                    },
                                                shape = CircleShape,
                                                color = PrimaryOrange,
                                            ) {
                                                Text(
                                                    text = "+",
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = CardBackground,
                            border = BorderStroke(1.dp, BorderDefault),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Select Payment Method",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    paymentTypes.forEach { payment ->
                                        val isSelected = selectedPaymentType == payment
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedPaymentType = payment },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) PrimaryOrange.copy(alpha = 0.08f) else CardBackground,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isSelected) PrimaryOrange else BorderDefault,
                                            ),
                                        ) {
                                            Text(
                                                text = payment,
                                                modifier = Modifier.padding(vertical = 18.dp),
                                                color = TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = {},
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                ) {
                                    Text(
                                        text = "Confirm Payment",
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(14.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, BorderDefault),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "Current Order",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(orderItems, key = { it.id }) { item ->
                                    val quantity = quantities[item.id] ?: 0
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            AsyncImage(
                                                model = item.image,
                                                contentDescription = item.name,
                                                modifier = Modifier
                                                    .weight(0.18f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(9.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                            Column(
                                                modifier = Modifier.weight(0.42f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = item.name,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = CardBackground,
                                                    border = BorderStroke(1.dp, BorderDefault),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                                                    ) {
                                                        Text(
                                                            text = "-",
                                                            modifier = Modifier.clickable {
                                                                updateQuantity(item.id, quantity - 1)
                                                            },
                                                            color = PrimaryOrange,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                        Text(
                                                            text = quantity.toString(),
                                                            color = TextPrimary,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                        Text(
                                                            text = "+",
                                                            modifier = Modifier.clickable {
                                                                updateQuantity(item.id, quantity + 1)
                                                            },
                                                            color = PrimaryOrange,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = formatPrice(item.price * quantity),
                                                modifier = Modifier.weight(0.28f),
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.End,
                                            )
                                            Text(
                                                text = "x",
                                                modifier = Modifier
                                                    .weight(0.06f)
                                                    .clickable { updateQuantity(item.id, 0) },
                                                color = TextSecondary,
                                                fontSize = 18.sp,
                                                textAlign = TextAlign.End,
                                            )
                                        }
                                        HorizontalDivider(color = BorderDefault)
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text("Subtotal", Modifier.weight(1f), color = TextSecondary, fontSize = 14.sp)
                                    Text(formatPrice(subtotal), color = TextSecondary, fontSize = 14.sp)
                                }
                                Row(Modifier.fillMaxWidth()) {
                                    Text("Tax (14%)", Modifier.weight(1f), color = TextSecondary, fontSize = 14.sp)
                                    Text(formatPrice(tax), color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                            HorizontalDivider(color = BorderDefault)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    text = "Total",
                                    modifier = Modifier.weight(1f),
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = formatPrice(total),
                                    color = PrimaryOrange,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                )
                            }
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            ) {
                                Text(
                                    text = "Make Order",
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardBackground,
                            border = BorderStroke(1.dp, BorderDefault),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                            ) {
                                AsyncImage(
                                    model = item.image,
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.45f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Text(
                                    text = item.name,
                                    modifier = Modifier.padding(top = 10.dp),
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.description,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = formatPrice(item.price),
                                        modifier = Modifier.weight(1f),
                                        color = PrimaryOrange,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Surface(
                                        modifier = Modifier.clickable {
                                            updateQuantity(
                                                item.id,
                                                (quantities[item.id] ?: 0) + 1,
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = PrimaryOrange,
                                    ) {
                                        Text(
                                            text = "+",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            color = Color.White,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = CardBackground,
                    border = BorderStroke(1.dp, BorderDefault),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Current Order",
                                modifier = Modifier.weight(1f),
                                color = TextPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryOrange.copy(alpha = 0.08f),
                            ) {
                                Text(
                                    text = "${orderItems.size} items",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = PrimaryOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            orderItems.forEach { item ->
                                val quantity = quantities[item.id] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AsyncImage(
                                        model = item.image,
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .weight(0.13f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Text(
                                        text = item.name,
                                        modifier = Modifier.weight(0.34f),
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Surface(
                                        modifier = Modifier.weight(0.24f),
                                        shape = RoundedCornerShape(9.dp),
                                        color = CardBackground,
                                        border = BorderStroke(1.dp, BorderDefault),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "-",
                                                modifier = Modifier.clickable {
                                                    updateQuantity(item.id, quantity - 1)
                                                },
                                                color = PrimaryOrange,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = quantity.toString(),
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = "+",
                                                modifier = Modifier.clickable {
                                                    updateQuantity(item.id, quantity + 1)
                                                },
                                                color = PrimaryOrange,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatPrice(item.price * quantity),
                                        modifier = Modifier.weight(0.2f),
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "x",
                                        modifier = Modifier
                                            .weight(0.05f)
                                            .clickable { updateQuantity(item.id, 0) },
                                        color = TextSecondary,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.End,
                                    )
                                }
                                HorizontalDivider(color = BorderDefault)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            paymentTypes.forEach { payment ->
                                val isSelected = selectedPaymentType == payment
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { selectedPaymentType = payment },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryOrange.copy(alpha = 0.08f) else CardBackground,
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryOrange else BorderDefault),
                                ) {
                                    Text(
                                        text = payment,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = if (isSelected) PrimaryOrange else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("Subtotal", Modifier.weight(1f), color = TextSecondary, fontSize = 15.sp)
                                Text(formatPrice(subtotal), color = TextSecondary, fontSize = 15.sp)
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text("Tax (14%)", Modifier.weight(1f), color = TextSecondary, fontSize = 15.sp)
                                Text(formatPrice(tax), color = TextSecondary, fontSize = 15.sp)
                            }
                        }
                        HorizontalDivider(color = BorderDefault)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = "Total",
                                modifier = Modifier.weight(1f),
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = formatPrice(total),
                                color = PrimaryOrange,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                            )
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        ) {
                            Text(
                                text = "Make Order",
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}