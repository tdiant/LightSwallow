package best.nyan.lightswallow.server.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * Utils for file operations
 */
object FileUtils {

    fun readFileToString(path: String): String {
        val file = File(path)
        if (!file.exists())
            return ""
        return BufferedReader(FileReader(file)).use { reader ->
            buildString {
                reader.readLines().forEach { append(it) }
            }
        }
    }

    fun checkDirectoryExists(path: String): Boolean =
        File(path).let { it.exists() && it.isDirectory }

    fun checkFileExists(path: String): Boolean =
        File(path).let { it.exists() && it.isFile }

    fun writeStringToFile(content: String, filePath: String) {
        val file = File(filePath)
        if (!file.exists())
            file.createNewFile()

        FileWriter(file).use { writer ->
            writer.write(content)
            writer.flush()
        }
    }

    fun relativeOrAbsolutePath(filename: String, relativeRoot: String): String {
        return if (filename.startsWith("/"))
            filename
        else
            Path(relativeRoot, filename).pathString
    }

}
