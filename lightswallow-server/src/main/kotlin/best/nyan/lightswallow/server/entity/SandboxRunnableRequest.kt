package best.nyan.lightswallow.server.entity

import best.nyan.lightswallow.core.LightSwallowEntity
import best.nyan.lightswallow.core.LightSwallowEntityTask
import best.nyan.lightswallow.core.process.ProcessParameter
import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.server.util.FileUtils.checkDirectoryExists
import best.nyan.lightswallow.server.util.FileUtils.ensureFileExists
import best.nyan.lightswallow.server.util.FileUtils.readFileToString
import best.nyan.lightswallow.server.util.FileUtils.writeStringToFile
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * The batch tasks for LightSwallow sandbox to run
 */
class SandboxRunnableRequest(
    /**
     * ID
     */
    val id: String = UUID.randomUUID().toString() + "_${System.currentTimeMillis()}",
    /**
     * Tasks need to run
     */
    val tasks: List<SandboxRunnableRequestTask> = listOf(),
    /**
     * Path replacement rules
     */
    val pathReplacement: Map<String, String> = mapOf(),
    /**
     * Path of chroot
     */
    val chrootPath: String = "/",
    /**
     * Path of chdir root
     */
    val chdirRootPath: String = "/home",
    /**
     * Path of home (working directory inside the sandbox)
     */
    val homePath: String = "/home",

    /**
     * Delete chdir directory when all processes done
     */
    val deleteDirectoryWhenFinished: Boolean = true,

    /**
     * Read content from IO files after all processes finished up
     */
    val readFromIOFiles: Boolean = false,

    /**
     * Prepare files, written to sandbox before launching processes
     */
    val prepareFiles: Map<String, String> = mapOf()
) {

    fun runAndWait(serverId: String): SandboxRunnableRequestResult {
        val startTime = System.currentTimeMillis()

        // Check directory exists
        if (!checkDirectoryExists(chrootPath))
            throw IOException("Target path not exists")
        val chdirPath = Path(chdirRootPath, id).pathString
        val chdirFile = File(chdirPath)
        chdirFile.mkdirs()

        // Prepared files
        prepareFiles.forEach { (filename, content) ->
            val filePath = Path(chdirPath, filename).pathString
            writeStringToFile(content, filePath)
        }

        // Create sandbox entity
        val sandbox = LightSwallowEntity(
            chrootPath = chrootPath,
            chdirPath = chdirPath,
            homePath = homePath
        )
        sandbox.loadSandbox()

        // Replace the paths
        tasks.forEach {
            pathReplacement.forEach { (x, y) ->
                it.sandboxParameter.paramPathReplace(x, y)
            }
            it.sandboxParameter.paramPathReplace("{root}", chdirPath)
        }

        fun ensureIOFile(param: ProcessParameter, filePath: String): Boolean {
            return if (param.redirectIOBeforeChroot) {
                ensureFileExists(filePath)
            } else {
                if (filePath.isNotEmpty()) {
                    ensureFileExists(Path(chdirPath, filePath).pathString)
                } else false
            }
        }

        fun ensureIOAbsolutePath(param: ProcessParameter, filePath: String): String? {
            if (!ensureIOFile(param, filePath))
                return null
            return if (param.redirectIOBeforeChroot) {
                filePath
            } else {
                Path(chdirPath, filePath).pathString
            }
        }

        // Make sure IO files exists
        tasks.forEach {
            if (!ensureIOFile(it.sandboxParameter, it.sandboxParameter.stdin))
                it.sandboxParameter.stdin = ""
            if (!ensureIOFile(it.sandboxParameter, it.sandboxParameter.stdout))
                it.sandboxParameter.stdout = ""
            if (!ensureIOFile(it.sandboxParameter, it.sandboxParameter.stderr))
                it.sandboxParameter.stderr = ""
        }

        val ioContentMap: MutableMap<String, Map<String, String>> = mutableMapOf()

        // Run and collect results
        val resultMap = buildMap {
            tasks.forEach { task ->
                val result = sandbox.runTask(
                    LightSwallowEntityTask(
                        sandboxParameter = task.sandboxParameter
                    )
                )
                put(task.id, result)

                if (readFromIOFiles) {
                    ioContentMap[task.id] = buildMap {
                        ensureIOAbsolutePath(task.sandboxParameter, task.sandboxParameter.stdout)?.let {
                            put("out", readFileToString(it))
                        }
                        ensureIOAbsolutePath(task.sandboxParameter, task.sandboxParameter.stderr)?.let {
                            put("err", readFileToString(it))
                        }
                    }
                }
            }
        }

        val endTime = System.currentTimeMillis()

        Thread.sleep(0)
        System.gc()

        if (deleteDirectoryWhenFinished) {
            chdirFile.listFiles()?.forEach { it.delete() }
            chdirFile.delete()
        }

        // Run and build result
        return SandboxRunnableRequestResult(
            id = id,
            startTime = startTime,
            endTime = endTime,
            serverId = serverId,
            result = resultMap,
            ioContent = ioContentMap
        )
    }

}

val defaultProcessParameter = ProcessParameter() //useless, just for adding a non-params constructor

data class SandboxRunnableRequestTask(
    val sandboxParameter: ProcessParameter = defaultProcessParameter
) {
    val id: String = UUID.randomUUID().toString()
}

data class SandboxRunnableRequestResult(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val serverId: String,
    val result: Map<String, ProcessResult>,
    val ioContent: Map<String, Map<String, String>>
)
