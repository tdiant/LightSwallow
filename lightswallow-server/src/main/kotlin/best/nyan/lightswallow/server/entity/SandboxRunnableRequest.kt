package best.nyan.lightswallow.server.entity

import best.nyan.lightswallow.core.LightSwallowEntity
import best.nyan.lightswallow.core.LightSwallowEntityTask
import best.nyan.lightswallow.core.process.ProcessParameter
import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.server.util.FileUtils.checkDirectoryExists
import best.nyan.lightswallow.server.util.FileUtils.ensureFileExists
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
     * Tasks need to run
     */
    val tasks: List<SandboxRunnableRequestTask>,
    /**
     * Path replacement rules
     */
    val pathReplacement: Map<String, String> = mapOf(),
    /**
     * Path of chroot
     */
    val chrootPath: String,
    /**
     * Path of chdir root
     */
    val chdirRootPath: String,
    /**
     * Path of home (working directory inside the sandbox)
     */
    val homePath: String,

    /**
     * Delete chdir directory when all processes done
     */
    val deleteDirectoryWhenFinished: Boolean = true
) {
    /**
     * ID
     */
    val id: String = UUID.randomUUID().toString() + "_${System.currentTimeMillis()}"

    fun runAndWait(serverId: String): SandboxRunnableRequestResult {
        val startTime = System.currentTimeMillis()

        // Check directory exists
        if (!checkDirectoryExists(chrootPath))
            throw IOException("Target path not exists")
        val chdirPath = Path(chdirRootPath, id).pathString
        val chdirFile = File(chdirPath)
        chdirFile.mkdirs()

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

        // Make sure IO files exists
        tasks.forEach {
            if (!ensureIOFile(it.sandboxParameter, it.sandboxParameter.stdin))
                it.sandboxParameter.stdin = ""
            if (!ensureIOFile(it.sandboxParameter, it.sandboxParameter.stdout))
                it.sandboxParameter.stdout = ""
            if (!ensureIOFile(it.sandboxParameter, it.sandboxParameter.stderr))
                it.sandboxParameter.stderr = ""
        }

        // Run and collect results
        val resultMap = buildMap {
            tasks.forEach { task ->
                val result = sandbox.runTask(
                    LightSwallowEntityTask(
                        sandboxParameter = task.sandboxParameter
                    )
                )
                put(task.id, result)
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
            result = resultMap
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
    val result: Map<String, ProcessResult>
)
