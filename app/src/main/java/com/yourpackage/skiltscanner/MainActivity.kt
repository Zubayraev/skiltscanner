package com.yourpackage.skiltscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yourpackage.skiltscanner.database.AppDatabase
import com.yourpackage.skiltscanner.database.ScanHistory
import com.yourpackage.skiltscanner.database.ScanRepository
import com.yourpackage.skiltscanner.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val vegvesenApi = VegvesenApi.create()

    // Database
    private lateinit var repository: ScanRepository

    // VIKTIG: Sett inn din API-nøkkel her!
    private val API_KEY = "326da191-a32a-4892-9a7b-495cd60e6298"

    private var isProcessing = false
    private var lastProcessedPlate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialiser database
        val database = AppDatabase.getDatabase(this)
        repository = ScanRepository(database.scanHistoryDao())

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Klikk for å vise historikk
        binding.cardInfo.setOnLongClickListener {
            showHistoryDialog()
            true
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Kunne ikke starte kamera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isProcessing) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text
                    val licensePlate = extractLicensePlate(detectedText)

                    if (licensePlate != null && licensePlate != lastProcessedPlate) {
                        lastProcessedPlate = licensePlate
                        isProcessing = true
                        fetchVehicleInfo(licensePlate)
                    }
                }
                .addOnFailureListener {
                    // Ignorer feil
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun extractLicensePlate(text: String): String? {
        // Norsk registreringsnummer: 2 bokstaver + 5 tall (f.eks AB12345)
        val regex = Regex("[A-Z]{2}\\s?\\d{5}")
        val match = regex.find(text.uppercase().replace(" ", ""))
        return match?.value?.replace(" ", "")
    }

    private fun fetchVehicleInfo(licensePlate: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvLicensePlate.text = licensePlate

                println("=== STARTER API KALL ===")
                println("Kjennemerke: $licensePlate")

                val response = withContext(Dispatchers.IO) {
                    vegvesenApi.getVehicleInfo(licensePlate, API_KEY)
                }

                // DEBUG LOGGING
                println("=== API RESPONS ===")
                println("kjoretoydataListe size: ${response.kjoretoydataListe?.size}")

                val vehicleData = response.kjoretoydataListe?.firstOrNull()
                println("vehicleData er null? ${vehicleData == null}")

                if (vehicleData != null) {
                    println("godkjenning: ${vehicleData.godkjenning}")
                    println("tekniskGodkjenning: ${vehicleData.godkjenning?.tekniskGodkjenning}")
                    println("tekniskeData: ${vehicleData.godkjenning?.tekniskGodkjenning?.tekniskeData}")
                    println("generelt: ${vehicleData.godkjenning?.tekniskGodkjenning?.tekniskeData?.generelt}")

                    val generelt = vehicleData.godkjenning?.tekniskGodkjenning?.tekniskeData?.generelt
                    println("merke list: ${generelt?.merke}")
                    println("handelsbetegnelse: ${generelt?.handelsbetegnelse}")
                    println("aarsmodell: ${generelt?.aarsmodell}")
                    println("periodiskKjoretoyKontroll: ${vehicleData.periodiskKjoretoyKontroll}")
                    println("kontrollfrist: ${vehicleData.periodiskKjoretoyKontroll?.kontrollfrist}")
                }
                println("=== SLUTT RESPONS ===")

                binding.progressBar.visibility = View.GONE

                if (vehicleData != null) {
                    val merke = vehicleData.godkjenning?.tekniskGodkjenning?.tekniskeData?.generelt?.merke?.firstOrNull()?.merke ?: "Ukjent"
                    val modell = vehicleData.godkjenning?.tekniskGodkjenning?.tekniskeData?.generelt?.handelsbetegnelse?.firstOrNull() ?: "Ukjent"
                    val aarsmodell = vehicleData.godkjenning?.tekniskGodkjenning?.tekniskeData?.generelt?.aarsmodell ?: "Ukjent"
                    val euKontroll = vehicleData.periodiskKjoretoyKontroll?.kontrollfrist ?: "Ikke tilgjengelig"

                    println("=== EKSTRAHERT DATA ===")
                    println("Merke: $merke")
                    println("Modell: $modell")
                    println("Årsmodell: $aarsmodell")
                    println("EU: $euKontroll")
                    println("=======================")

                    updateUI(merke, modell, aarsmodell, euKontroll)
                    saveToDatabase(licensePlate, merke, modell, aarsmodell, euKontroll)
                } else {
                    println("vehicleData er NULL!")
                    showNoDataFound()
                }

                delay(5000)
                isProcessing = false

            } catch (e: retrofit2.HttpException) {
                binding.progressBar.visibility = View.GONE
                val errorBody = e.response()?.errorBody()?.string()
                println("=== HTTP ERROR ===")
                println("Status code: ${e.code()}")
                println("Error body: $errorBody")
                println("==================")

                Toast.makeText(
                    this@MainActivity,
                    "HTTP feil ${e.code()}: Kunne ikke hente data",
                    Toast.LENGTH_LONG
                ).show()
                delay(3000)
                isProcessing = false

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                println("=== GENERELL FEIL ===")
                println("Exception: ${e.javaClass.simpleName}")
                println("Message: ${e.message}")
                e.printStackTrace()
                println("=====================")

                Toast.makeText(
                    this@MainActivity,
                    "Feil: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                delay(3000)
                isProcessing = false
            }
        }
    }

    private fun updateUI(merke: String, modell: String, aarsmodell: String, euKontroll: String) {
        binding.tvMerke.text = "Merke: $merke"
        binding.tvModell.text = "Modell: $modell"
        binding.tvAarsmodell.text = "Årsmodell: $aarsmodell"
        binding.tvEuOk.text = "EU-kontroll: $euKontroll"
    }

    private fun showNoDataFound() {
        binding.tvMerke.text = "Merke: Ingen data funnet"
        binding.tvModell.text = "Modell: -"
        binding.tvAarsmodell.text = "Årsmodell: -"
        binding.tvEuOk.text = "EU-kontroll: -"
    }

    private fun saveToDatabase(plate: String, merke: String, modell: String, aarsmodell: String, euKontroll: String) {
        lifecycleScope.launch {
            val scanHistory = ScanHistory(
                licensePlate = plate,
                merke = merke,
                modell = modell,
                aarsmodell = aarsmodell,
                euKontroll = euKontroll
            )
            repository.insert(scanHistory)
            Toast.makeText(this@MainActivity, "Lagret i historikk", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHistoryDialog() {
        lifecycleScope.launch {
            val count = repository.getCount()

            if (count == 0) {
                Toast.makeText(this@MainActivity, "Ingen historikk enda", Toast.LENGTH_SHORT).show()
                return@launch
            }

            repository.allHistory.collect { historyList ->
                if (historyList.isNotEmpty()) {
                    val items = historyList.map { history ->
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        val date = dateFormat.format(Date(history.timestamp))
                        "${history.licensePlate} - ${history.merke} ${history.modell} ($date)"
                    }.toTypedArray()

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Skannings Historikk (${historyList.size})")
                        .setItems(items) { _, which ->
                            val selected = historyList[which]
                            displayHistoryItem(selected)
                        }
                        .setNegativeButton("Lukk", null)
                        .setNeutralButton("Slett alt") { _, _ ->
                            confirmDeleteAll()
                        }
                        .show()
                }
            }
        }
    }

    private fun displayHistoryItem(history: ScanHistory) {
        binding.tvLicensePlate.text = history.licensePlate
        binding.tvMerke.text = "Merke: ${history.merke}"
        binding.tvModell.text = "Modell: ${history.modell}"
        binding.tvAarsmodell.text = "Årsmodell: ${history.aarsmodell}"
        binding.tvEuOk.text = "EU-kontroll: ${history.euKontroll}"
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Slett historikk")
            .setMessage("Er du sikker på at du vil slette all historikk?")
            .setPositiveButton("Ja") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteAll()
                    Toast.makeText(this@MainActivity, "Historikk slettet", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Nei", null)
            .show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Tillatelser ikke gitt", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textRecognizer.close()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}