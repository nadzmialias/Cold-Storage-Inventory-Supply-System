package com.csiss.inventory.ui.inventory

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csiss.inventory.data.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

internal val Ink = Color(0xFF153154)
internal val Muted = Color(0xFF526A88)
internal val Blue = Color(0xFF1478ED)
internal val Background = Color(0xFFF4F8FD)
internal val Line = Color(0xFFDCE7F4)
internal val White = Color.White
internal val Red = Color(0xFFC32D56)
internal val Shape = RoundedCornerShape(12.dp)
internal val DateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

@Composable
internal fun ProductCard(product: Product, today: LocalDate, onClick: (() -> Unit)? = null) {
    val status = product.status(today)
    val modifier = Modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = Shape, color = White, border = BorderStroke(1.dp, Line)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFEAF3FE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) { ProductThumbnail(product.photo, product.imagePath) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(product.category, color = Muted, fontSize = 11.sp)
                Text("${product.quantity.display()} ${product.unit}", color = Muted, fontSize = 11.sp)
            }
            Text(status.title, color = status.foreground, fontSize = 10.sp,
                modifier = Modifier.background(status.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            if (onClick != null) Text(" ›", color = Blue)
        }
    }
}

@Composable
internal fun ProductThumbnail(uri: Uri?, imagePath: String?) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(null, uri, imagePath, resolver) {
        value = null
        value = withContext(Dispatchers.IO) {
            try {
                when {
                    !imagePath.isNullOrBlank() -> {
                        val bytes = SupabaseClientProvider.client.storage
                            .from("product-images")
                            .downloadAuthenticated(imagePath)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }

                    uri != null -> resolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }

                    else -> null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w("InventoryImage", error.javaClass.simpleName)
                null
            }
        }
    }

    val image = bitmap
    if (image == null) {
        Text("❄", color = Blue)
    } else {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = "Product photo",
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
internal fun Field(value: String, onChange: (String) -> Unit, label: String,
                  keyboard: KeyboardType = KeyboardType.Text, placeholder: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        placeholder = if (placeholder.isEmpty()) null else { { Text(placeholder) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true, shape = Shape, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = White, unfocusedContainerColor = White, unfocusedBorderColor = Line
        )
    )
}

@Composable
internal fun ScreenTitle(title: String, caption: String, back: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back) { Text("‹ Back") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(caption, color = Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, shape = Shape, contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = White),
        modifier = Modifier.fillMaxWidth().height(46.dp).background(
            Brush.horizontalGradient(listOf(Blue, Color(0xFF459DED))), Shape
        )
    ) {
        Text(label, color = White, fontWeight = FontWeight.SemiBold)
    }
}
