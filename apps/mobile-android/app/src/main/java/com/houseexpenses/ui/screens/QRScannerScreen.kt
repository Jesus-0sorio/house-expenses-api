package com.houseexpenses.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.houseexpenses.ui.theme.EmeraldPrimary
import com.houseexpenses.ui.theme.PurpleAccent
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQRScanned: (description: String, amount: String) -> Unit,
    onSaveDirectly: ((description: String, amount: Double, scope: String) -> Unit)? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualText by remember { mutableStateOf("") }
    var detectedInvoice by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedScope by remember { mutableStateOf("HOUSEHOLD") }

    val formatCOP = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        format
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear Factura DIAN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val scanner = BarcodeScanning.getClient()
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && detectedInvoice == null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val rawValue = barcode.rawValue ?: continue
                                                    val parsed = parseDianPayload(rawValue)
                                                    if (parsed != null) {
                                                        detectedInvoice = parsed
                                                        break
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Retícula de Escaneo
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(2.dp, EmeraldPrimary, RoundedCornerShape(16.dp))
                            .align(Alignment.Center)
                    )

                    Text(
                        text = "Apunta al código QR de la factura",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Se requiere permiso de cámara para escanear facturas")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                            Text("Conceder Permiso")
                        }
                    }
                }
            }

            // Entrada manual de respaldo
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "¿No puedes escanear? Pega el texto del QR:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = { Text("NumFac: ... ValTolFac: ...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Button(
                        onClick = {
                            val parsed = parseDianPayload(manualText)
                            if (parsed != null) {
                                detectedInvoice = parsed
                            }
                        },
                        enabled = manualText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Procesar Texto QR")
                    }
                }
            }
        }
    }

    // Modal BottomSheet en 2 Taps para Guardar Inmediatamente
    detectedInvoice?.let { invoice ->
        ModalBottomSheet(
            onDismissRequest = { detectedInvoice = null },
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Factura DIAN Detectada",
                    fontSize = 14.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )

                val amountNum = invoice.second.toDoubleOrNull() ?: 0.0

                Text(
                    text = formatCOP.format(amountNum),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = invoice.first,
                    fontSize = 15.sp,
                    color = Color.LightGray
                )

                Text(
                    text = "¿Cómo deseas registrar esta compra?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { selectedScope = "HOUSEHOLD" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedScope == "HOUSEHOLD") PurpleAccent else Color(0xFF334155)
                        )
                    ) {
                        Text("Hogar (50/50)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedScope = "PERSONAL" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedScope == "PERSONAL") EmeraldPrimary else Color(0xFF334155)
                        )
                    ) {
                        Text("Personal", fontWeight = FontWeight.Bold, color = if (selectedScope == "PERSONAL") Color.Black else Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Boton Confirmar Guardado (2 Taps)
                Button(
                    onClick = {
                        if (onSaveDirectly != null) {
                            onSaveDirectly(invoice.first, amountNum, selectedScope)
                        } else {
                            onQRScanned(invoice.first, invoice.second)
                        }
                        detectedInvoice = null
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(
                        text = "Confirmar y Guardar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Boton Editar Mas Detalles
                TextButton(
                    onClick = {
                        val current = invoice
                        detectedInvoice = null
                        onQRScanned(current.first, current.second)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Editar más detalles en formulario", color = Color.LightGray, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun parseDianPayload(text: String): Pair<String, String>? {
    fun match(pattern: String): String? {
        val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    val numFac = match("NumFac\\s*:\\s*([^\\r\\n]+)") ?: "Factura"
    val nit = match("NitFac\\s*:\\s*([^\\r\\n]+)") ?: ""
    val total = match("ValTolFac\\s*:\\s*([0-9.,]+)") ?: match("ValFac\\s*:\\s*([0-9.,]+)")

    val vendorName = when {
        nit.startsWith("900480569") -> "D1 S.A.S."
        nit.startsWith("890900608") -> "Éxito"
        nit.startsWith("860002193") -> "Carulla"
        nit.startsWith("800242106") -> "Jumbo"
        nit.startsWith("900215904") -> "Tiendas Ara"
        else -> if (nit.isNotBlank()) "Comercio (NIT: $nit)" else "Factura DIAN"
    }

    val cleanTotal = total?.replace(",", ".") ?: "0"
    return Pair("$vendorName - $numFac", cleanTotal)
}
