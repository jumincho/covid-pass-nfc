package com.jumincho.cvpass.ocr

import android.content.Context
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Recognises the text on a certificate image. */
fun interface CertificateTextReader {

    /** Returns the text on the image at [imageUri], one recognised line per line. */
    suspend fun read(imageUri: String): String
}

/**
 * [CertificateTextReader] using ML Kit's on-device Korean text recognition model, which is
 * bundled with the app: images never leave the device and no API key is needed.
 */
class MlKitCertificateTextReader(private val context: Context) : CertificateTextReader {

    private val recognizer by lazy { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }

    override suspend fun read(imageUri: String): String {
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, imageUri.toUri()) }
        val text = recognizer.process(image).await()
        return text.textBlocks.flatMap { it.lines }.joinToString("\n") { it.text }
    }
}
