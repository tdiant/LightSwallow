package best.nyan.lightswallow.server.entity

import best.nyan.lightswallow.core.LightSwallowEntity
import best.nyan.lightswallow.core.LightSwallowEntityTask
import best.nyan.lightswallow.core.process.ProcessParameter
import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.server.util.FileUtils.checkDirectoryExists
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
    val homePath: String
) {
    fun runAndWait(serverId: String): SandboxRunnableRequestResult {
        val startTime = System.currentTimeMillis()

        // Check directory exists
        if (!checkDirectoryExists(chrootPath))
            throw IOException("Target path not exists")
        val chdirPath = Path(chdirRootPath, id).pathString
        File(chdirPath).mkdirs()

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
        }

        // Run and collect results
        val resultMap = buildMap {
            tasks.forEach {
                val result = sandbox.runTask(
                    LightSwallowEntityTask(
                        sandboxParameter = it.sandboxParameter
                    )
                )
                put(it.id, result)
            }
        }

        val endTime = System.currentTimeMillis()

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

data class SandboxRunnableRequestTask(
    val id: String = UUID.randomUUID().toString(),
    val sandboxParameter: ProcessParameter
)

data class SandboxRunnableRequestResult(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val serverId: String,
    val result: Map<String, ProcessResult>
)
