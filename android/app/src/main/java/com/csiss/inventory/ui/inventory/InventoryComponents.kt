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

internal val Ink = Color(0xFF182B48)
internal val Muted = Color(0xFF718198)
internal val Blue = Color(0xFF132945)
internal val Background = Color(0xFFF1F6FB)
internal val Line = Color(0xFFE0E6EF)
internal val White = Color.White
internal val Red = Color(0xFFE45465)
internal val Shape = RoundedCornerShape(16.dp)
internal val DateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

@Composable
internal fun ProductCard(product: Product, today: LocalDate, onClick: (() -> Unit)? = null) {
    val status = product.status(today)
    val modifier = Modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp), color = White
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Color(0xFFEAF3FE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center) { ProductThumbnail(product.photo, product.imagePath) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(product.category, color = Muted, fontSize = 11.sp)
                Text("${product.quantity.display()} ${product.unit}", color = Muted, fontSize = 11.sp)
            }
            Text(status.title, color = status.foreground, fontSize = 10.sp,
                modifier = Modifier.background(status.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            if (onClick != null) Text(" ›", color = Color(0xFF34B9DB))
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
            focusedContainerColor = White, unfocusedContainerColor = White,
            unfocusedBorderColor = Line, focusedBorderColor = Blue,
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            focusedLabelColor = Blue, unfocusedLabelColor = Muted
        )
    )
}

@Composable
internal fun ScreenTitle(title: String, caption: String, back: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = back, contentPadding = PaddingValues(0.dp)) {
            Text("‹ Back", color = Blue)
        }
        Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(caption, color = Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = White),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
