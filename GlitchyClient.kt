import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

fun main() {
    val client = Client("http://127.0.0.1:8080")

    try {
        println("Starting download from glitchy server...")
        val data = client.downloadCompleteFile()
        if (data.isEmpty()) {
            println("Download failed")
            return
        }
        // Calculate SHA-256 hash
        val hash = calculateSha256(data)
        println("Download complete! ${data.size} bytes received.")
        println("SHA-256 hash of downloaded data: $hash")

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}



class Client(private val host: String) {
    companion object {
        const val CHUNK_SIZE = 64 * 1024 // 64KB chunks to avoid glitchy behavior
        const val MAX_RETRIES = 20
        const val MAX_SIZE_RETRIES = 5
    }

    fun downloadCompleteFile(): ByteArray {

        // determine the total size with retries in case of an error
        var totalSizeTemp = -1;
        var sizeRetries = 0;
        while (totalSizeTemp < 0 && sizeRetries < MAX_SIZE_RETRIES) {
            totalSizeTemp = getContentLength()
            sizeRetries++
        }

        val totalSize = totalSizeTemp;
        // error
        if (totalSize <= 0) {
            return ByteArray(0)
        }

        println("Total content size: $totalSize bytes")
        println("Starting download in chunks of $CHUNK_SIZE bytes...")

        // Download all chunks
        var retries = 0
        val output = ByteArrayOutputStream(totalSize)
        var start = 0
        var downloadedBytes = 0;
        var end = minOf( CHUNK_SIZE - 1, totalSize - 1)

        // get next chunks with retries count
        while (start < totalSize && retries < MAX_RETRIES) {
            try {
                val data = downloadRange(start, end)
                // Check if we received the expected amount of data
                if (data.size == end - start + 1) {
                    output.write(data)
                    downloadedBytes += data.size

                    // move to next chunk
                    start = end + 1
                    end = minOf(start + CHUNK_SIZE - 1, totalSize - 1)
                } else {
                    println("Partial data received for range $start-$end: got ${data.size} bytes, expected ${end - start + 1}")
                    retries++
                    //try smaller chunk
                    end /= 2
                    continue
                }
            } catch (e: Exception) {
                println("Error downloading range $start-$end: ${e.message}")
                retries++
            }
            // Print progress
            val progress = (downloadedBytes.toDouble() / totalSize.toDouble()) * 100.0
            print("\rProgress: %.2f%% (%d/%d bytes)".format(progress, downloadedBytes, totalSize))
            System.out.flush()
        }

        println()
        if (downloadedBytes != totalSize) {
            println("Warning: Failed to download all chunks after $MAX_RETRIES retries")
        }

        return output.toByteArray()
    }

    private fun getContentLength(): Int {
        try {
            //get connection without range header
            val connection = createConnection(-1, 0)
            connection.connect()

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val contentLength = connection.getHeaderField("Content-Length")
                connection.disconnect()
                return contentLength?.toInt() ?: -1
            } else {
                connection.disconnect()
                return -1
            }
        } catch (e: Exception) {
            println("Error determining content length: ${e.message}")
            // Fallback to large range method
            return -1
        }
    }

    private fun downloadRange(start: Int, end: Int): ByteArray {
        val connection = createConnection(start, end + 1)

        try {
            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw Exception("Server returned error $responseCode")
            }

            val input = BufferedInputStream(getConnectionInputStream(connection))
            val data = input.readAllBytes()
            input.close()

            return data
        } finally {
            connection.disconnect()
        }
    }

    private fun createConnection(start: Int, end: Int): HttpURLConnection {
        val url = URL(host)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        // start < 0 indicates request without range
        if(start >= 0) {
            connection.setRequestProperty("Range", "bytes=$start-$end")
        }
        connection.setRequestProperty("Connection", "close")
        connection.connectTimeout = 5000 // 5 seconds
        connection.readTimeout = 10000    // 10 seconds

        return connection
    }

    private fun getConnectionInputStream(connection: HttpURLConnection): InputStream {
        return try {
            connection.inputStream
        } catch (e: Exception) {
            connection.errorStream ?: throw e
        }
    }
}

fun calculateSha256(data: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(data)
    return digest.joinToString("") { "%02x".format(it) }
}