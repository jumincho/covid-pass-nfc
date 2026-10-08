package com.jumincho.cvpass.core.pass

/** Recognises the text on an image of a vaccination certificate. */
fun interface CertificateTextReader {

    /**
     * Returns the text on the image that [imageUri] identifies, one recognised line per
     * line. Throws if the image cannot be opened or its text cannot be recognised.
     */
    suspend fun read(imageUri: String): String
}
