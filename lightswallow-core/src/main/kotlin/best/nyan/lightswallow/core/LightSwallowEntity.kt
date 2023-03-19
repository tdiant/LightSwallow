package best.nyan.lightswallow.core

import best.nyan.lightswallow.core.process.MountPair
import best.nyan.lightswallow.core.process.ProcessParameter
import best.nyan.lightswallow.core.process.toSafetyJavaList
import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.core.result.ProcessResultType
import best.nyan.lightswallow.core.sandbox.SandboxCore
import java.util.*

/**
 * Entity for LightSwallow sandbox
 */
class LightSwallowEntity(
    val id: UUID = UUID.randomUUID(),
    val chrootPath: String,
    val chdirPath: String,
    val homePath: String
) {

    private val cgroupName get() = id.toString().replace("-", "")

    private var status: SandboxStatus = SandboxStatus.DEAD
    private val sandboxCore: SandboxCore = SandboxCore(cgroupName)

    /**
     * Load sandbox to check the runtime environment
     */
    fun loadSandbox() {
        status = SandboxStatus.LOADING
        val result = sandboxCore.runProcess(
            ProcessParameter(
                name = cgroupName,
                executable = "/bin/echo",
                parameters = toSafetyJavaList(listOf("/bin/echo", "_LOAD_SANDBOX_TEST_")),
                chrootPath = chrootPath,
                chdirPath = homePath,
                timeLimit = 500,
                memoryLimit = 1 * 1024 * 1024,
                stdout = "/dev/stdout",
                stderr = "/dev/stderr",
                redirectIOBeforeChroot = true,
                mounts = toSafetyJavaList(
                    listOf(
                        MountPair(sourcePath = chdirPath, targetPath = homePath, readonly = false)
                    )
                )
            )
        )

        status = if (result.status == ProcessResultType.SUCCESS_EXIT)
            SandboxStatus.READY
        else
            SandboxStatus.DEAD
    }

    /**
     * Run a task in this sandbox environment
     */
    fun runTask(task: LightSwallowEntityTask): ProcessResult {
        if (status != SandboxStatus.READY)
            throw RuntimeException("Status is not READY")
        status = SandboxStatus.RUNNING
        task.sandboxParameter.name = cgroupName
        try {
            val result = sandboxCore.runProcess(task.sandboxParameter)
            status = SandboxStatus.READY
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            status = SandboxStatus.READY
            throw RuntimeException(e)
        }
    }

    /**
     * Destroy the environment
     */
    fun destroySandbox() {
        sandboxCore.destoryEnvironment()
    }

}

enum class SandboxStatus {
    LOADING, READY, RUNNING, DEAD
}

/**
 * Task for LightSwallow sandbox
 */
data class LightSwallowEntityTask(
    val id: String = UUID.randomUUID().toString(),
    val sandboxParameter: ProcessParameter = ProcessParameter()
)
