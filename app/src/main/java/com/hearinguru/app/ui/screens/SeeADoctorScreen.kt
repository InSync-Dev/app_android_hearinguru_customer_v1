package com.hearinguru.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hearinguru.app.R


private const val HEARINGURU_WHATSAPP = "918886111694"
private const val HEARINGURU_PHONE    = "+918886111694"


@Composable
fun SeeADoctorScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {

        // Doctor image
        Image(
            painter            = painterResource(R.drawable.doctor),
            contentDescription = null,
            modifier           = Modifier.fillMaxWidth().height(220.dp),
            contentScale       = ContentScale.Crop
        )

        // Flow steps strip
        Surface(
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FlowStep("You share\nyour concern")
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                FlowStep("Specialist\ncalls you")
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                FlowStep("You get\na solution")
            }
        }

        // Headline + trust badges
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                "Struggling to hear?",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "A specialist will listen, review your concern, and guide you — at no cost.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrustBadge("100% Free")
                TrustBadge("Within 24 hrs")
                TrustBadge("Private")
            }
        }

        // Primary CTA — Call
        Button(
            onClick  = {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$HEARINGURU_PHONE"))
                context.startActivity(dialIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Call, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Call a Doctor Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Talk to a specialist right away", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
            }
        }

        Spacer(Modifier.height(10.dp))

        // Secondary CTA — WhatsApp
        OutlinedButton(
            onClick  = {
                val whatsappUri = Uri.parse("https://wa.me/$HEARINGURU_WHATSAPP?text=Hi+HearinGuru%2C+I+need+help+with+my+hearing.")
                val waIntent    = Intent(Intent.ACTION_VIEW, whatsappUri)
                context.startActivity(waIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Chat, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Chat on WhatsApp", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(20.dp))

        // Simple inquiry form
        ContactForm()
    }
}


@Composable
private fun ContactForm() {
    var name      by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var concern   by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Or send us a message",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "We'll call you back within 24 hours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (submitted) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                    Text(
                        "Request received! We'll call you within 24 hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        } else {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Full name") },
                leadingIcon   = { Icon(Icons.Default.Person, null) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value          = phone,
                onValueChange  = { phone = it },
                label          = { Text("Phone number") },
                leadingIcon    = { Icon(Icons.Default.Phone, null) },
                modifier       = Modifier.fillMaxWidth(),
                singleLine     = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape          = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = concern,
                onValueChange = { concern = it },
                label         = { Text("Describe your hearing concern") },
                leadingIcon   = { Icon(Icons.Default.Message, null) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3,
                maxLines      = 5,
                shape         = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { if (name.isNotBlank() && phone.isNotBlank()) submitted = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = name.isNotBlank() && phone.isNotBlank(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request Callback", fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun FlowStep(label: String) {
    Text(
        text       = label,
        style      = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign  = TextAlign.Center,
        lineHeight = 14.sp
    )
}


@Composable
private fun TrustBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            label,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
